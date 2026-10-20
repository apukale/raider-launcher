package com.gathin.tvhome;

/** Defaults for on-TV launcher preferences. */
final class LauncherPrefs {
    private LauncherPrefs() {}
    static boolean showPillLabels(boolean hasStoredValue, boolean storedValue) {
        return hasStoredValue && storedValue;
    }
}