package com.gathin.tvhome;

/**
 * Hindu calendar placement. The two settings toggles (Show on Home, Show on Screensaver)
 * collapse to one style choice. World clocks have no matching pair, so they stay as they are.
 * Stored keys stay {@code home_hindu_calendar} and {@code dream_calendar}.
 */
final class CalendarPlacement {
    static final int HOME_ONLY = 0;
    static final int HOME_AND_SCREENSAVER = 1;
    static final String HOME = "home_hindu_calendar";
    static final String DREAM = "dream_calendar";
    static final String[] LABELS = {"Only Home", "Home and Screensaver"};

    private CalendarPlacement() {}

    /**
     * Screensaver on becomes Home and Screensaver, which also turns Home on.
     * Screensaver off becomes Only Home, which also turns Home on.
     * Both-off is Only Home: there is no off choice, and Only Home is the closer one.
     */
    static int mode(boolean showOnHome, boolean showOnScreensaver) {
        return showOnScreensaver ? HOME_AND_SCREENSAVER : HOME_ONLY;
    }

    static boolean showOnHome(int mode) { return true; }

    static boolean showOnScreensaver(int mode) { return mode == HOME_AND_SCREENSAVER; }

    static boolean resolvedHome(boolean showOnHome, boolean showOnScreensaver) {
        return showOnHome(mode(showOnHome, showOnScreensaver));
    }

    static boolean resolvedDream(boolean showOnHome, boolean showOnScreensaver) {
        return showOnScreensaver(mode(showOnHome, showOnScreensaver));
    }
}
