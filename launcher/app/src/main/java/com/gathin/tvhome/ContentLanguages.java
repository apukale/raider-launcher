package com.gathin.tvhome;

/** Languages for the India rows. Stored as a stable comma list, default Telugu, Hindi, and English. */
final class ContentLanguages {
    static final String PREF = "tmdb_languages";
    static final String DEFAULT = "te,hi,en";
    static final String[] CODES = {"te", "hi", "en", "ta", "ml", "kn"};
    static final String[] LABELS = {"Telugu", "Hindi", "English", "Tamil", "Malayalam", "Kannada"};

    private ContentLanguages() {}

    static String[] selected(String stored) {
        boolean[] on = checked(stored);
        if (!any(on)) on = checked(DEFAULT);
        String[] out = new String[count(on)];
        int n = 0;
        for (int i = 0; i < CODES.length; i++) if (on[i]) out[n++] = CODES[i];
        return out;
    }

    static boolean[] checked(String stored) {
        boolean[] on = new boolean[CODES.length];
        if (stored == null || stored.trim().isEmpty()) return checked(DEFAULT);
        for (String part : stored.split(",")) {
            String code = part.trim().toLowerCase(java.util.Locale.US);
            for (int i = 0; i < CODES.length; i++) if (CODES[i].equals(code)) on[i] = true;
        }
        return on;
    }

    static String join(boolean[] on) {
        if (on == null || !any(on)) return DEFAULT;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < CODES.length && i < on.length; i++) {
            if (!on[i]) continue;
            if (b.length() > 0) b.append(',');
            b.append(CODES[i]);
        }
        return b.length() == 0 ? DEFAULT : b.toString();
    }

    static String label(String stored) {
        String[] codes = selected(stored);
        StringBuilder b = new StringBuilder();
        for (String code : codes) {
            if (b.length() > 0) b.append(", ");
            b.append(name(code));
        }
        return b.toString();
    }

    static boolean any(boolean[] on) {
        if (on == null) return false;
        for (boolean value : on) if (value) return true;
        return false;
    }

    private static int count(boolean[] on) {
        int n = 0;
        for (boolean value : on) if (value) n++;
        return n;
    }

    private static String name(String code) {
        for (int i = 0; i < CODES.length; i++) if (CODES[i].equals(code)) return LABELS[i];
        return code;
    }
}
