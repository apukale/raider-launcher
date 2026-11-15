package com.gathin.tvhome;

import android.content.SharedPreferences;

/** On/off switches for each TMDB piece. Region stays India. A missing key hides the feature. */
final class ContentPrefs {
    static final String HERO = "tmdb_hero";
    static final String TRENDING = "tmdb_trending";
    static final String TOP10 = "tmdb_top10";
    static final String TITLE = "tmdb_title";
    static final String REGION = "IN";
    static final String REGION_LABEL = "India";
    static final String FETCHED = "tmdb_fetched_at";
    static final String ERROR = "tmdb_last_error";

    private ContentPrefs() {}

    static boolean available() {
        return !BuildConfig.TMDB_API_KEY.trim().isEmpty() || !BuildConfig.TMDB_READ_TOKEN.trim().isEmpty();
    }

    static boolean on(SharedPreferences prefs, String key) {
        return prefs == null || prefs.getBoolean(key, true);
    }

    static boolean showsHome(SharedPreferences prefs) {
        return available() && (on(prefs, HERO) || on(prefs, TRENDING) || on(prefs, TOP10));
    }
}
