package com.gathin.tvhome;

import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Map;
import java.util.WeakHashMap;

/** Applies reversible warm night colors to launcher view trees without accumulating adjustments. */
final class NightTheme {
    private static final int WARM = 0xFFFFE2B8;
    private static final float TEXT_FACTOR = 0.65f;
    private static final float IMAGE_FACTOR = 0.60f;
    private static final int HOME_SCRIM = 0x590E0904;
    private static final Map<View, Integer> TEXT_COLORS = new WeakHashMap<>();
    private static final Map<ImageView, android.graphics.ColorFilter> FILTERS = new WeakHashMap<>();
    private static final Map<ImageView, Boolean> FILTER_CAPTURED = new WeakHashMap<>();

    private NightTheme() {}

    static void applyHome(View root, boolean night, int originalBackground) {
        if (root == null) return;
        if (night) {
            tintTree(root, TEXT_FACTOR);
            root.setBackgroundColor(blend(originalBackground, HOME_SCRIM));
        } else {
            restoreTree(root);
            root.setBackgroundColor(originalBackground);
        }
    }

    static void prepareHomeTextUpdate(View root, int originalBackground) {
        if (root == null) return;
        restoreTree(root);
        clearTextSnapshot(root);
        root.setBackgroundColor(originalBackground);
    }

    static void applyScreensaver(View root, ImageView photo, ImageView nextPhoto, boolean night) {
        if (root == null) return;
        if (night) {
            tintTree(root, TEXT_FACTOR);
            filter(photo, true);
            filter(nextPhoto, true);
        } else {
            restoreTree(root);
            filter(photo, false);
            filter(nextPhoto, false);
        }
    }

    private static void tintTree(View view, float factor) {
        if (view instanceof TextView) {
            TEXT_COLORS.putIfAbsent(view, ((TextView) view).getCurrentTextColor());
            ((TextView) view).setTextColor(tint(TEXT_COLORS.get(view), factor));
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) tintTree(group.getChildAt(i), factor);
        }
    }

    private static void restoreTree(View view) {
        if (view instanceof TextView) {
            Integer color = TEXT_COLORS.get(view);
            if (color != null) ((TextView) view).setTextColor(color);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) restoreTree(group.getChildAt(i));
        }
    }

    private static void clearTextSnapshot(View view) {
        TEXT_COLORS.remove(view);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) clearTextSnapshot(group.getChildAt(i));
        }
    }

    private static void filter(ImageView view, boolean night) {
        if (view == null) return;
        if (!FILTER_CAPTURED.containsKey(view)) {
            FILTERS.put(view, view.getColorFilter());
            FILTER_CAPTURED.put(view, Boolean.TRUE);
        }
        if (!night) { view.setColorFilter(FILTERS.get(view)); return; }
        ColorMatrix matrix = new ColorMatrix(new float[]{
                IMAGE_FACTOR, 0f, 0f, 0f, 24f,
                0f, 0.54f, 0f, 0f, 12f,
                0f, 0f, 0.42f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
        });
        view.setColorFilter(new ColorMatrixColorFilter(matrix));
    }

    private static int tint(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.round(Color.red(color) * factor + Color.red(WARM) * (1f - factor));
        int g = Math.round(Color.green(color) * factor + Color.green(WARM) * (1f - factor));
        int b = Math.round(Color.blue(color) * factor + Color.blue(WARM) * (1f - factor));
        return Color.argb(a, r, g, b);
    }

    private static int blend(int base, int overlay) {
        float alpha = Color.alpha(overlay) / 255f;
        return Color.rgb(Math.round(Color.red(base) * (1f - alpha) + Color.red(overlay) * alpha),
                Math.round(Color.green(base) * (1f - alpha) + Color.green(overlay) * alpha),
                Math.round(Color.blue(base) * (1f - alpha) + Color.blue(overlay) * alpha));
    }
}
