package com.gathin.tvhome;

/** Pure Java accent palette mapping. */
final class RaiderColors {
    private RaiderColors() { }
    static int accentColor(String name) {
        if ("Teal".equalsIgnoreCase(name)) return 0xFF2EC4B6;
        if ("Rose".equalsIgnoreCase(name)) return 0xFFF06A8A;
        if ("Silver".equalsIgnoreCase(name)) return 0xFFC9CED6;
        return 0xFFF2A93B;
    }
}
