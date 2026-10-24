package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;

/**
 * The one place the launcher picks typefaces. Layouts and code name Outfit weights; this maps them
 * onto the family saved for that screen (Home, Settings, screensaver, Panchang). Outfit stays the
 * default until a choice is saved.
 *
 * <p>Families and files live in {@code res/font}. Variable families are one Latin-subset file plus
 * a font-family XML per weight (baked once per process, same as {@code outfit_regular.xml}).
 * Atkinson Hyperlegible is Regular and Bold only. Roboto is not in the APK: the chooser uses the
 * device's Roboto files when they exist, otherwise the platform sans-serif family. Licenses are in
 * {@code FONT-LICENSE-OFL.txt}. The list is the Look → Fonts setting.
 */
final class Fonts {
    static final String HOME = "home", SETTINGS = "settings", DREAM = "dream", PANCHANG = "panchang";
    static final String[] AREAS = {HOME, SETTINGS, DREAM, PANCHANG};
    static final String[] AREA_LABELS = {"Home", "Launcher settings", "Screensaver", "Panchang"};

    private static final class Fam {
        final String name;
        final int[] weights;
        final int[] ids;
        final boolean system;
        Fam(String name, int[] weights, int[] ids, boolean system) {
            this.name = name;
            this.weights = weights;
            this.ids = ids;
            this.system = system;
        }
    }

    private static Fam cuts(String name, int... pairs) {
        int n = pairs.length / 2;
        int[] weights = new int[n];
        int[] ids = new int[n];
        for (int i = 0; i < n; i++) {
            weights[i] = pairs[i * 2];
            ids[i] = pairs[i * 2 + 1];
        }
        return new Fam(name, weights, ids, false);
    }

    private static final Fam[] FAM = {
            cuts("Outfit", 200, R.font.outfit_extralight, 300, R.font.outfit_light, 400, R.font.outfit_regular, 500, R.font.outfit_medium),
            cuts("Sora", 200, R.font.sora_extralight, 300, R.font.sora_light, 400, R.font.sora_regular, 500, R.font.sora_medium, 700, R.font.sora_bold),
            cuts("Sofia Sans", 200, R.font.sofia_sans_extralight, 300, R.font.sofia_sans_light, 400, R.font.sofia_sans_regular, 500, R.font.sofia_sans_medium, 700, R.font.sofia_sans_bold),
            cuts("Nunito", 200, R.font.nunito_extralight, 300, R.font.nunito_light, 400, R.font.nunito_regular, 500, R.font.nunito_medium, 700, R.font.nunito_bold),
            cuts("Work Sans", 200, R.font.work_sans_extralight, 300, R.font.work_sans_light, 400, R.font.work_sans_regular, 500, R.font.work_sans_medium, 700, R.font.work_sans_bold),
            cuts("Lexend", 200, R.font.lexend_extralight, 300, R.font.lexend_light, 400, R.font.lexend_regular, 500, R.font.lexend_medium, 700, R.font.lexend_bold),
            cuts("Atkinson Hyperlegible", 400, R.font.atkinson_regular, 700, R.font.atkinson_bold),
            cuts("Space Grotesk", 300, R.font.space_grotesk_light, 400, R.font.space_grotesk_regular, 500, R.font.space_grotesk_medium, 700, R.font.space_grotesk_bold),
            cuts("Inter", 400, R.font.inter_regular, 500, R.font.inter_medium, 600, R.font.inter_semibold, 700, R.font.inter_bold),
            cuts("Source Serif 4", 200, R.font.source_serif_4_extralight, 300, R.font.source_serif_4_light, 400, R.font.source_serif_4_regular, 500, R.font.source_serif_4_medium, 700, R.font.source_serif_4_bold),
            new Fam("Roboto", new int[]{300, 400, 500, 700}, new int[0], true),
            cuts("Poppins", 300, R.font.poppins_light, 400, R.font.poppins_regular, 500, R.font.poppins_medium, 700, R.font.poppins_bold),
            cuts("Roboto Flex", 200, R.font.roboto_flex_extralight, 300, R.font.roboto_flex_light, 400, R.font.roboto_flex_regular, 500, R.font.roboto_flex_medium, 700, R.font.roboto_flex_bold),
            cuts("Manrope", 200, R.font.manrope_extralight, 300, R.font.manrope_light, 400, R.font.manrope_regular, 500, R.font.manrope_medium, 700, R.font.manrope_bold)};

    static final String[] NAMES = names();
    private static final Typeface[] ROBOTO_CACHE = new Typeface[4];
    private static final String[] ROBOTO_FILES = {"Roboto-Light.ttf", "Roboto-Regular.ttf", "Roboto-Medium.ttf", "Roboto-Bold.ttf"};
    private static final String[] ROBOTO_DIRS = {"/system/fonts/", "/product/fonts/", "/system/product/fonts/"};

    private Fonts() {}

    private static String[] names() {
        String[] names = new String[FAM.length];
        for (int i = 0; i < FAM.length; i++) names[i] = FAM[i].name;
        return names;
    }

    static String key(String area) { return "font_" + area; }

    static int family(Context c, String area) {
        return index(c.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE).getString(key(area), NAMES[0]));
    }

    static String name(Context c, String area) { return NAMES[family(c, area)]; }

    static int index(String name) {
        if (name == null) return 0;
        for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equalsIgnoreCase(name)) return i;
        return 0;
    }

    /**
     * Bundled weight for an Outfit weight already on the view. Buckets match the old light /
     * regular / medium split, with extralight (200) and bold (700) and Inter semibold (600)
     * as their own steps. A weight under 200 is the raw Outfit file's Thin default, which the
     * old mapper treated as light. The result is always one of {@code weights}, and picking
     * again returns the same weight.
     */
    static int pick(int[] weights, int want) {
        int target = want < 200 ? 300 : want <= 250 ? 200 : want <= 350 ? 300 : want < 480 ? 400 : want < 560 ? 500 : want < 650 ? 600 : 700;
        for (int weight : weights) if (weight >= target) return weight;
        return weights[weights.length - 1];
    }

    static int[] weights(int family) { return FAM[Math.max(0, Math.min(FAM.length - 1, family))].weights; }

    /** False only for Roboto, which is loaded from the device instead of {@code res/font}. */
    static boolean bundled(int family) { return !FAM[Math.max(0, Math.min(FAM.length - 1, family))].system; }

    private static int weightOf(int outfitId) {
        if (outfitId == R.font.outfit_extralight) return 200;
        if (outfitId == R.font.outfit_light) return 300;
        if (outfitId == R.font.outfit_medium) return 500;
        return 400;
    }

    /** Typeface for an Outfit weight resource in the area's family. Outfit itself is returned unchanged. */
    static Typeface of(Context c, String area, int outfitId) {
        int f = family(c, area);
        if (f == 0) return c.getResources().getFont(outfitId);
        return face(c, f, weightOf(outfitId));
    }

    static Typeface regular(Context c, int fam) {
        return face(c, Math.max(0, Math.min(FAM.length - 1, fam)), 400);
    }

    private static Typeface face(Context c, int fam, int want) {
        Fam family = FAM[fam];
        int weight = pick(family.weights, want);
        if (family.system) return systemRoboto(weight);
        int id = family.ids[0];
        for (int i = 0; i < family.weights.length; i++) if (family.weights[i] == weight) id = family.ids[i];
        return c.getResources().getFont(id);
    }

    /** Re-skin every TextView in an inflated tree with the area's family, keeping each view's weight. */
    static void apply(View root, String area) {
        if (root == null) return;
        int f = family(root.getContext(), area);
        if (f == 0) return; // layouts already use Outfit
        applyTree(root.getContext(), root, f);
    }

    private static void applyTree(Context c, View v, int f) {
        if (v instanceof TextView) {
            TextView t = (TextView) v;
            Typeface cur = t.getTypeface();
            int w = cur != null && Build.VERSION.SDK_INT >= 28 ? cur.getWeight() : 400;
            t.setTypeface(face(c, f, w));
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) applyTree(c, g.getChildAt(i), f);
        }
    }

    private static Typeface systemRoboto(int weight) {
        int slot = 1;
        int[] weights = FAM[index("Roboto")].weights;
        for (int i = 0; i < weights.length; i++) if (weights[i] == weight) slot = i;
        if (ROBOTO_CACHE[slot] != null) return ROBOTO_CACHE[slot];
        Typeface face = robotoFile(ROBOTO_FILES[slot]);
        if (face == null) face = robotoVariable(weight);
        if (face == null) face = robotoNamed(weight);
        if (face == null) face = Typeface.SANS_SERIF;
        ROBOTO_CACHE[slot] = face;
        return face;
    }

    private static Typeface robotoFile(String filename) {
        for (String dir : ROBOTO_DIRS) {
            File file = new File(dir + filename);
            if (!file.isFile() || file.length() == 0) continue;
            try { return Typeface.createFromFile(file); } catch (RuntimeException ignored) { }
        }
        return null;
    }

    /** One variable Roboto file can still supply Light/Medium/Bold when the static cuts are absent. */
    private static Typeface robotoVariable(int weight) {
        File file = null;
        for (String name : new String[]{"Roboto-Regular.ttf", "Roboto[wdth,wght].ttf"}) {
            for (String dir : ROBOTO_DIRS) {
                File candidate = new File(dir + name);
                if (candidate.isFile() && candidate.length() > 0) { file = candidate; break; }
            }
            if (file != null) break;
        }
        if (file == null) return null;
        try {
            return new Typeface.Builder(file).setFontVariationSettings("'wght' " + weight).build();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Typeface robotoNamed(int weight) {
        if (weight <= 350) return Typeface.create("sans-serif-light", Typeface.NORMAL);
        if (weight >= 650) return Typeface.create("sans-serif", Typeface.BOLD);
        if (weight >= 480) return Typeface.create("sans-serif-medium", Typeface.NORMAL);
        return Typeface.create("sans-serif", Typeface.NORMAL);
    }
}
