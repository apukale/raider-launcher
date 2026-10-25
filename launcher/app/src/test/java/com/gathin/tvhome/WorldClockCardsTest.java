package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class WorldClockCardsTest {
    @Test(timeout = 5000)
    public void cityCardsAreTallerTypeOnATranslucentFill() {
        assertTrue(WorldClockCards.LABEL_SP > 16);
        assertTrue(WorldClockCards.TIME_SP > 28);
        assertTrue(WorldClockCards.OFFSET_SP > 16);
        assertTrue(WorldClockCards.TIME_MIN_SP >= 28);
        assertTrue(WorldClockCards.TIME_MIN_SP <= WorldClockCards.TIME_SP);
        assertTrue(WorldClockCards.MIN_HEIGHT_DP >= 200);
        assertTrue(WorldClockCards.readableOverBackdrop());
        assertTrue(WorldClockCards.fillAlpha() < 0xCC);
    }
}
