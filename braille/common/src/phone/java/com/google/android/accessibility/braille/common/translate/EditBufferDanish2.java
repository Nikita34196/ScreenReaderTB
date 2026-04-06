/*
 * Copyright (C) 2023 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.accessibility.braille.common.translate;

import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtils.DOTS3456;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtils.DOTS46;

import android.content.Context;
import com.google.android.accessibility.braille.common.TalkBackSpeaker;
import com.google.android.accessibility.braille.interfaces.BrailleCharacter;
import com.google.android.accessibility.braille.translate.BrailleTranslator;
import java.util.HashMap;
import java.util.Map;

/** An EditBuffer for Danish Braille Grade 2. */
public class EditBufferDanish2 extends EditBufferContracted {

  private static final Map<String, String> INITIAL_MAP = new HashMap<>();
  static {
      INITIAL_MAP.put("1", "a");
      INITIAL_MAP.put("12", "b");
      INITIAL_MAP.put("14", "c");
      INITIAL_MAP.put("145", "d");
      INITIAL_MAP.put("15", "e");
      INITIAL_MAP.put("124", "f");
      INITIAL_MAP.put("1245", "g");
      INITIAL_MAP.put("125", "h");
      INITIAL_MAP.put("24", "i");
      INITIAL_MAP.put("245", "j");
      INITIAL_MAP.put("13", "k");
      INITIAL_MAP.put("123", "l");
      INITIAL_MAP.put("134", "m");
      INITIAL_MAP.put("1345", "n");
      INITIAL_MAP.put("135", "o");
      INITIAL_MAP.put("1234", "p");
      INITIAL_MAP.put("12345", "q");
      INITIAL_MAP.put("1235", "r");
      INITIAL_MAP.put("234", "s");
      INITIAL_MAP.put("2345", "t");
      INITIAL_MAP.put("136", "u");
      INITIAL_MAP.put("1236", "v");
      INITIAL_MAP.put("2456", "w");
      INITIAL_MAP.put("1346", "x");
      INITIAL_MAP.put("13456", "y");
      INITIAL_MAP.put("1356", "z");
      INITIAL_MAP.put("345", "æ");
      INITIAL_MAP.put("246", "ø");
      INITIAL_MAP.put("16", "å");
  }

  public EditBufferDanish2(
      Context context, BrailleTranslator translator, TalkBackSpeaker talkBack) {
    super(context, translator, talkBack);
  }

  @Override
  protected void fillTranslatorMaps(
      Map<String, String> initialCharacterTranslationMap,
      Map<String, String> nonInitialCharacterTranslationMap) {
    INITIAL_MAP.forEach(initialCharacterTranslationMap::put);
  }

  @Override
  protected boolean isLetter(char character) {
    return false;
  }

  @Override
  protected BrailleCharacter getCapitalize() {
    return DOTS46;
  }

  @Override
  protected BrailleCharacter getNumeric() {
    return DOTS3456;
  }

  @Override
  protected boolean forceInitialDefaultTranslation(String dotsNumber) {
    return false;
  }

  @Override
  protected boolean forceNonInitialDefaultTranslation(String dotsNumber) {
    return false;
  }
}
