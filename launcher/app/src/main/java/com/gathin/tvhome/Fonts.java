package com.gathin.tvhome;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/** The one place the launcher picks typefaces. Each area (Home, Settings, screensaver, Panchang) has its own family;
 *  layouts and code name Outfit weights and this maps them to the chosen family, keeping light / regular / medium. */
final class Fonts {
    static final String HOME = "home", SETTINGS = "settings", DREAM = "dream", PANCHANG = "panchang";
    static final String[] AREAS = {HOME, SETTINGS, DREAM, PANCHANG};
    static final String[] AREA_LABELS = {"Home", "Launcher settings", "Screensaver", "Panchang"};
    static final String[] NAMES = {"Outfit", "Inter", "Poppins", "Nunito", "Manrope", "Lexend"};
    // light, regular, medium (SIL OFL Google Fonts; only these three weights are bundled)
    private static final int[][] IDS = {
            {R.font.outfit_light, R.font.outfit_regular, R.font.outfit_medium},
            {R.font.inter_light, R.font.inter_regular, R.font.inter_medium},
            {R.font.poppins_light, R.font.poppins_regular, R.font.poppins_medium},
            {R.font.nunito_light, R.font.nunito_regular, R.font.nunito_medium},
            {R.font.manrope_light, R.font.manrope_regular, R.font.manrope_medium},
            {R.font.lexend_light, R.font.lexend_regular, R.font.lexend_medium}};
    private Fonts() {}

    static String key(String area) { return "font_" + area; }
    static int family(Context c, String area) {
        return index(c.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE).getString(key(area), NAMES[0]));
    }
    static String name(Context c, String area) { return NAMES[family(c, area)]; }
    static int index(String name) { for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equalsIgnoreCase(name)) return i; return 0; }
    private static int slot(int outfitId) {
        if (outfitId == R.font.outfit_light || outfitId == R.font.outfit_extralight) return 0;
        if (outfitId == R.font.outfit_medium) return 2;
        return 1;
    }
    /** Typeface for an Outfit weight resource in the area's family (Outfit itself is returned unchanged). */
    static Typeface of(Context c, String area, int outfitId) {
        int f = family(c, area);
        Resources r = c.getResources();
        return f == 0 ? r.getFont(outfitId) : r.getFont(IDS[f][slot(outfitId)]);
    }
    static Typeface regular(Context c, int fam) { return c.getResources().getFont(IDS[Math.max(0, Math.min(IDS.length - 1, fam))][1]); }
    /** Re-skin every TextView in an inflated tree with the area's family, keeping each view's weight. */
    static void apply(View root, String area) {
        if (root == null) return;
        int f = family(root.getContext(), area);
        if (f == 0) return;   // layouts already use Outfit
        applyTree(root.getResources(), root, f);
    }
    private static void applyTree(Resources r, View v, int f) {
        if (v instanceof TextView) {
            TextView t = (TextView) v;
            Typeface cur = t.getTypeface();
            int w = cur != null && Build.VERSION.SDK_INT >= 28 ? cur.getWeight() : 400;
            t.setTypeface(r.getFont(IDS[f][w <= 350 ? 0 : w >= 480 ? 2 : 1]));
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) applyTree(r, g.getChildAt(i), f);
        }
    }
}
