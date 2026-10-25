package com.gathin.tvhome;

/**
 * Calendar &amp; Clocks is eleven single-line rows plus one caption.
 * Open Panchang, Hindu calendar, Home, Screensaver, text size, four cities,
 * screensaver clocks, and Open World Clocks. India is the home clock and is not a row.
 * City rows are one line ("City N — name"), so this card has no two-line rows.
 * On a 1080p leanback TV the settings scroll area is short; these heights fit it
 * with a few dp to spare so the page does not scroll.
 */
final class CalendarPanelFit {
    static final int ROWS = 11;
    static final int CAPTIONS = 1;
    /** Settings chrome: 27dp top, 27dp bottom, 44dp header, 16dp gap under the header. */
    static final int CHROME_DP = 27 + 27 + 44 + 16;

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
            int children = ROWS + CAPTIONS;
            return cardPadDp * 2 + captionDp + gapDp * children + rowDp * ROWS + bottomPadDp;
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
     * Readable sizes that still fit, leaving 4dp for rounding. Rows stay at least 30dp.
     * A 1dp gap is preferred over a taller row that would sit flush against the next one.
     */
    static Spec forViewport(int viewportDp, int contentBottomPadDp) {
        int bottom = Math.max(0, contentBottomPadDp);
        int limit = Math.max(0, viewportDp - 4);
        int[] rows = {40, 36, 34, 32, 30};
        int[] caption = {22, 20, 18, 16};
        int[] pad = {8, 6, 4, 2};
        int[] gap = {4, 2, 1};
        Spec best = null;
        for (int row : rows) {
            for (int cap : caption) {
                for (int cardPad : pad) {
                    for (int gapDp : gap) {
                        Spec spec = new Spec(row, cap, cardPad, gapDp, bottom);
                        if (spec.totalDp() > limit) continue;
                        if (better(spec, best)) best = spec;
                    }
                }
            }
        }
        if (best != null) return best;
        return new Spec(30, 16, 2, 0, bottom);
    }

    private static boolean better(Spec spec, Spec best) {
        if (best == null) return true;
        if (spec.rowDp != best.rowDp) return spec.rowDp > best.rowDp;
        if (spec.gapDp != best.gapDp) return spec.gapDp > best.gapDp;
        if (spec.cardPadDp != best.cardPadDp) return spec.cardPadDp > best.cardPadDp;
        return spec.captionDp > best.captionDp;
    }
}
