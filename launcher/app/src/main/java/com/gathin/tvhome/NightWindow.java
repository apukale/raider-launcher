package com.gathin.tvhome;

import java.util.Calendar;
import java.util.TimeZone;

/** Time-of-day rules for night mode. Equal endpoints mean the window is disabled. */
public final class NightWindow {
    private static final int MINUTES_PER_DAY = 24 * 60;
    private NightWindow() {}

    public static boolean isNight(int minutesOfDay, int startMin, int endMin) {
        if (startMin == endMin) return false;
        int minute = Math.floorMod(minutesOfDay, MINUTES_PER_DAY);
        if (startMin < endMin) return minute >= startMin && minute < endMin;
        return minute >= startMin || minute < endMin;
    }

    /** Returns the next local-time boundary as an epoch instant, accounting for timezone/DST rules. */
    public static long nextBoundaryMillis(long nowMillis, TimeZone tz, int startMin, int endMin) {
        if (startMin == endMin) return Long.MAX_VALUE;
        Calendar now = Calendar.getInstance(tz);
        now.setTimeInMillis(nowMillis);
        Calendar candidate = (Calendar) now.clone();
        int minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int target = isNight(minute, startMin, endMin) ? endMin : startMin;
        setLocalBoundary(candidate, target);
        // Capped: at most a few days ahead. If the local time does not exist on a day
        // (DST gap), Calendar shifts it; accept the first shifted instant after now.
        for (int i = 0; i < 4; i++) {
            boolean exact = candidate.get(Calendar.HOUR_OF_DAY) == target / 60
                    && candidate.get(Calendar.MINUTE) == target % 60;
            if (candidate.getTimeInMillis() > nowMillis && (exact || i >= 2)) break;
            candidate.add(Calendar.DAY_OF_MONTH, 1);
            setLocalBoundary(candidate, target);
        }
        if (candidate.getTimeInMillis() <= nowMillis) return nowMillis + 24L * 60 * 60 * 1000;
        return candidate.getTimeInMillis();
    }

    /** Next instant (strictly after now) at the given local minute of day. */
    public static long nextLocalTimeMillis(long nowMillis, TimeZone tz, int minuteOfDay) {
        Calendar c = Calendar.getInstance(tz);
        c.setTimeInMillis(nowMillis);
        setLocalBoundary(c, Math.floorMod(minuteOfDay, MINUTES_PER_DAY));
        for (int i = 0; i < 3 && c.getTimeInMillis() <= nowMillis; i++) {
            c.add(Calendar.DAY_OF_MONTH, 1);
            setLocalBoundary(c, Math.floorMod(minuteOfDay, MINUTES_PER_DAY));
        }
        return c.getTimeInMillis();
    }

    private static void setLocalBoundary(Calendar calendar, int minuteOfDay) {
        calendar.set(Calendar.HOUR_OF_DAY, minuteOfDay / 60);
        calendar.set(Calendar.MINUTE, minuteOfDay % 60);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }
}
