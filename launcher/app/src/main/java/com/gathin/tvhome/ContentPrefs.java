package com.gathin.tvhome;

import android.content.SharedPreferences;

/** Movies & Series switch. Region stays India. A missing key hides the icon. */
final class ContentPrefs {
    static final String ENABLED = "tmdb_on";
    static final String REGION = "IN";
    static final String REGION_LABEL = "India";
    static final String FETCHED = "tmdb_fetched_at";
    static final String ERROR = "tmdb_last_error";

    private ContentPrefs() {}

    static boolean available() {
        return !BuildConfig.TMDB_API_KEY.trim().isEmpty() || !BuildConfig.TMDB_READ_TOKEN.trim().isEmpty();
    }

    /** The top-bar icon is shown only when a key is present and the switch is on. */
    static boolean enabled(SharedPreferences prefs) {
        return available() && (prefs == null || prefs.getBoolean(ENABLED, true));
    }
}
