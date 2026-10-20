package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** User names for TV inputs ("Jio" for HDMI 1). Shared by the Home inputs list (long-press) and Launcher settings. */
final class InputNames {
    private static final String PREFS = "input_names";
    private InputNames() {}

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    static String name(Context c, String id) { return prefs(c).getString(id, ""); }
    static int count(Context c) { return prefs(c).getAll().size(); }
    static void clearAll(Context c) { prefs(c).edit().clear().apply(); }

    /** "Jio (HDMI 1)" when named, otherwise the TV's own label. */
    static String display(Context c, String id, String label) {
        String n = name(c, id).trim();
        return n.isEmpty() ? label : n + " (" + label + ")";
    }

    /** {id, TV label} for every hardware passthrough input. */
    static List<String[]> list(Context c) {
        ArrayList<String[]> out = new ArrayList<>();
        try {
            TvInputManager tim = (TvInputManager) c.getSystemService(Context.TV_INPUT_SERVICE);
            if (tim == null) return out;
            for (TvInputInfo info : tim.getTvInputList()) {
                if (!info.isPassthroughInput()) continue;
                CharSequence l = info.loadLabel(c);
                String label = l == null || l.length() == 0 ? info.getId() : l.toString();
                if (info.getType() == TvInputInfo.TYPE_HDMI && !label.toUpperCase(Locale.ROOT).contains("HDMI")) label = "HDMI " + label;
                out.add(new String[]{info.getId(), label});
            }
        } catch (RuntimeException ignored) { }
        return out;
    }

    static void showRename(Activity a, String id, String label, Runnable done) {
        EditText e = new EditText(a);
        e.setSingleLine(true);
        e.setText(name(a, id));
        e.setHint("e.g. Jio");
        e.setSelection(e.getText().length());
        int pad = (int) (20 * a.getResources().getDisplayMetrics().density);
        FrameLayout f = new FrameLayout(a);
        f.setPadding(pad, pad / 2, pad, 0);
        f.addView(e);
        new AlertDialog.Builder(a).setTitle("Name for " + label).setView(f)
                .setPositiveButton("Save", (d, w) -> {
                    String v = e.getText().toString().trim();
                    if (v.isEmpty()) prefs(a).edit().remove(id).apply();
                    else prefs(a).edit().putString(id, v.length() > 24 ? v.substring(0, 24) : v).apply();
                    if (done != null) done.run();
                })
                .setNeutralButton("Clear", (d, w) -> { prefs(a).edit().remove(id).apply(); if (done != null) done.run(); })
                .setNegativeButton(android.R.string.cancel, null).show();
    }
}
