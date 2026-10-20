package com.gathin.tvhome;

import android.content.SharedPreferences;
import java.util.Calendar;
import java.util.TimeZone;

/** Night mode state: the schedule, unless a manual Start now / Stop now override is still before its expiry. */
final class NightState {
    static final String OVERRIDE_ON = "night_override_on";
    static final String OVERRIDE_UNTIL = "night_override_until";
    private NightState() {}

    static int start(SharedPreferences p) { return p.getInt("night_start_min", 22 * 60); }
    static int end(SharedPreferences p) { return p.getInt("night_end_min", 6 * 60); }

    static boolean overrideActive(SharedPreferences p, long now) { return p.getLong(OVERRIDE_UNTIL, 0L) > now; }

    static boolean scheduled(SharedPreferences p, long now) {
        if (!p.getBoolean("night_mode", true)) return false;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(now);
        return NightWindow.isNight(c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE), start(p), end(p));
    }

    static boolean isActive(SharedPreferences p, long now) {
        return overrideActive(p, now) ? p.getBoolean(OVERRIDE_ON, false) : scheduled(p, now);
    }

    /** Next instant the state can change: override expiry, else the next schedule boundary. */
    static long nextChangeMillis(SharedPreferences p, long now) {
        if (overrideActive(p, now)) return p.getLong(OVERRIDE_UNTIL, 0L);
        if (!p.getBoolean("night_mode", true) || start(p) == end(p)) return Long.MAX_VALUE;
        return NightWindow.nextBoundaryMillis(now, TimeZone.getDefault(), start(p), end(p));
    }

    /** Start now: on until the next scheduled end. Stop now: off until the next scheduled start. */
    static void toggleNow(SharedPreferences p, long now) {
        boolean turnOn = !isActive(p, now);
        long until = NightWindow.nextLocalTimeMillis(now, TimeZone.getDefault(), turnOn ? end(p) : start(p));
        p.edit().putBoolean(OVERRIDE_ON, turnOn).putLong(OVERRIDE_UNTIL, until).apply();
    }
}
