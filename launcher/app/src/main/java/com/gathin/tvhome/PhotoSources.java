package com.gathin.tvhome;

import java.util.LinkedHashSet;
import java.util.Set;

/** Pure Java parsing and migration for the selected photo sources. Picsum was removed in 2.11.5. */
final class PhotoSources {
    static final String BING = "bing";
    static final String UNSPLASH = "unsplash";
    static final String APOD = "apod";
    static final String NASA = "nasa";
    static final String WIKIMEDIA = "wikimedia";
    static final String PEXELS = "pexels";
    static final String PIXABAY = "pixabay";
    static final String ART = "art";
    static final String ALL = "all";
    static final String[] KNOWN = {BING, UNSPLASH, APOD, NASA, WIKIMEDIA, PEXELS, PIXABAY, ART};
    static final String[] EXTRAS = {APOD, NASA, WIKIMEDIA, PEXELS, PIXABAY, ART};

    private PhotoSources() {}

    static boolean known(String s) { for (String k : KNOWN) if (k.equals(s)) return true; return false; }

    static Set<String> parse(String value) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (value == null || value.trim().isEmpty() || "both".equals(value.trim()) || ALL.equals(value.trim())) {
            for (String k : KNOWN) out.add(k);
            return out;
        }
        for (String item : value.split(",")) {
            String source = item.trim().toLowerCase(java.util.Locale.ROOT);
            if (known(source)) out.add(source);
        }
        // A selection that only named removed sources (for example the old Picsum) falls back to Bing + Unsplash.
        if (out.isEmpty()) { out.add(BING); out.add(UNSPLASH); }
        return out;
    }

    static String serialize(Set<String> sources) {
        if (sources == null || sources.isEmpty()) return "";
        return String.join(",", sources);
    }
}
