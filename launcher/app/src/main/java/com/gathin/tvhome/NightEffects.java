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
 * Dim percent (default 45) is the strength of both: 45% is the historical veil and the
 * historical 35% residual brightness.
 */
final class NightEffects {
    static final String PREF_DIM = "night_dim_percent";
    /** Matches the old fixed veil, alpha 0x73. */
    static final int DEFAULT_DIM = 45;
    /** Discrete steps for a D-pad picker. 45 stays in the list so the old look is one of the choices. */
    static final int[] DIM_STEPS = {20, 30, 40, 45, 50, 60, 70, 80};
    private static final int WARM = 0x140A00;
    private static final String SAVED = "night_saved_brightness", SAVED_MODE = "night_saved_brightness_mode";
    private static final String APPLIED = "night_applied_dim";
    private NightEffects() {}

    /** Nearest discrete step. Ties keep the lower step. Values outside the list clamp to the ends. */
    static int percent(int stored) {
        int best = DIM_STEPS[0];
        int bestDist = Math.abs(stored - best);
        for (int step : DIM_STEPS) {
            int dist = Math.abs(stored - step);
            if (dist < bestDist) {
                best = step;
                bestDist = dist;
            }
        }
        return best;
    }

    /** Warm brown veil. 45% is 0x73140A00, the colour night mode used before this setting existed. */
    static int dimColor(int storedPercent) {
        int alpha = Math.round(percent(storedPercent) * 255f / 100f);
        if (alpha < 0) alpha = 0;
        if (alpha > 255) alpha = 255;
        return (alpha << 24) | WARM;
    }

    /**
     * Brightness to set while night mode is on. 45% keeps 35% of the saved level.
     * Lower percents leave more of the original; higher percents go darker, never below 10.
     */
    static int nightBrightness(int saved, int storedPercent) {
        float residual = 1f - (0.65f * percent(storedPercent) / DEFAULT_DIM);
        if (residual < 0.08f) residual = 0.08f;
        if (residual > 1f) residual = 1f;
        return Math.max(10, Math.round(saved * residual));
    }

    static void apply(Context c, View root, boolean night) {
        dim(c, root, night);
        brightness(c, night);
    }

    static void dim(Context context, View root, boolean night) {
        dim(root, night, readPercent(context));
    }

    static void dim(View root, boolean night) {
        dim(root, night, DEFAULT_DIM);
    }

    static void dim(View root, boolean night, int storedPercent) {
        if (root == null) return;
        if (!night) {
            if (root.getForeground() instanceof NightDim) root.setForeground(null);
            return;
        }
        int color = dimColor(storedPercent);
        if (root.getForeground() instanceof NightDim) ((NightDim) root.getForeground()).setColor(color);
        else root.setForeground(new NightDim(color));
    }

    static void brightness(Context c, boolean night) {
        try {
            if (!Settings.System.canWrite(c)) { Log.w("TvHomeNight", "WRITE_SETTINGS not granted; brightness unchanged"); return; }
            SharedPreferences p = c.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
            ContentResolver r = c.getContentResolver();
            boolean saved = p.contains(SAVED);
            int dim = readPercent(c);
            if (night) {
                int original;
                if (!saved) {
                    original = Settings.System.getInt(r, Settings.System.SCREEN_BRIGHTNESS, 128);
                    int mode = Settings.System.getInt(r, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                    p.edit().putInt(SAVED, original).putInt(SAVED_MODE, mode).apply();
                } else original = p.getInt(SAVED, 128);
                if (p.getInt(APPLIED, -1) != dim) {
                    int low = nightBrightness(original, dim);
                    Settings.System.putInt(r, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                    Settings.System.putInt(r, Settings.System.SCREEN_BRIGHTNESS, low);
                    p.edit().putInt(APPLIED, dim).apply();
                    Log.i("TvHomeNight", "night dim " + dim + "%: brightness " + original + " -> " + low);
                }
            } else if (saved) {
                int b = p.getInt(SAVED, 128), m = p.getInt(SAVED_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                Settings.System.putInt(r, Settings.System.SCREEN_BRIGHTNESS, b);
                Settings.System.putInt(r, Settings.System.SCREEN_BRIGHTNESS_MODE, m);
                p.edit().remove(SAVED).remove(SAVED_MODE).remove(APPLIED).apply();
                Log.i("TvHomeNight", "night off: brightness restored to " + b);
            }
        } catch (RuntimeException e) {
            Log.w("TvHomeNight", "brightness change failed: " + e);
        }
    }

    private static int readPercent(Context c) {
        if (c == null) return DEFAULT_DIM;
        return percent(c.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE).getInt(PREF_DIM, DEFAULT_DIM));
    }

    private static final class NightDim extends ColorDrawable { NightDim(int c) { super(c); } }
}
