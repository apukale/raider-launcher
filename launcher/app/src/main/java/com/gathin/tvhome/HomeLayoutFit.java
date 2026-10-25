package com.gathin.tvhome;

/**
 * Home Layout is the 2×2 (Weather, World clocks, Panchang, Screensaver) plus the
 * apps-per-row band, the column chips, two single-line rows, and two detail rows
 * (Photos and the photo interval, which keep a second line). The card has to fit
 * the 1080p leanback scroll area, including the 24dp pad under the card.
 */
final class HomeLayoutFit {
    static final int CHILDREN = 7;
    static final int SINGLE_ROWS = 2;
    static final int DETAIL_ROWS = 2;

    static final class Spec {
        final int moduleDp, moduleGapDp, bandDp, rowDp, detailDp, cardPadDp, gapDp, bottomPadDp;

        Spec(int moduleDp, int moduleGapDp, int bandDp, int rowDp, int detailDp, int cardPadDp, int gapDp, int bottomPadDp) {
            this.moduleDp = moduleDp;
            this.moduleGapDp = moduleGapDp;
            this.bandDp = bandDp;
            this.rowDp = rowDp;
            this.detailDp = detailDp;
            this.cardPadDp = cardPadDp;
            this.gapDp = gapDp;
            this.bottomPadDp = bottomPadDp;
        }

        int totalDp() {
            int grid = moduleDp * 2 + moduleGapDp;
            int bands = bandDp * 2;
            int singles = rowDp * SINGLE_ROWS;
            int details = detailDp * DETAIL_ROWS;
            return cardPadDp * 2 + grid + bands + singles + details + gapDp * CHILDREN + bottomPadDp;
        }

        boolean fits(int viewportDp) {
            return totalDp() <= viewportDp;
        }
    }

    private HomeLayoutFit() {}

    /**
     * Largest comfortable sizes that still fit, leaving 4dp for rounding.
     * The 2×2 stays at least 36dp. Detail rows stay at least 42dp so the second line is not clipped.
     */
    static Spec forViewport(int viewportDp, int contentBottomPadDp) {
        int bottom = Math.max(0, contentBottomPadDp);
        int limit = Math.max(0, viewportDp - 4);
        int[] modules = {48, 44, 40, 36};
        int[] moduleGaps = {6, 4};
        int[] bands = {40, 36, 32};
        int[] rows = {40, 36, 32};
        int[] details = {48, 44, 42};
        int[] pads = {8, 6, 4};
        int[] gaps = {4, 2, 1};
        Spec best = null;
        for (int module : modules) {
            for (int moduleGap : moduleGaps) {
                for (int band : bands) {
                    for (int row : rows) {
                        for (int detail : details) {
                            for (int pad : pads) {
                                for (int gap : gaps) {
                                    Spec spec = new Spec(module, moduleGap, band, row, detail, pad, gap, bottom);
                                    if (spec.totalDp() > limit) continue;
                                    if (better(spec, best)) best = spec;
                                }
                            }
                        }
                    }
                }
            }
        }
        if (best != null) return best;
        return new Spec(36, 4, 32, 32, 42, 2, 1, bottom);
    }

    private static boolean better(Spec spec, Spec best) {
        if (best == null) return true;
        if (spec.moduleDp != best.moduleDp) return spec.moduleDp > best.moduleDp;
        if (spec.detailDp != best.detailDp) return spec.detailDp > best.detailDp;
        if (spec.rowDp != best.rowDp) return spec.rowDp > best.rowDp;
        if (spec.bandDp != best.bandDp) return spec.bandDp > best.bandDp;
        if (spec.gapDp != best.gapDp) return spec.gapDp > best.gapDp;
        if (spec.moduleGapDp != best.moduleGapDp) return spec.moduleGapDp > best.moduleGapDp;
        return spec.cardPadDp > best.cardPadDp;
    }
}
