package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CalendarPlacementTest {
    @Test(timeout = 5000)
    public void eitherSurfaceMapsToTheCloserOfTheTwoChoices() {
        assertEquals(CalendarPlacement.HOME_ONLY, CalendarPlacement.mode(true, false));
        assertEquals(CalendarPlacement.HOME_AND_SCREENSAVER, CalendarPlacement.mode(true, true));
        assertEquals(CalendarPlacement.HOME_AND_SCREENSAVER, CalendarPlacement.mode(false, true));
        assertEquals(CalendarPlacement.HOME_ONLY, CalendarPlacement.mode(false, false));
        assertEquals("Only Home", CalendarPlacement.LABELS[CalendarPlacement.HOME_ONLY]);
        assertEquals("Home and Screensaver", CalendarPlacement.LABELS[CalendarPlacement.HOME_AND_SCREENSAVER]);
    }

    @Test(timeout = 5000)
    public void resolvedFlagsShowHomeOnlyOrBoth() {
        assertTrue(CalendarPlacement.resolvedHome(true, false));
        assertFalse(CalendarPlacement.resolvedDream(true, false));
        assertTrue(CalendarPlacement.resolvedHome(true, true));
        assertTrue(CalendarPlacement.resolvedDream(true, true));
        assertTrue(CalendarPlacement.resolvedHome(false, true));
        assertTrue(CalendarPlacement.resolvedDream(false, true));
        assertTrue(CalendarPlacement.resolvedHome(false, false));
        assertFalse(CalendarPlacement.resolvedDream(false, false));
        assertTrue(CalendarPlacement.showOnHome(CalendarPlacement.HOME_ONLY));
        assertFalse(CalendarPlacement.showOnScreensaver(CalendarPlacement.HOME_ONLY));
        assertTrue(CalendarPlacement.showOnScreensaver(CalendarPlacement.HOME_AND_SCREENSAVER));
    }
}
