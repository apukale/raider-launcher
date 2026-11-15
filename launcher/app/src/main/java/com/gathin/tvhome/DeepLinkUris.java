package com.gathin.tvhome;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the view URIs checked on the TCL. The package is pinned separately by
 * {@link DeepLinkLauncher}. A missing id returns null so the caller opens the app
 * instead of guessing a page. JioTV+ is app-open only and always returns null.
 *
 * <p>Prime always uses {@code app.primevideo.com}. A www or amazon.com link is
 * reduced to its id and rebuilt on that host.
 */
final class DeepLinkUris {
    static final String NETFLIX = "netflix";
    static final String HOTSTAR = "hotstar";
    static final String PRIME = "prime";
    static final String SONYLIV = "sonyliv";
    static final String ZEE5 = "zee5";
    static final String AHA = "aha";
    static final String YOUTUBE = "youtube";
    static final String APP = "app";

    private static final Pattern NETFLIX_ID = Pattern.compile("(?:title/)?([0-9]{4,12})");
    private static final Pattern HOTSTAR_ID = Pattern.compile("([0-9]{4,12})");
    private static final Pattern PRIME_ASIN = Pattern.compile("(B[0-9A-Z]{9})");
    private static final Pattern PRIME_GTI = Pattern.compile("(?:detail/)([A-Z0-9]{10,40})");
    private static final Pattern SONY_ID = Pattern.compile("(1[0-9]{9})");
    private static final Pattern YOUTUBE_ID = Pattern.compile("(?:v=|youtu\\.be/|embed/)([A-Za-z0-9_-]{6,16})");
    private static final Pattern ZEE_ID = Pattern.compile("([A-Za-z0-9][A-Za-z0-9-]{2,40})");
    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    private DeepLinkUris() {}

    /**
     * @param movie true for a film, false for a series
     * @param externalRaw Wikidata value or a bare id; may be a full URL
     */
    static String build(String kind, boolean movie, String title, String externalRaw) {
        if (kind == null || APP.equals(kind)) return null;
        String slug = slug(title);
        switch (kind) {
            case NETFLIX: {
                String id = first(NETFLIX_ID, externalRaw);
                return id == null ? null : "https://www.netflix.com/title/" + id;
            }
            case HOTSTAR: {
                String id = first(HOTSTAR_ID, externalRaw);
                if (id == null) return null;
                String section = movie ? "movies" : "shows";
                return "https://www.hotstar.com/in/" + section + "/" + slug + "/" + id;
            }
            case PRIME: {
                String id = primeId(externalRaw);
                return id == null ? null : "https://app.primevideo.com/detail/" + id;
            }
            case SONYLIV: {
                String id = first(SONY_ID, externalRaw);
                if (id == null) return null;
                String section = movie ? "movies" : "shows";
                return "https://www.sonyliv.com/" + section + "/" + slug + "-" + id;
            }
            case ZEE5: {
                String id = zeeId(externalRaw);
                if (id == null) return null;
                String section = movie ? "movies" : "tv-shows";
                return "https://www.zee5.com/" + section + "/details/" + slug + "/" + id;
            }
            case AHA: {
                String aha = ahaSlug(externalRaw);
                if (aha == null) return null;
                return "https://www.aha.video/" + (movie ? "movie/" : "webseries/") + aha;
            }
            case YOUTUBE: {
                String id = youtubeId(externalRaw);
                return id == null ? null : "https://www.youtube.com/watch?v=" + id;
            }
            default:
                return null;
        }
    }

    static String slug(String title) {
        if (title == null) return "title";
        String s = title.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (s.isEmpty()) return "title";
        if (s.length() > 60) s = s.substring(0, 60).replaceAll("-+$", "");
        return s.isEmpty() ? "title" : s;
    }

    private static String primeId(String raw) {
        if (raw == null) return null;
        String upper = raw.toUpperCase(Locale.US);
        String asin = first(PRIME_ASIN, upper);
        if (asin != null) return asin;
        Matcher gti = PRIME_GTI.matcher(upper);
        if (gti.find()) return gti.group(1);
        String trimmed = upper.replaceAll("[^A-Z0-9]", "");
        if (trimmed.matches("[A-Z0-9]{10,40}")) return trimmed;
        return null;
    }

    private static String youtubeId(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        Matcher m = YOUTUBE_ID.matcher(trimmed);
        if (m.find()) return m.group(1);
        return trimmed.matches("[A-Za-z0-9_-]{6,16}") ? trimmed : null;
    }

    private static String zeeId(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        int slash = trimmed.lastIndexOf('/');
        if (slash >= 0 && slash < trimmed.length() - 1) trimmed = trimmed.substring(slash + 1);
        Matcher m = ZEE_ID.matcher(trimmed);
        if (!m.find()) return null;
        String id = m.group(1);
        if (id.contains("..") || id.startsWith("-") || id.endsWith("-")) return null;
        return id;
    }

    private static String ahaSlug(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toLowerCase(Locale.US);
        int slash = s.lastIndexOf('/');
        if (slash >= 0 && slash < s.length() - 1) s = s.substring(slash + 1);
        s = s.replaceAll("[^a-z0-9-]", "");
        if (!SLUG.matcher(s).matches() || s.length() < 2 || s.length() > 80) return null;
        return s;
    }

    private static String first(Pattern pattern, String raw) {
        if (raw == null || raw.isEmpty()) return null;
        Matcher m = pattern.matcher(raw.trim());
        return m.find() ? m.group(1) : null;
    }
}
