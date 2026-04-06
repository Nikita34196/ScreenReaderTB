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

/** Supported device info for HID devices. */
public class SupportedDeviceHid extends SupportedDevice {
  private static final String HID_STUB_DEVICE_NAME = "HID";
  private static final List<Pattern> NAME_REGEXES =
      List.of(Pattern.compile(HID_STUB_DEVICE_NAME));

  @Override
  public String driverCode() {
    return "hid";
  }

  @Override
  public boolean connectSecurely() {
    return true;
  }

  @Override
  public Map<String, Integer> friendlyKeyNames() {
    // The keys match the key set in
    // //depot/google3/third_party/brltty/Drivers/Braille/HID/brldefs-hid.h;rcl=626237007;l=16
    return new KeyNameMapBuilder()
        .dots8()
        .add("Space", R.string.key_Space)
        .add("PanLeft", R.string.key_pan_left)
        .add("PanRight", R.string.key_pan_right)
        .add("DPadUp", R.string.key_dpad_up)
        .add("DPadDown", R.string.key_dpad_down)
        .add("DPadLeft", R.string.key_dpad_left)
        .add("DPadRight", R.string.key_dpad_right)
        .add("DPadCenter", R.string.key_dpad_center)
        .add("RockerUp", R.string.key_rocker_up)
        .add("RockerDown", R.string.key_rocker_down)
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
