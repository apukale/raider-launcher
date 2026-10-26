package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Display names for Home app tiles ("JioHotstar" for the app whose APK label is "JioHotstar Morphe").
 * The package is unchanged. Shared by the Home tile menu (long-press, Rename) and Launcher settings,
 * the same two paths as {@link InputNames}.
 */
final class AppNames {
    private static final String PREFS = "app_names";

    private AppNames() {}

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    static String name(Context c, String pkg) { return pkg == null ? "" : prefs(c).getString(pkg, ""); }
    static int count(Context c) { return prefs(c).getAll().size(); }
    static void clearAll(Context c) { prefs(c).edit().clear().apply(); }

    /** Control characters and bidi overrides become spaces, then the tile-label cap from {@link Cards#cleanLabel}. */
    static String normalize(String typed) {
        if (typed == null) return "";
        String cleaned = Cards.cleanLabel(typed, "");
        return cleaned == null ? "" : cleaned;
    }

    /**
     * Value to store. Empty, or the same as the tile's own label, means no custom name
     * so Clear and a blank Save both return the system label.
     */
    static String storedValue(String typed, String systemLabel) {
        String custom = normalize(typed);
        if (custom.isEmpty()) return "";
        String system = systemLabel == null ? "" : systemLabel;
        if (custom.equals(system) || custom.equals(normalize(system))) return "";
        return custom;
    }

    /** Tile, hero, and sort text. A custom name replaces the system label. */
    static String display(String systemLabel, String stored) {
        String system = systemLabel == null ? "" : systemLabel;
        String custom = normalize(stored);
        if (custom.isEmpty() || custom.equals(system) || custom.equals(normalize(system))) return system;
        return custom;
    }

    /** Settings row: "JioHotstar (JioHotstar Morphe)" when named, otherwise the system label. */
    static String listLine(String systemLabel, String stored) {
        String system = systemLabel == null ? "" : systemLabel;
        String shown = display(system, stored);
        return shown.equals(system) ? system : shown + " (" + system + ")";
    }

    /** {package, system label} for every leanback app except this launcher, one row per package. */
    static List<String[]> list(Context c) {
        ArrayList<String[]> out = new ArrayList<>();
        try {
            PackageManager pm = c.getPackageManager();
            Intent probe = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
            Map<String, String> byPkg = new LinkedHashMap<>();
            for (ResolveInfo ri : pm.queryIntentActivities(probe, 0)) {
                if (ri.activityInfo == null) continue;
                String pkg = ri.activityInfo.packageName;
                if (pkg == null || pkg.equals(c.getPackageName()) || byPkg.containsKey(pkg)) continue;
                byPkg.put(pkg, Cards.cleanLabel(String.valueOf(ri.loadLabel(pm)), pkg));
            }
            for (Map.Entry<String, String> e : byPkg.entrySet()) out.add(new String[]{e.getKey(), e.getValue()});
            Collections.sort(out, (a, b) -> listLine(a[1], name(c, a[0])).compareToIgnoreCase(listLine(b[1], name(c, b[0]))));
        } catch (RuntimeException ignored) { }
        return out;
    }

    static void showRename(Activity a, String pkg, String systemLabel, Runnable done) {
        if (pkg == null || pkg.isEmpty()) return;
        EditText e = new EditText(a);
        e.setSingleLine(true);
        e.setText(normalize(name(a, pkg)));
        e.setHint("Display name");
        e.setSelection(e.getText().length());
        e.setTextColor(0xFFF4F1EA);
        e.setHintTextColor(0xFFA9ABC7);
        int pad = (int) (16 * a.getResources().getDisplayMetrics().density);
        FrameLayout f = new FrameLayout(a);
        f.setPadding(pad, pad / 4, pad, 0);
        f.addView(e, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        String title = systemLabel == null || systemLabel.isEmpty() ? pkg : systemLabel;
        SettingsPicker.form(a, "Name for " + title, f,
                a.getString(android.R.string.cancel), null,
                "Clear", () -> { prefs(a).edit().remove(pkg).apply(); if (done != null) done.run(); },
                "Save", () -> {
                    String v = storedValue(e.getText().toString(), systemLabel);
                    if (v.isEmpty()) prefs(a).edit().remove(pkg).apply();
                    else prefs(a).edit().putString(pkg, v).apply();
                    if (done != null) done.run();
                });
    }
}
