package com.gathin.tvhome;

import java.util.Locale;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Filters Unsplash metadata that suggests people or phones. */
final class PhotoContentFilter {
    private static final Set<String> BLOCKED = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "person", "people", "man", "men", "woman", "women", "girl", "boy",
            "hand", "hands", "phone", "smartphone", "selfie", "portrait", "face",
            "couple", "crowd", "child", "children", "kid", "kids")));

    private PhotoContentFilter() {}

    static boolean isPeopleLike(String... texts) {
        if (texts == null) return false;
        for (String text : texts) {
            if (text == null || text.isEmpty()) continue;
            String lower = text.toLowerCase(Locale.ROOT);
            int start = -1;
            for (int i = 0; i <= lower.length(); i++) {
                boolean wordChar = i < lower.length() && Character.isLetterOrDigit(lower.charAt(i));
                if (wordChar && start < 0) start = i;
                else if (!wordChar && start >= 0) {
                    if (BLOCKED.contains(lower.substring(start, i))) return true;
                    start = -1;
                }
            }
        }
        return false;
    }
}
