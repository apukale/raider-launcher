package com.gathin.tvhome;

/** Labels, keys, per-refresh limits and small pure helpers for the photo sources. */
final class ExtraPhotoSources {
    private ExtraPhotoSources() {}

    static String label(String s) {
        switch (s) {
            case PhotoSources.BING: return "Bing";
            case PhotoSources.UNSPLASH: return "Unsplash";
            case PhotoSources.APOD: return "NASA APOD";
            case PhotoSources.NASA: return "NASA Images";
            case PhotoSources.WIKIMEDIA: return "Wikimedia POTD";
            case PhotoSources.PEXELS: return "Pexels";
            case PhotoSources.PIXABAY: return "Pixabay";
            case PhotoSources.ART: return "Paintings";
            default: return s;
        }
    }

    /** Sources that need a key are hidden and skipped while the key is missing from local.properties. */
    static boolean available(String s) {
        if (PhotoSources.UNSPLASH.equals(s)) return has(BuildConfig.UNSPLASH_ACCESS_KEY);
        if (PhotoSources.PEXELS.equals(s)) return has(BuildConfig.PEXELS_API_KEY);
        if (PhotoSources.PIXABAY.equals(s)) return has(BuildConfig.PIXABAY_API_KEY);
        return PhotoSources.known(s);
    }
    private static boolean has(String k) { return k != null && !k.trim().isEmpty(); }

    static String nasaKey() { return has(BuildConfig.NASA_API_KEY) ? BuildConfig.NASA_API_KEY.trim() : "DEMO_KEY"; }

    /** Most new photos one daily refresh adds per source. */
    static int limit(String s) {
        switch (s) {
            case PhotoSources.APOD: return 10;
            case PhotoSources.NASA: return 15;
            case PhotoSources.WIKIMEDIA: return 10;
            case PhotoSources.PEXELS: return 20;
            case PhotoSources.PIXABAY: return 20;
            case PhotoSources.ART: return 15;
            default: return 10;
        }
    }

    /** upload.wikimedia.org original -> its sized thumbnail (".../commons/thumb/a/ab/Name.jpg/1920px-Name.jpg"). */
    static String wikimediaThumb(String src, int width) {
        String marker = "/wikipedia/commons/";
        int i = src == null ? -1 : src.indexOf(marker);
        if (i < 0 || src.contains("/thumb/")) return src;
        String rest = src.substring(i + marker.length());
        String name = rest.substring(rest.lastIndexOf('/') + 1);
        return src.substring(0, i + marker.length()) + "thumb/" + rest + "/" + width + "px-" + name;
    }

    static String stripHtml(String s) { return s == null ? "" : s.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").trim(); }

    /** Bing serves the same picture per market under different suffixes (_EN-IN..., _EN-US...). */
    static String bingKey(String urlbase) { return urlbase == null ? "" : urlbase.replaceAll("_[A-Za-z]{2}-[A-Za-z]{2}[0-9]*$", ""); }
}
