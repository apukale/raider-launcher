package com.gathin.tvhome;

import java.util.LinkedHashSet;
import java.util.Set;

/** Pure Java parsing and migration for the selected photo sources. */
final class PhotoSources {
    static final String BING = "bing";
    static final String PICSUM = "picsum";
    static final String UNSPLASH = "unsplash";
    static final String ALL = "all";

    private PhotoSources() {}

    static Set<String> parse(String value) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (value == null || value.trim().isEmpty()) {
            addAll(out);
            return out;
        }
        if ("both".equals(value.trim())) {
            out.add(BING);
            out.add(PICSUM);
            out.add(UNSPLASH);
            return out;
        }
        if (ALL.equals(value.trim())) {
            addAll(out);
            return out;
        }
        for (String item : value.split(",")) {
            String source = item.trim().toLowerCase(java.util.Locale.ROOT);
            if (BING.equals(source) || PICSUM.equals(source) || UNSPLASH.equals(source)) out.add(source);
        }
        return out;
    }

    static String serialize(Set<String> sources) {
        if (sources == null || sources.isEmpty()) return "";
        return String.join(",", sources);
    }

    private static void addAll(Set<String> out) {
        out.add(BING);
        out.add(PICSUM);
        out.add(UNSPLASH);
    }
}
