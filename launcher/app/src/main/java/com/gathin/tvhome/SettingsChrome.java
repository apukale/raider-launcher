package com.gathin.tvhome;

/**
 * 2.11.33 settings layout. The left rail keeps a 4dp corner and uses one even gap
 * between its rows. The right-hand card has the same gutter on both sides, and the
 * same horizontal padding inside every category, so text does not sit on the screen edge.
 */
final class SettingsChrome {
    static final int RAIL_ROW_DP = 36;
    /** Same inset on every side of the rail card. The old end inset was 2dp. */
    static final int RAIL_PAD_DP = 10;
    /** Preferred gap. 2dp read as cramped on a 1080p TV. */
    static final int RAIL_MIN_GAP_DP = 8;
    /** Cap so a taller window does not string the menu out. 1080p lands under this. */
    static final int RAIL_MAX_GAP_DP = 12;
    /** Space outside the right-hand card, applied on the start and the end. */
    static final int PANEL_GUTTER_DP = 24;
    /** Space inside the right-hand card, left and right. Taller than the old 12dp. */
    static final int PANEL_PAD_H_DP = 20;

    private SettingsChrome() {}

    /**
     * Bottom margin of each rail row except the last, in pixels. The values differ by
     * at most one pixel and, when the preferred gap fits, use the whole column so the
     * menu is not bunched at the top.
     */
    static int[] railGapsPx(int viewportPx, int count, float density) {
        int gaps = count < 2 ? 0 : count - 1;
        int[] out = new int[gaps];
        if (gaps == 0 || viewportPx <= 0 || density <= 0f) return out;
        int pad = Math.round(RAIL_PAD_DP * density);
        int row = Math.round(RAIL_ROW_DP * density);
        int min = Math.round(RAIL_MIN_GAP_DP * density);
        int max = Math.round(RAIL_MAX_GAP_DP * density);
        int leftover = viewportPx - pad * 2 - row * count;
        if (leftover <= 0) return out;
        int natural = leftover / gaps;
        int gap = natural >= min ? Math.min(natural, max) : natural;
        int extra = gap == natural ? leftover % gaps : 0;
        for (int i = 0; i < gaps; i++) out[i] = gap + (i < extra ? 1 : 0);
        return out;
    }
}
