package com.gathin.tvhome;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

/**
 * Visible night mode: a warm dim layer over launcher screens plus a lower system brightness
 * (WRITE_SETTINGS). The previous brightness is saved once and restored when night mode ends.
 */
final class NightEffects {
    private static final int DIM = 0x73140A00;
    private static final String SAVED = "night_saved_brightness", SAVED_MODE = "night_saved_brightness_mode";
    private NightEffects() {}

    static void apply(Context c, View root, boolean night) { dim(root, night); brightness(c, night); }

    static void dim(View root, boolean night) {
        if (root == null) return;
        boolean has = root.getForeground() instanceof NightDim;
        if (night && !has) root.setForeground(new NightDim(DIM));
        else if (!night && has) root.setForeground(null);
    }

    static void brightness(Context c, boolean night) {
        try {
            if (!Settings.System.canWrite(c)) { Log.w("TvHomeNight", "WRITE_SETTINGS not granted; brightness unchanged"); return; }
            SharedPreferences p = c.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
            ContentResolver r = c.getContentResolver();
            boolean saved = p.contains(SAVED);
            if (night && !saved) {
                int b = Settings.System.getInt(r, Settings.System.SCREEN_BRIGHTNESS, 128);
                int m = Settings.System.getInt(r, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                int low = Math.max(10, Math.round(b * 0.35f));
                p.edit().putInt(SAVED, b).putInt(SAVED_MODE, m).apply();
                Settings.System.putInt(r, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                Settings.System.putInt(r, Settings.System.SCREEN_BRIGHTNESS, low);
                Log.i("TvHomeNight", "night on: brightness " + b + " -> " + low);
            } else if (!night && saved) {
                int b = p.getInt(SAVED, 128), m = p.getInt(SAVED_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                Settings.System.putInt(r, Settings.System.SCREEN_BRIGHTNESS, b);
                Settings.System.putInt(r, Settings.System.SCREEN_BRIGHTNESS_MODE, m);
                p.edit().remove(SAVED).remove(SAVED_MODE).apply();
                Log.i("TvHomeNight", "night off: brightness restored to " + b);
            }
        } catch (RuntimeException e) {
            Log.w("TvHomeNight", "brightness change failed: " + e);
        }
    }

    private static final class NightDim extends ColorDrawable { NightDim(int c) { super(c); } }
}
