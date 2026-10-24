package com.gathin.tvhome;

import android.graphics.drawable.GradientDrawable;

/** Small set of dark leanback gradients. A GradientDrawable only — no bitmap and no blur. */
final class PhotoGradients {
    static final int[][] PAIRS = {
            {0xFF1B2838, 0xFF3D5A80},
            {0xFF1A1A2E, 0xFF4A3F6B},
            {0xFF10241C, 0xFF1F6F54},
            {0xFF2A1810, 0xFF8C4A2F},
            {0xFF14182B, 0xFF2E4A7A},
            {0xFF241820, 0xFF6B3A4A},
            {0xFF0E1C24, 0xFF1E5A6E},
            {0xFF1C1814, 0xFF5C4033}
    };

    private PhotoGradients() {}

    static int index(int seed) { return Math.floorMod(seed, PAIRS.length); }

    static int[] pair(int seed) {
        int[] row = PAIRS[index(seed)];
        return new int[] {row[0], row[1]};
    }

    static GradientDrawable drawable(int seed) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, pair(seed));
        drawable.setCornerRadius(0f);
        return drawable;
    }
}
