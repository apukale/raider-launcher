package com.gathin.tvhome;

/** Defaults for on-TV launcher preferences. */
final class LauncherPrefs {
    static final String HINDU = "hindu_calendar";

    private LauncherPrefs() {}

    static boolean showPillLabels(boolean hasStoredValue, boolean storedValue) {
        return hasStoredValue && storedValue;
    }

    /** Missing key stays on, so a TV that already shows Panchang keeps showing it. */
    static boolean hinduOn(boolean hasStoredValue, boolean storedValue) {
        return !hasStoredValue || storedValue;
    }

    /** Home and screensaver Panchang. World clocks do not use this. */
    static boolean panchangOnHome(boolean masterOn, boolean showOnHome) {
        return masterOn && showOnHome;
    }

    static boolean panchangOnDream(boolean masterOn, boolean showOnScreensaver) {
        return masterOn && showOnScreensaver;
    }
}