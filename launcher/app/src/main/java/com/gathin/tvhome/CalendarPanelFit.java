package com.gathin.tvhome;

/**
 * Calendar &amp; Clocks has nine rows (five single-line, four city rows with a second line) plus a caption.
 * India is the home clock and is not a row on this panel.
 * On a 1080p leanback TV the settings scroll area is short, so this picks row heights that fit it.
 */
final class CalendarPanelFit {
    static final int ROWS = 9;
    static final int TWO_LINE = 4;
    static final int SINGLE = ROWS - TWO_LINE;
    /** Settings chrome: 27dp top, 27dp bottom, 44dp header, 16dp gap under the header. */
    static final int CHROME_DP = 27 + 27 + 44 + 16;

    static final class Spec {
        final int singleDp, twoDp, captionDp, cardPadDp, gapDp, bottomPadDp;

        Spec(int singleDp, int twoDp, int captionDp, int cardPadDp, int gapDp, int bottomPadDp) {
            this.singleDp = singleDp;
            this.twoDp = twoDp;
            this.captionDp = captionDp;
            this.cardPadDp = cardPadDp;
            this.gapDp = gapDp;
            this.bottomPadDp = bottomPadDp;
        }

        int totalDp() {
            return cardPadDp * 2 + captionDp + gapDp * ROWS + singleDp * SINGLE + twoDp * TWO_LINE + bottomPadDp;
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
     * Readable sizes that still fit. Single-line rows stay at least 30dp; city rows stay at least 40dp
     * so a 16sp title and a 13sp zone line both remain legible from the couch.
     */
    static Spec forViewport(int viewportDp, int contentBottomPadDp) {
        int bottom = Math.max(0, contentBottomPadDp);
        int[] single = {40, 36, 34, 32, 30};
        int[] two = {52, 48, 46, 44, 42, 40};
        int[] caption = {22, 20, 18};
        int[] pad = {8, 6, 4};
        int[] gap = {4, 2, 1};
        Spec best = null;
        for (int s : single) {
            for (int t : two) {
                if (t < s + 8) continue;
                for (int c : caption) {
                    for (int p : pad) {
                        for (int g : gap) {
                            Spec spec = new Spec(s, t, c, p, g, bottom);
                            if (spec.fits(viewportDp) && (best == null || spec.singleDp > best.singleDp
                                    || (spec.singleDp == best.singleDp && spec.twoDp > best.twoDp))) {
                                best = spec;
                            }
                        }
                    }
                }
            }
        }
        if (best != null) return best;
        return new Spec(30, 40, 16, 2, 0, bottom);
    }
}
