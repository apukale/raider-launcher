package com.gathin.tvhome;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class NightWindowTest {
    @Test(timeout = 5000)
    public void overnightWindow() {
        int start = 22 * 60, end = 6 * 60;
        assertFalse(NightWindow.isNight(21 * 60 + 59, start, end));
        assertTrue(NightWindow.isNight(22 * 60, start, end));
        assertTrue(NightWindow.isNight(23 * 60 + 59, start, end));
        assertTrue(NightWindow.isNight(0, start, end));
        assertTrue(NightWindow.isNight(5 * 60 + 59, start, end));
        assertFalse(NightWindow.isNight(6 * 60, start, end));
        assertFalse(NightWindow.isNight(12 * 60, start, end));
    }

    @Test(timeout = 5000)
    public void sameDayAndEqualEndpoints() {
        assertFalse(NightWindow.isNight(12 * 60 + 59, 13 * 60, 15 * 60));
        assertTrue(NightWindow.isNight(13 * 60, 13 * 60, 15 * 60));
        assertTrue(NightWindow.isNight(14 * 60 + 59, 13 * 60, 15 * 60));
        assertFalse(NightWindow.isNight(15 * 60, 13 * 60, 15 * 60));
        assertFalse(NightWindow.isNight(22 * 60, 22 * 60, 22 * 60));
    }

    @Test(timeout = 5000)
    public void nextBoundariesInIst() {
        TimeZone tz = TimeZone.getTimeZone("Asia/Kolkata");
        assertBoundary("2026-10-20 21:00", "2026-10-20 22:00", tz, 22 * 60, 6 * 60);
        assertBoundary("2026-10-20 23:00", "2026-10-21 06:00", tz, 22 * 60, 6 * 60);
        assertBoundary("2026-10-20 05:00", "2026-10-20 06:00", tz, 22 * 60, 6 * 60);
        assertBoundary("2026-10-20 06:00", "2026-10-20 22:00", tz, 22 * 60, 6 * 60);
    }

    @Test(timeout = 5000)
    public void dstGapTerminatesAndMovesForward() {
        TimeZone ny = TimeZone.getTimeZone("America/New_York"); // 2026-03-08 02:00-03:00 does not exist
        long now = instant("2026-03-08 01:00", ny);
        long next = NightWindow.nextBoundaryMillis(now, ny, 2 * 60 + 30, 6 * 60);
        assertTrue(next > now);
        assertTrue(next - now < 3L * 24 * 60 * 60 * 1000);
    }

    private static void assertBoundary(String from, String expected, TimeZone tz, int start, int end) {
        long now = instant(from, tz);
        assertEquals(instant(expected, tz), NightWindow.nextBoundaryMillis(now, tz, start, end));
    }

    private static long instant(String value, TimeZone tz) {
        String[] parts = value.split("[ :-]");
        Calendar c = Calendar.getInstance(tz);
        c.clear();
        c.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1,
                Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
        return c.getTimeInMillis();
    }
    @Test(timeout = 5000) public void nextLocalTimeIsStrictlyAfterNow() {
        TimeZone ist = TimeZone.getTimeZone("Asia/Kolkata");
        Calendar c = Calendar.getInstance(ist); c.clear(); c.set(2026, Calendar.OCTOBER, 20, 14, 0, 0);
        long six = NightWindow.nextLocalTimeMillis(c.getTimeInMillis(), ist, 6 * 60);
        Calendar r = Calendar.getInstance(ist); r.setTimeInMillis(six);
        assertEquals(21, r.get(Calendar.DAY_OF_MONTH)); assertEquals(6, r.get(Calendar.HOUR_OF_DAY));
        long ten = NightWindow.nextLocalTimeMillis(c.getTimeInMillis(), ist, 22 * 60);
        r.setTimeInMillis(ten); assertEquals(20, r.get(Calendar.DAY_OF_MONTH)); assertEquals(22, r.get(Calendar.HOUR_OF_DAY));
        assertTrue(NightWindow.nextLocalTimeMillis(c.getTimeInMillis(), ist, 14 * 60) > c.getTimeInMillis());
    }
}
