package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class NightEffectsTest {
    @Test(timeout = 5000)
    public void defaultDimMatchesTheOldVeil() {
        assertEquals(45, NightEffects.DEFAULT_DIM);
        assertEquals(45, NightEffects.percent(45));
        assertEquals(0x73140A00, NightEffects.dimColor(45));
        assertEquals(0x73140A00, NightEffects.dimColor(NightEffects.DEFAULT_DIM));
    }

    @Test(timeout = 5000)
    public void percentSnapsToTheNearestStep() {
        assertEquals(20, NightEffects.percent(0));
        assertEquals(20, NightEffects.percent(24));
        assertEquals(45, NightEffects.percent(47));
        assertEquals(50, NightEffects.percent(48));
        assertEquals(50, NightEffects.percent(49));
        assertEquals(80, NightEffects.percent(100));
    }

    @Test(timeout = 5000)
    public void dimColorUsesTheChosenPercent() {
        assertEquals(0x33140A00, NightEffects.dimColor(20));
        assertEquals(0xCC140A00, NightEffects.dimColor(80));
    }

    @Test(timeout = 5000)
    public void brightnessAtTheDefaultMatchesTheOldCut() {
        assertEquals(35, NightEffects.nightBrightness(100, 45));
        assertEquals(10, NightEffects.nightBrightness(20, 45));
        assertEquals(71, NightEffects.nightBrightness(100, 20));
        assertEquals(16, NightEffects.nightBrightness(200, 80));
    }
}
