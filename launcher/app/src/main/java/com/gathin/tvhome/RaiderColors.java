package com.gathin.tvhome;

/**
 * Eight colour themes. Hexes are chosen for a dark TCL (and one light theme) because the
 * preview notes did not include Isha's hex values. Graphite + Teal and the old second slot are gone.
 *
 * <ul>
 *   <li>Indigo + Marigold — #12142B / #F6C453</li>
 *   <li>Midnight + Ice — #070B14 / #9FD6FF</li>
 *   <li>Espresso + Amber — #1A120C / #E8A04A</li>
 *   <li>Forest + Mint — #0C1612 / #7DDCB0</li>
 *   <li>Plum + Orchid — #160E18 / #E29AD4</li>
 *   <li>Onyx + Crimson — #0C0C0E / #E24B5A</li>
 *   <li>True Black + Silver — #000000 / #D5D8DE</li>
 *   <li>Porcelain + Espresso — #F4EFE8 / #4A2E22 (light)</li>
 * </ul>
 */
final class RaiderColors {
    static final class Theme {
        final String id, label;
        final int accent, background, surface, onSurface, secondary;
        final boolean light;
        Theme(String id, String label, int accent, int background, int surface, int onSurface, int secondary, boolean light) {
            this.id = id;
            this.label = label;
            this.accent = accent;
            this.background = background;
            this.surface = surface;
            this.onSurface = onSurface;
            this.secondary = secondary;
            this.light = light;
        }
    }

    private static final Theme[] THEMES = {
            new Theme("Indigo", "Indigo + Marigold", 0xFFF6C453, 0xFF12142B, 0xFF1C2044, 0xFFF4F1EA, 0xFFC4C0D4, false),
            new Theme("Midnight", "Midnight + Ice", 0xFF9FD6FF, 0xFF070B14, 0xFF121A2A, 0xFFE8F1FA, 0xFFA8B6C8, false),
            new Theme("Espresso", "Espresso + Amber", 0xFFE8A04A, 0xFF1A120C, 0xFF2A1C14, 0xFFF6EDE4, 0xFFCDB8A4, false),
            new Theme("Forest", "Forest + Mint", 0xFF7DDCB0, 0xFF0C1612, 0xFF14241C, 0xFFE7F4EC, 0xFFA8C4B4, false),
            new Theme("Plum", "Plum + Orchid", 0xFFE29AD4, 0xFF160E18, 0xFF261628, 0xFFF6EAF4, 0xFFC8B0C4, false),
            new Theme("Onyx", "Onyx + Crimson", 0xFFE24B5A, 0xFF0C0C0E, 0xFF1A1A1E, 0xFFF2F0F2, 0xFFB8B4B8, false),
            new Theme("True Black", "True Black + Silver", 0xFFD5D8DE, 0xFF000000, 0xFF161616, 0xFFF4F5F7, 0xFFB0B4BC, false),
            new Theme("Porcelain", "Porcelain + Espresso", 0xFF4A2E22, 0xFFF4EFE8, 0xFFFFFBF6, 0xFF2A211C, 0xFF6E5C52, true)
    };

    private RaiderColors() {}

    static Theme[] themes() { return THEMES; }

    static String[] ids() {
        String[] ids = new String[THEMES.length];
        for (int i = 0; i < THEMES.length; i++) ids[i] = THEMES[i].id;
        return ids;
    }

    static String[] labels() {
        String[] labels = new String[THEMES.length];
        for (int i = 0; i < THEMES.length; i++) labels[i] = THEMES[i].label;
        return labels;
    }

    /** Stored name, including the retired Marigold / Teal / Rose / Silver values. */
    static Theme theme(String stored) {
        String key = stored == null ? "" : stored.trim();
        if ("Teal".equalsIgnoreCase(key)) key = "Forest";
        else if ("Rose".equalsIgnoreCase(key)) key = "Plum";
        else if ("Silver".equalsIgnoreCase(key)) key = "True Black";
        else if ("Marigold".equalsIgnoreCase(key) || key.isEmpty()) key = "Indigo";
        for (Theme theme : THEMES) if (theme.id.equalsIgnoreCase(key)) return theme;
        return THEMES[0];
    }

    static int accentColor(String name) { return theme(name).accent; }
}
