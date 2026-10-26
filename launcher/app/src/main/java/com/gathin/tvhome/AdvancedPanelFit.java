package com.gathin.tvhome;

/**
 * Advanced is eight short rows plus a Reset caption: DNS, TV settings, input names,
 * app names, default Home, the caption, reset app order, reset theme choices, and Kids.
 * Clear cache and Free RAM live in the header, so this panel does not repeat them.
 * Heights match the Calendar rule: everything fits the 1080p leanback scroll area.
 */
final class AdvancedPanelFit {
    static final int ROWS = 8;
    static final int CHROME_DP = CalendarPanelFit.CHROME_DP;

    static final class Spec {
        final int rowDp, captionDp, cardPadDp, gapDp, bottomPadDp;

        Spec(int rowDp, int captionDp, int cardPadDp, int gapDp, int bottomPadDp) {
            this.rowDp = rowDp;
            this.captionDp = captionDp;
            this.cardPadDp = cardPadDp;
            this.gapDp = gapDp;
            this.bottomPadDp = bottomPadDp;
        }

        int totalDp() {
            return cardPadDp * 2 + captionDp + rowDp * ROWS + gapDp * (ROWS + 1) + bottomPadDp;
        }

        boolean fits(int viewportDp) {
            return totalDp() <= viewportDp;
        }
    }

    private AdvancedPanelFit() {}

    /** Prefer a 44dp row. Drop padding before dropping below 32dp. */
    static Spec forViewport(int viewportDp, int contentBottomPadDp) {
        int bottom = Math.max(0, contentBottomPadDp);
        int[] rows = {44, 40, 36, 34, 32};
        int[] caption = {18, 16};
        int[] pad = {6, 4};
        int[] gap = {4, 2, 1};
        Spec best = null;
        for (int row : rows) {
            for (int cap : caption) {
                for (int cardPad : pad) {
                    for (int gapDp : gap) {
                        Spec spec = new Spec(row, cap, cardPad, gapDp, bottom);
                        if (spec.fits(viewportDp) && (best == null || spec.rowDp > best.rowDp)) best = spec;
                    }
                }
            }
        }
        if (best != null) return best;
        return new Spec(32, 16, 2, 0, bottom);
    }
}
