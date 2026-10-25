package com.gathin.tvhome;

/**
 * World Clocks screen. Four cities sit in a 2×2 with the same gap in both directions.
 * Sizes are chosen so a 1080p leanback TV (about 540dp tall) keeps a clear gap under the
 * header and a full "12:00 PM" inside each card. Shorter screens give up gap before type.
 */
final class WorldClockCards {
    static final int COLUMNS = 2;
    static final int SCREEN_PAD_H_DP = 48;
    static final int SCREEN_PAD_V_DP = 32;
    /** Title, India time and the header buttons. */
    static final int HEADER_BLOCK_DP = 56;
    static final int HEADER_GAP_DP = 32;
    static final int GRID_GAP_DP = 16;
    static final int PAD_H_DP = 24;
    static final int PAD_V_DP = 18;
    static final int LABEL_GAP_DP = 12;
    static final int OFFSET_GAP_DP = 8;
    static final int LABEL_SP = 22;
    static final int TIME_SP = 48;
    /** Safety floor if a very long period must shrink. The 2×2 is wide enough for 48sp. */
    static final int TIME_MIN_SP = 32;
    static final int OFFSET_SP = 20;
    /** 60% card. Dark enough to read, open enough that the backdrop shows through. */
    static final int FILL = 0x991A1B2E;
    static final int STROKE = 0x55FFFFFF;

    private WorldClockCards() {}

    static int fillAlpha() { return (FILL >>> 24) & 0xFF; }

    static boolean readableOverBackdrop() {
        int alpha = fillAlpha();
        return alpha >= 0x70 && alpha < 0xCC;
    }

    static int rows(int count) {
        if (count <= 0) return 0;
        return (count + COLUMNS - 1) / COLUMNS;
    }

    /** Line box in dp for a font-scale of 1, including a little room under the baseline. */
    static int lineDp(int sp) { return sp + 8; }

    static int textStackDp() {
        return lineDp(LABEL_SP) + LABEL_GAP_DP + lineDp(TIME_SP) + OFFSET_GAP_DP + lineDp(OFFSET_SP);
    }

    /** Width left for the clock face inside one card of a 2×2. */
    static int innerTimeWidthDp(int screenWidthDp) {
        int grid = screenWidthDp - 2 * SCREEN_PAD_H_DP;
        int card = (grid - GRID_GAP_DP) / COLUMNS;
        return card - 2 * PAD_H_DP;
    }

    static final class Frame {
        final int screenPadH, screenPadV, headerGap, gridGap, cardPadH, cardPadV;
        final int labelSp, timeSp, offsetSp;

        Frame(int screenPadH, int screenPadV, int headerGap, int gridGap, int cardPadH, int cardPadV,
              int labelSp, int timeSp, int offsetSp) {
            this.screenPadH = screenPadH;
            this.screenPadV = screenPadV;
            this.headerGap = headerGap;
            this.gridGap = gridGap;
            this.cardPadH = cardPadH;
            this.cardPadV = cardPadV;
            this.labelSp = labelSp;
            this.timeSp = timeSp;
            this.offsetSp = offsetSp;
        }

        int stage(int screenHeightDp) {
            return screenHeightDp - screenPadV * 2 - HEADER_BLOCK_DP - headerGap;
        }

        /** Inner height of one card when two rows share the stage. */
        int cardInner(int screenHeightDp) {
            int card = (stage(screenHeightDp) - gridGap) / 2;
            return card - cardPadV * 2;
        }

        boolean fits(int screenHeightDp) {
            return cardInner(screenHeightDp) >= textStackDp();
        }
    }

    /** Preferred gaps on a 540dp screen. Shorter screens reduce gap, never the clock face. */
    static Frame frame(int screenHeightDp) {
        int height = screenHeightDp <= 0 ? 540 : screenHeightDp;
        int padV = SCREEN_PAD_V_DP;
        int headerGap = HEADER_GAP_DP;
        int gridGap = GRID_GAP_DP;
        int cardPadV = PAD_V_DP;
        Frame frame = new Frame(SCREEN_PAD_H_DP, padV, headerGap, gridGap, PAD_H_DP, cardPadV, LABEL_SP, TIME_SP, OFFSET_SP);
        int guard = 0;
        while (!frame.fits(height) && guard++ < 20) {
            if (headerGap > 20) headerGap -= 2;
            else if (gridGap > 12) gridGap -= 2;
            else if (cardPadV > 12) cardPadV -= 2;
            else if (padV > 27) padV -= 1;
            else break;
            frame = new Frame(SCREEN_PAD_H_DP, padV, headerGap, gridGap, PAD_H_DP, cardPadV, LABEL_SP, TIME_SP, OFFSET_SP);
        }
        return frame;
    }

    /**
     * Largest text size in pixels that draws a string of {@code measuredAt1Px} width
     * (measured at a 1px text size) inside {@code availablePx}, and never above {@code maxPx}.
     * The result is small enough that the whole string, including AM/PM, stays inside the slot.
     */
    static float fitTextPx(float availablePx, float measuredAt1Px, float maxPx) {
        if (availablePx <= 0f || measuredAt1Px <= 0f || maxPx <= 0f) return Math.max(0f, maxPx);
        float fit = availablePx / measuredAt1Px;
        return Math.min(maxPx, fit);
    }
}
