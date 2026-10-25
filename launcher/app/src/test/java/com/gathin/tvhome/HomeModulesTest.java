package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class HomeModulesTest {
    @Test(timeout = 5000)
    public void leftToRightGridMovesInsideAndExitsTowardTheClock() {
        assertEquals(HomeModules.CLOCKS, HomeModules.neighbor(HomeModules.WEATHER, HomeModules.RIGHT, false));
        assertEquals(HomeModules.PANCHANG, HomeModules.neighbor(HomeModules.WEATHER, HomeModules.DOWN, false));
        assertEquals(HomeModules.EXIT_SIDE, HomeModules.neighbor(HomeModules.WEATHER, HomeModules.LEFT, false));
        assertEquals(HomeModules.STAY, HomeModules.neighbor(HomeModules.CLOCKS, HomeModules.RIGHT, false));
        assertEquals(HomeModules.STAY, HomeModules.neighbor(HomeModules.WEATHER, HomeModules.UP, false));
        assertEquals(HomeModules.SCREENSAVER, HomeModules.neighbor(HomeModules.CLOCKS, HomeModules.DOWN, false));
        assertEquals(HomeModules.EXIT_DOWN, HomeModules.neighbor(HomeModules.PANCHANG, HomeModules.DOWN, false));
        assertEquals(HomeModules.EXIT_DOWN, HomeModules.neighbor(HomeModules.SCREENSAVER, HomeModules.DOWN, false));
        assertEquals(HomeModules.WEATHER, HomeModules.neighbor(HomeModules.PANCHANG, HomeModules.UP, false));
        assertEquals(HomeModules.PANCHANG, HomeModules.neighbor(HomeModules.SCREENSAVER, HomeModules.LEFT, false));
    }

    @Test(timeout = 5000)
    public void hebrewExitsTowardTheClockOnTheRight() {
        assertEquals(HomeModules.EXIT_SIDE, HomeModules.neighbor(HomeModules.CLOCKS, HomeModules.RIGHT, true));
        assertEquals(HomeModules.STAY, HomeModules.neighbor(HomeModules.WEATHER, HomeModules.LEFT, true));
        assertEquals(HomeModules.WEATHER, HomeModules.neighbor(HomeModules.CLOCKS, HomeModules.LEFT, true));
        assertEquals(HomeModules.EXIT_SIDE, HomeModules.neighbor(HomeModules.SCREENSAVER, HomeModules.RIGHT, true));
    }

    @Test(timeout = 5000)
    public void pillsMeetTheBlockAboveSettings() {
        assertEquals(HomeModules.SCREENSAVER, HomeModules.moduleAbovePill(HomeModules.PILL_SETTINGS, false));
        assertEquals(HomeModules.PANCHANG, HomeModules.moduleAbovePill(HomeModules.PILL_INPUTS, false));
        assertEquals(HomeModules.PILL_SETTINGS, HomeModules.pillBelow(HomeModules.SCREENSAVER, false));
        assertEquals(HomeModules.PILL_INPUTS, HomeModules.pillBelow(HomeModules.PANCHANG, false));
        assertEquals(HomeModules.PANCHANG, HomeModules.moduleAbovePill(HomeModules.PILL_SETTINGS, true));
        assertEquals(HomeModules.SCREENSAVER, HomeModules.moduleAbovePill(HomeModules.PILL_INPUTS, true));
        assertEquals(HomeModules.PILL_SETTINGS, HomeModules.pillBelow(HomeModules.PANCHANG, true));
        assertEquals(HomeModules.PILL_INPUTS, HomeModules.pillBelow(HomeModules.SCREENSAVER, true));
    }
}
