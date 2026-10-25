package com.gathin.tvhome;

/** Left-rail labels for launcher settings 2.11.19. Order is the on-screen order. */
final class SettingsMenu {
    /** Intent extra: open this left-rail index instead of Home Layout. */
    static final String OPEN_CATEGORY = "open_category";
    static final int HOME = 0;
    static final int PHOTOS = 3;
    static final int CALENDAR = 4;
    static final String[] LABELS = {
            "Home Layout",
            "Appearance",
            "Screensaver",
            "Photos",
            "Calendar & Clocks",
            "Weather",
            "Night Mode",
            "Advanced",
            "About Raider Launcher"
    };

    private SettingsMenu() {}
}
