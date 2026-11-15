package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/** Date window for discover queries. A thin result widens up to {@link #MAX_DAYS}. */
final class Recency {
    static final int MAX_DAYS = 45;
    static final int TREND_ENOUGH = 8;
    static final int TOP_ENOUGH = 6;

    private Recency() {}

    static String today() {
        return format(Calendar.getInstance());
    }

    /** {@code today} is yyyy-MM-dd. The result is that many days earlier, UTC. */
    static String minusDays(String today, int days) {
        if (today == null || !today.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) return today == null ? "" : today;
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.YEAR, Integer.parseInt(today.substring(0, 4)));
        calendar.set(Calendar.MONTH, Integer.parseInt(today.substring(5, 7)) - 1);
        calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(today.substring(8, 10)));
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.add(Calendar.DAY_OF_MONTH, -Math.max(0, days));
        return format(calendar);
    }

    /** Chosen window first, then wider steps through 45 days. Any stays a single open-ended step. */
    static int[] steps(int windowDays) {
        if (windowDays <= 0) return new int[]{ContentWindow.ANY};
        int start = windowDays;
        ArrayList<Integer> out = new ArrayList<>();
        out.add(start);
        for (int day : new int[]{7, 14, 21, 30, MAX_DAYS}) {
            if (day > start) out.add(day);
        }
        int[] steps = new int[out.size()];
        for (int i = 0; i < out.size(); i++) steps[i] = out.get(i);
        return steps;
    }

    static boolean filled(int trending, int top, boolean wantTrend, boolean wantTop) {
        if (wantTrend && trending < TREND_ENOUGH) return false;
        if (wantTop && top < TOP_ENOUGH) return false;
        return true;
    }

    /** ISO dates sort as text. Empty dates are not inside a window. */
    static boolean onOrAfter(String date, String from) {
        if (date == null || from == null || date.length() < 10 || from.length() < 10) return false;
        return date.substring(0, 10).compareTo(from.substring(0, 10)) >= 0;
    }

    private static String format(Calendar calendar) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
    }
}
