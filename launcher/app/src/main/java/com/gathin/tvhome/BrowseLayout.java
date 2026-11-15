package com.gathin.tvhome;

/**
 * Movies &amp; Series geometry for a 1920x1080 screen at 320 dpi (960x540 dp).
 * The filter block sits above the grid. A poster plus its two text lines fits in what remains.
 */
final class BrowseLayout {
    static final int POSTER_DP = 140;
    static final int FILTER_ROWS = 4;
    static final int CHIP_DP = 36;
    static final int CHIP_GAP_DP = 6;
    static final int FILTER_PAD_DP = 8;
    static final int SIDE_DP = 48;
    static final int LABEL_DP = 36;
    static final int COL_GAP_DP = 12;

    static final class Spec {
        final int posterWidthPx;
        final int posterHeightPx;
        final int cellHeightPx;
        final int columns;
        final int filterHeightPx;
        final int gridTopPx;
        final boolean posterInRange;
        final boolean oneCellFits;
        final boolean separated;

        Spec(int posterWidthPx, int posterHeightPx, int cellHeightPx, int columns, int filterHeightPx, int gridTopPx,
             boolean posterInRange, boolean oneCellFits, boolean separated) {
            this.posterWidthPx = posterWidthPx;
            this.posterHeightPx = posterHeightPx;
            this.cellHeightPx = cellHeightPx;
            this.columns = columns;
            this.filterHeightPx = filterHeightPx;
            this.gridTopPx = gridTopPx;
            this.posterInRange = posterInRange;
            this.oneCellFits = oneCellFits;
            this.separated = separated;
        }
    }

    private BrowseLayout() {}

    static Spec at(int widthPx, int heightPx, float density) {
        int posterW = Math.round(POSTER_DP * density);
        int posterH = Math.round(posterW * 3f / 2f);
        int labels = Math.round(LABEL_DP * density);
        int cell = posterH + labels;
        int chip = Math.round(CHIP_DP * density);
        int gap = Math.round(CHIP_GAP_DP * density);
        int filter = FILTER_ROWS * chip + (FILTER_ROWS - 1) * gap + Math.round(FILTER_PAD_DP * density);
        int pad = Math.round(SIDE_DP * density);
        int colGap = Math.round(COL_GAP_DP * density);
        int inner = Math.max(posterW, widthPx - pad * 2);
        int columns = Math.max(1, (inner + colGap) / (posterW + colGap));
        int minW = Math.round(130 * density);
        int maxW = Math.round(150 * density);
        boolean range = posterW >= minW && posterW <= maxW;
        boolean fits = cell <= heightPx - filter;
        boolean separated = filter > 0 && filter + cell <= heightPx;
        return new Spec(posterW, posterH, cell, columns, filter, filter, range, fits, separated);
    }

    static int columns(int widthPx, float density) {
        return at(Math.max(widthPx, 1), Math.max(widthPx, 1), density).columns;
    }
}
