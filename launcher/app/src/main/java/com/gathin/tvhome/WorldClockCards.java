package com.gathin.tvhome;

/** City-card sizes on the World Clocks screen. The row fills the stage; type grows with it. */
final class WorldClockCards {
    static final int LABEL_SP = 24;
    static final int TIME_SP = 48;
    /** Floor so four cities on a 1080p TV still fit "12:00 PM" on one line. */
    static final int TIME_MIN_SP = 32;
    static final int OFFSET_SP = 20;
    static final int PAD_H_DP = 18;
    static final int PAD_V_DP = 28;
    static final int MIN_HEIGHT_DP = 220;
    /** 60% card. Dark enough to read, open enough that the backdrop shows through. */
    static final int FILL = 0x991A1B2E;
    static final int STROKE = 0x55FFFFFF;

    private WorldClockCards() {}

    static int fillAlpha() { return (FILL >>> 24) & 0xFF; }

    static boolean readableOverBackdrop() {
        int alpha = fillAlpha();
        return alpha >= 0x70 && alpha < 0xCC;
    }
}
