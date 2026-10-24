package com.gathin.tvhome;

/**
 * Seven colour themes. Display names are plain ("Indigo + Marigold"). Graphite + Teal and
 * Porcelain + Espresso are not shipped. A saved Porcelain value falls back to Indigo + Marigold.
 * Background, accent, focus, and primary text are the hexes printed on each schematic.
 * Indigo panels are white at 7% over #0E1226 (the stated 6–8% frost). Midnight's panel is the
 * written #12293D. The other panels are the card surfaces in those schematics.
 *
 * <ul>
 *   <li>Indigo + Marigold — bg #0E1226, panel #1F2335, accent #F2A93B, focus #F5B942, text #EDEFF7, secondary #9AA0B8</li>
 *   <li>Midnight + Ice — bg #0B1B2B, panel #12293D, accent #7DD3FC, focus #BAE6FD, text #E6F0F7, secondary #8EADC0</li>
 *   <li>Espresso + Amber — bg #171210, panel #2A221C, accent #F59E0B, focus #FBBF24, text #F3EDE4, secondary #C4B5A4</li>
 *   <li>Forest + Mint — bg #0F1A14, panel #1A2C22, accent #34D399, focus #6EE7B7, text #E9F2EC, secondary #A3C4B4</li>
 *   <li>Plum + Orchid — bg #1A1026, panel #2A1840, accent #E879F9, focus #F0ABFC, text #F1EAF7, secondary #C9B6D4</li>
 *   <li>Onyx + Crimson — bg #140D0F, panel #241618, accent #EF4444, focus #F87171, text #F5EFEF, secondary #C4B0B0</li>
 *   <li>True Black + Silver — bg #000000, panel #141414, accent #D4D4D4, focus #FFFFFF, text #F2F2F2, secondary #A3A3A3</li>
 * </ul>
 */
final class RaiderColors {
    static final class Theme {
        final String id, label;
        final int accent, focus, background, surface, onSurface, secondary;
        final boolean light;
        Theme(String id, String label, int accent, int focus, int background, int surface, int onSurface, int secondary, boolean light) {
            this.id = id;
            this.label = label;
            this.accent = accent;
            this.focus = focus;
            this.background = background;
            this.surface = surface;
            this.onSurface = onSurface;
            this.secondary = secondary;
            this.light = light;
        }
    }

    private static final Theme[] THEMES = {
            new Theme("Indigo", "Indigo + Marigold", 0xFFF2A93B, 0xFFF5B942, 0xFF0E1226, 0xFF1F2335, 0xFFEDEFF7, 0xFF9AA0B8, false),
            new Theme("Midnight", "Midnight + Ice", 0xFF7DD3FC, 0xFFBAE6FD, 0xFF0B1B2B, 0xFF12293D, 0xFFE6F0F7, 0xFF8EADC0, false),
            new Theme("Espresso", "Espresso + Amber", 0xFFF59E0B, 0xFFFBBF24, 0xFF171210, 0xFF2A221C, 0xFFF3EDE4, 0xFFC4B5A4, false),
            new Theme("Forest", "Forest + Mint", 0xFF34D399, 0xFF6EE7B7, 0xFF0F1A14, 0xFF1A2C22, 0xFFE9F2EC, 0xFFA3C4B4, false),
            new Theme("Plum", "Plum + Orchid", 0xFFE879F9, 0xFFF0ABFC, 0xFF1A1026, 0xFF2A1840, 0xFFF1EAF7, 0xFFC9B6D4, false),
            new Theme("Onyx", "Onyx + Crimson", 0xFFEF4444, 0xFFF87171, 0xFF140D0F, 0xFF241618, 0xFFF5EFEF, 0xFFC4B0B0, false),
            new Theme("True Black", "True Black + Silver", 0xFFD4D4D4, 0xFFFFFFFF, 0xFF000000, 0xFF141414, 0xFFF2F2F2, 0xFFA3A3A3, false)
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

    /** Stored name, including retired Marigold / Teal / Rose / Silver / Porcelain values. */
    static Theme theme(String stored) {
        String key = stored == null ? "" : stored.trim();
        if ("Teal".equalsIgnoreCase(key)) key = "Forest";
        else if ("Rose".equalsIgnoreCase(key)) key = "Plum";
        else if ("Silver".equalsIgnoreCase(key)) key = "True Black";
        else if ("Marigold".equalsIgnoreCase(key) || "Porcelain".equalsIgnoreCase(key) || key.isEmpty()) key = "Indigo";
        for (Theme theme : THEMES) if (theme.id.equalsIgnoreCase(key)) return theme;
        return THEMES[0];
    }

    static int accentColor(String name) { return theme(name).accent; }

    /** Text drawn on a solid accent or focus fill. Bright fills get dark ink. */
    static int inkOn(int color) {
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        int y = (r * 299 + g * 587 + b * 114) / 1000;
        return y >= 150 ? 0xFF141210 : 0xFFF6F3EE;
    }
}
