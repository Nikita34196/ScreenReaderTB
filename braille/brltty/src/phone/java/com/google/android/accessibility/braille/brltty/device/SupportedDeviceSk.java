package com.google.android.accessibility.braille.brltty.device;

import android.util.Pair;
import com.google.android.accessibility.braille.brltty.KeyNameMapBuilder;
import com.google.android.accessibility.braille.brltty.R;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Supported device info for Seika Mini Note Taker. Secure connections fail to connect reliably. */
public class SupportedDeviceSk extends SupportedDevice {
  private static final List<Pattern> NAME_REGEXES =
      List.of(Pattern.compile("TSM|seika"));

  @Override
  public String driverCode() {
    return "sk";
  }

  @Override
  public boolean connectSecurely() {
    return false;
  }

  @Override
  public Map<String, Integer> friendlyKeyNames() {
    return new KeyNameMapBuilder()
        .dots8()
        .routing()
        .dualJoysticks()
        .add("Backspace", R.string.key_Backspace)
        .add("Space", R.string.key_Space)
        .add("LeftButton", R.string.key_skntk_PanLeft)
        .add("RightButton", R.string.key_skntk_PanRight)
        .build();
  }

  @Override
  public Set<Pair<Integer, Integer>> vendorProdIds() {
    return Set.of();
  }

  @Override
  public List<Pattern> nameRegexes() {
    return NAME_REGEXES;
  }
}
