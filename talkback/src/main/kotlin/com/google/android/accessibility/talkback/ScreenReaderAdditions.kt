package com.google.android.accessibility.talkback

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * ScreenReaderAdditions — наши улучшения поверх TalkBack:
 * 1. Anthropic Vision API для неподписанных иконок (MAX мессенджер и другие)
 * 2. Позиционная эвристика для кнопок мессенджеров
 * 3. Словарь resource-id → русские названия
 *
 * Используется из TalkBackService через singleton.
 */
object ScreenReaderAdditions {

    private const val TAG = "ScreenReaderAdditions"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private val cache = mutableMapOf<String, String>()

    // Пакеты поддерживаемых мессенджеров
    private val MESSENGER_PACKAGES = setOf(
        "ru.oneme.app",    // MAX мессенджер
        "com.vk.im",       // VK Мессенджер
        "com.vk.vkclient", // ВКонтакте
        "org.telegram.messenger",
        "com.whatsapp"
    )

    // Anthropic API ключ (пользователь задаёт через настройки)
    var anthropicApiKey: String = ""

    /**
     * Вызывается из TalkBackService когда TalkBack не нашёл описание для узла.
     * Возвращает описание через callback на главном потоке.
     */
    fun describeUnlabeledNode(
        service: AccessibilityService,
        node: AccessibilityNodeInfo,
        onResult: (String) -> Unit
    ) {
        val pkg = node.packageName?.toString() ?: ""
        val cacheKey = "${pkg}_${node.viewIdResourceName}_${node.className}"

        // Проверяем кэш
        cache[cacheKey]?.let { onResult(it); return }

        // 1. Resource ID эвристика
        val fromId = guessFromId(node.viewIdResourceName)
        if (fromId.isNotBlank()) {
            cache[cacheKey] = fromId; onResult(fromId); return
        }

        // 2. Позиционная эвристика для мессенджеров
        if (pkg in MESSENGER_PACKAGES) {
            val fromPos = guessMessengerButton(node)
            if (fromPos.isNotBlank()) {
                cache[cacheKey] = fromPos; onResult(fromPos); return
            }
        }

        // 3. Anthropic Vision API
        if (anthropicApiKey.isNotBlank()) {
            scope.launch {
                val result = tryAnthropicVision(service, node, pkg in MESSENGER_PACKAGES)
                if (!result.isNullOrBlank()) {
                    cache[cacheKey] = result
                    handler.post { onResult(result) }
                }
            }
        }
    }

    // ─── Позиционная эвристика для мессенджеров ──────────────────────

    private fun guessMessengerButton(node: AccessibilityNodeInfo): String {
        val cn = node.className?.toString() ?: ""
        if (!node.isClickable) return ""
        if (!cn.contains("Button") && !cn.contains("ImageView") && !cn.contains("View")) return ""

        val bounds = Rect(); node.getBoundsInScreen(bounds)
        if (bounds.isEmpty) return ""

        // Ищем строку ввода рядом
        val inputRow = findInputRowBounds(node) ?: return ""
        val relX = (bounds.centerX() - inputRow.left).toFloat() / maxOf(inputRow.width(), 1)

        return when {
            relX > 0.82f -> "отправить или голосовое сообщение"
            relX < 0.10f -> "прикрепить файл"
            relX in 0.10f..0.25f -> "эмодзи или стикеры"
            relX in 0.25f..0.40f -> "ещё"
            else -> ""
        }
    }

    private fun findInputRowBounds(node: AccessibilityNodeInfo): Rect? {
        var p = node.parent; var depth = 0
        while (p != null && depth < 5) {
            for (i in 0 until p.childCount) {
                val child = p.getChild(i) ?: continue
                if (child.className?.toString()?.contains("EditText") == true) {
                    val r = Rect(); p.getBoundsInScreen(r)
                    return if (r.isEmpty) null else r
                }
            }
            p = p.parent; depth++
        }
        return null
    }

    // ─── Resource ID словарь ─────────────────────────────────────────

    private fun guessFromId(resId: String?): String {
        if (resId.isNullOrBlank()) return ""
        val id = resId.substringAfterLast('/').lowercase()
            .replace(Regex("[_-]"), " ")
            .replace(Regex("\\b(btn|button|iv|img|image|ic|icon|tv|text|et|edit|fab|view|layout)\\b\\s?"), "")
            .trim()
        if (id.isBlank() || id.length < 2) return ""
        return LABELS[id] ?: LABELS.entries.find { (k, _) -> id.contains(k) }?.value ?: ""
    }

    // ─── Anthropic Vision API ─────────────────────────────────────────

    private suspend fun tryAnthropicVision(
        service: AccessibilityService,
        node: AccessibilityNodeInfo,
        isMessenger: Boolean
    ): String? {
        return try {
            val bmp = screenshot(service, node) ?: return null
            val stream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 85, stream)
            val b64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

            val context = if (isMessenger) "Это кнопка в мессенджере. " else ""
            val cn = node.className?.toString()?.substringAfterLast('.') ?: "элемент"
            val prompt = "${context}Опиши эту Android иконку одной фразой на русском, 2-4 слова. Тип: $cn."

            val conn = (URL("https://api.anthropic.com/v1/messages")
                .openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-api-key", anthropicApiKey)
                setRequestProperty("anthropic-version", "2023-06-01")
                doOutput = true; connectTimeout = 8000; readTimeout = 10000
            }
            val body = JSONObject().apply {
                put("model", "claude-haiku-4-5-20251001")
                put("max_tokens", 30)
                put("messages", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "image")
                            put("source", JSONObject().apply {
                                put("type", "base64")
                                put("media_type", "image/png")
                                put("data", b64)
                            })
                        })
                        put(JSONObject().apply { put("type", "text"); put("text", prompt) })
                    })
                }))
            }
            conn.outputStream.write(body.toString().toByteArray())
            JSONObject(conn.inputStream.bufferedReader().readText())
                .getJSONArray("content").getJSONObject(0).getString("text").trim()
        } catch (e: Exception) {
            Log.e(TAG, "Vision API error: ${e.message}")
            null
        }
    }

    private fun screenshot(service: AccessibilityService, node: AccessibilityNodeInfo): Bitmap? {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) return null
        return try {
            val rect = Rect(); node.getBoundsInScreen(rect)
            if (rect.isEmpty || rect.width() < 8 || rect.height() < 8) return null
            var result: Bitmap? = null
            val latch = CountDownLatch(1)
            service.takeScreenshot(android.view.Display.DEFAULT_DISPLAY, service.mainExecutor,
                object : AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(s: AccessibilityService.ScreenshotResult) {
                        val hw = s.hardwareBuffer
                        result = Bitmap.wrapHardwareBuffer(hw, null)
                            ?.copy(Bitmap.Config.ARGB_8888, false)
                        hw.close(); latch.countDown()
                    }
                    override fun onFailure(e: Int) { latch.countDown() }
                })
            latch.await(3, TimeUnit.SECONDS)
            val full = result ?: return null
            val x = rect.left.coerceAtLeast(0)
            val y = rect.top.coerceAtLeast(0)
            val w = rect.width().coerceAtMost(full.width - x).coerceAtLeast(1)
            val h = rect.height().coerceAtMost(full.height - y).coerceAtLeast(1)
            val crop = Bitmap.createBitmap(full, x, y, w, h)
            if (crop.width > 128 || crop.height > 128) {
                val sc = 128f / maxOf(crop.width, crop.height)
                Bitmap.createScaledBitmap(
                    crop, (crop.width * sc).toInt(), (crop.height * sc).toInt(), true)
            } else crop
        } catch (e: Exception) { null }
    }

    fun clearCache() = cache.clear()

    private val LABELS = mapOf(
        "search" to "поиск", "find" to "найти",
        "back" to "назад", "forward" to "вперёд",
        "close" to "закрыть", "dismiss" to "закрыть",
        "home" to "домой", "menu" to "меню",
        "more" to "ещё", "overflow" to "ещё",
        "add" to "добавить", "create" to "создать",
        "delete" to "удалить", "remove" to "удалить",
        "edit" to "редактировать", "share" to "поделиться",
        "send" to "отправить", "save" to "сохранить",
        "done" to "готово", "cancel" to "отмена",
        "settings" to "настройки", "gear" to "настройки",
        "profile" to "профиль", "account" to "аккаунт",
        "notification" to "уведомление", "bell" to "уведомление",
        "camera" to "камера", "photo" to "фото",
        "like" to "нравится", "heart" to "нравится",
        "star" to "избранное", "bookmark" to "закладка",
        "play" to "воспроизвести", "pause" to "пауза",
        "next" to "следующий", "prev" to "предыдущий",
        "download" to "скачать", "upload" to "загрузить",
        "refresh" to "обновить", "filter" to "фильтр",
        "call" to "звонок", "phone" to "телефон",
        "message" to "сообщение", "chat" to "чат",
        "voice" to "голосовое", "mic" to "микрофон",
        "attach" to "прикрепить", "emoji" to "эмодзи",
        "sticker" to "стикер", "reply" to "ответить",
        "copy" to "копировать", "paste" to "вставить",
        "info" to "информация", "help" to "помощь",
        "lock" to "заблокировано", "map" to "карта",
        "wifi" to "вай-фай", "bluetooth" to "блютус",
        "volume" to "громкость", "mute" to "без звука"
    )
}
