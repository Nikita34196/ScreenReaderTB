package com.google.android.accessibility.braille.brltty.device;

import android.util.Pair;
import com.google.android.accessibility.braille.brltty.KeyNameMapBuilder;
import com.google.android.accessibility.braille.brltty.R;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Supported device info for KGS devices. */
public class SupportedDeviceMm extends SupportedDevice {
  private static final List<Pattern> NAME_REGEXES =
      List.of(Pattern.compile("BM-NextTouch"));

  @Override
  public String driverCode() {
    return "mm";
  }

  @Override
  public boolean connectSecurely() {
    return false;
  }

  @Override
  public Map<String, Integer> friendlyKeyNames() {
    return new KeyNameMapBuilder().dots8().routing().add("Space", R.string.key_Space);
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
