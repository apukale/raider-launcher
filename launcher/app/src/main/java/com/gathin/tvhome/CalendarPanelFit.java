package com.gathin.tvhome;

/**
 * Calendar &amp; Clocks on a 1080p leanback TV.
 * Six full-width single-line rows, one caption, and the four world-clock cities
 * in a 2×2. Open Panchang stays above Hindu calendar. Open World Clocks is the
 * first control under the World clocks heading, and the city grid sits under that
 * control. India is the home clock and is not a row.
 * Section gaps and the gap inside the city grid are the same, and the card keeps
 * padding on every edge, so the grid is not cramped against the heading or the
 * screensaver row. These heights fit the scroll area with a few dp to spare.
 */
final class CalendarPanelFit {
    /** Full-width controls. The four cities are {@link #CITY_ROWS} rows, not four more of these. */
    static final int ROWS = 6;
    static final int CAPTIONS = 1;
    static final int CITY_ROWS = 2;
    static final int CITY_COLUMNS = 2;
    /**
     * On-screen reading order. Cities read left to right, then the next row:
     * City 1, City 2, City 3, City 4.
     */
    static final String[] ORDER = {
            "open_panchang",
            "hindu",
            "placement",
            "text_size",
            "world_clocks",
            "open_world_clocks",
            "city1", "city2", "city3", "city4",
            "screensaver_clocks"
    };

    static int index(String id) {
        if (id == null) return -1;
        for (int i = 0; i < ORDER.length; i++) if (ORDER[i].equals(id)) return i;
        return -1;
    }
    /** Settings chrome: 27dp top, 27dp bottom, 44dp header, 16dp gap under the header. */
    static final int CHROME_DP = 27 + 27 + 44 + 16;

    static final class Spec {
        final int rowDp, captionDp, cardPadDp, gapDp, cityGapDp, cityDp, bottomPadDp;

        Spec(int rowDp, int captionDp, int cardPadDp, int gapDp, int cityGapDp, int cityDp, int bottomPadDp) {
            this.rowDp = rowDp;
            this.captionDp = captionDp;
            this.cardPadDp = cardPadDp;
            this.gapDp = gapDp;
            this.cityGapDp = cityGapDp;
            this.cityDp = cityDp;
            this.bottomPadDp = bottomPadDp;
        }

        int totalDp() {
            int children = ROWS + CAPTIONS + 1;
            int grid = cityDp * CITY_ROWS + cityGapDp * (CITY_ROWS - 1);
            return cardPadDp * 2 + captionDp + rowDp * ROWS + grid + gapDp * children + bottomPadDp;
        }

        boolean fits(int viewportDp) {
            return totalDp() <= viewportDp;
        }
    }

    private CalendarPanelFit() {}

    /** Height of the right-hand scroll area for a screen this tall, in dp. */
    static int viewportDp(int screenHeightDp) {
        return screenHeightDp - CHROME_DP;
    }

    /**
     * Sizes that still fit, leaving 4dp for rounding. The section gap and the city-grid
     * gap stay equal and at least a few dp, ahead of growing a row until neighbours touch.
     * City blocks use the same height as the other rows. Horizontal inset is the shared
     * panel padding, applied on both sides by the settings screen.
     */
    static Spec forViewport(int viewportDp, int contentBottomPadDp) {
        int bottom = Math.max(0, contentBottomPadDp);
        int limit = Math.max(0, viewportDp - 4);
        int[] rows = {40, 36, 34, 32};
        int[] caption = {22, 20, 18};
        int[] pad = {10, 8, 6};
        int[] gap = {8, 6, 4};
        Spec best = null;
        for (int row : rows) {
            for (int cap : caption) {
                for (int cardPad : pad) {
                    for (int gapDp : gap) {
                        Spec spec = new Spec(row, cap, cardPad, gapDp, gapDp, row, bottom);
                        if (spec.totalDp() > limit) continue;
                        if (better(spec, best)) best = spec;
                    }
                }
            }
        }
        if (best != null) return best;
        return new Spec(32, 18, 6, 4, 4, 32, bottom);
    }

    private static boolean better(Spec spec, Spec best) {
        if (best == null) return true;
        if (spec.gapDp != best.gapDp) return spec.gapDp > best.gapDp;
        if (spec.rowDp != best.rowDp) return spec.rowDp > best.rowDp;
        if (spec.cardPadDp != best.cardPadDp) return spec.cardPadDp > best.cardPadDp;
        return spec.captionDp > best.captionDp;
    }
}
