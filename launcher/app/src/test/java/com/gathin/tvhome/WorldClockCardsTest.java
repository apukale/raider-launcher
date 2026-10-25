package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WorldClockCardsTest {
    @Test(timeout = 5000)
    public void cityCardsAreARoomyTranslucentGrid() {
        assertEquals(2, WorldClockCards.COLUMNS);
        assertEquals(2, WorldClockCards.rows(4));
        assertEquals(2, WorldClockCards.rows(3));
        assertEquals(1, WorldClockCards.rows(2));
        assertTrue(WorldClockCards.LABEL_SP >= 20);
        assertTrue(WorldClockCards.TIME_SP >= 44);
        assertTrue(WorldClockCards.OFFSET_SP >= 18);
        assertTrue(WorldClockCards.TIME_MIN_SP >= 28);
        assertTrue(WorldClockCards.TIME_MIN_SP <= WorldClockCards.TIME_SP);
        assertTrue(WorldClockCards.HEADER_GAP_DP >= 28);
        assertTrue(WorldClockCards.GRID_GAP_DP >= 12);
        assertTrue(WorldClockCards.SCREEN_PAD_H_DP >= 40);
        assertTrue(WorldClockCards.readableOverBackdrop());
        assertTrue(WorldClockCards.fillAlpha() < 0xCC);
        assertTrue(WorldClockCards.innerTimeWidthDp(960) >= 300);
    }

    @Test(timeout = 5000)
    public void gridFitsA1080pScreenWithoutShrinkingTheClockFace() {
        WorldClockCards.Frame full = WorldClockCards.frame(540);
        assertTrue(full.fits(540));
        assertEquals(WorldClockCards.TIME_SP, full.timeSp);
        assertEquals(WorldClockCards.HEADER_GAP_DP, full.headerGap);
        assertEquals(WorldClockCards.GRID_GAP_DP, full.gridGap);
        assertTrue(full.cardInner(540) >= WorldClockCards.textStackDp());
        WorldClockCards.Frame shortTv = WorldClockCards.frame(480);
        assertTrue(shortTv.fits(480));
        assertEquals(WorldClockCards.TIME_SP, shortTv.timeSp);
        assertTrue(shortTv.headerGap >= 20);
        assertTrue(shortTv.gridGap >= 12);
    }

    @Test(timeout = 5000)
    public void timeSizeShrinksUntilThePeriodFits() {
        assertEquals(48f, WorldClockCards.fitTextPx(200f, 2f, 48f), 0.01f);
        float fitted = WorldClockCards.fitTextPx(100f, 4f, 48f);
        assertEquals(25f, fitted, 0.01f);
        assertTrue(fitted * 4f <= 100f);
        assertEquals(48f, WorldClockCards.fitTextPx(0f, 4f, 48f), 0.01f);
    }
}
