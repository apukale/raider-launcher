package com.gathin.tvhome;

/** Recency choice for both India rows. One week is the default. */
final class ContentWindow {
    static final String PREF = "tmdb_window_days";
    static final int DEFAULT_DAYS = 7;
    static final int[] DAYS = {3, 7, 14};
    static final String[] LABELS = {"3 days", "1 week", "2 weeks"};

    private ContentWindow() {}

    static int normalize(int days) {
        for (int value : DAYS) if (value == days) return value;
        return DEFAULT_DAYS;
    }

    /** Three days uses trending/day. A week or two uses trending/week. */
    static boolean dailyTrending(int days) {
        return normalize(days) <= 3;
    }

    static String label(int days) {
        int value = normalize(days);
        for (int i = 0; i < DAYS.length; i++) if (DAYS[i] == value) return LABELS[i];
        return LABELS[1];
    }

    static int index(int days) {
        int value = normalize(days);
        for (int i = 0; i < DAYS.length; i++) if (DAYS[i] == value) return i;
        return 1;
    }
}
