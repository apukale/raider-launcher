package com.gathin.tvhome;

import android.content.Context;

/** One switch for every photo surface. Missing key stays on. */
final class PhotosMaster {
    static final String PREF = "photos_master";

    private PhotosMaster() {}

    static boolean on(boolean contains, boolean stored) { return !contains || stored; }

    static boolean on(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        return on(prefs.contains(PREF), prefs.getBoolean(PREF, true));
    }
}
