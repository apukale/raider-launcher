package com.gathin.tvhome;

import java.net.URL;
import java.util.Locale;

/** Pure Java allow-list for photo network URLs, including every redirect target. */
final class PhotoUrlPolicy {
    private PhotoUrlPolicy() {}

    static boolean isAllowed(URL url) {
        int port = url.getPort();
        String host = url.getHost().toLowerCase(Locale.ROOT);
        boolean allowed = "www.bing.com".equals(host)
                || "api.nasa.gov".equals(host)
                || "apod.nasa.gov".equals(host)
                || "images-api.nasa.gov".equals(host)
                || "images-assets.nasa.gov".equals(host)
                || "api.wikimedia.org".equals(host)
                || "upload.wikimedia.org".equals(host)
                || "api.pexels.com".equals(host)
                || "images.pexels.com".equals(host)
                || "pixabay.com".equals(host)
                || "cdn.pixabay.com".equals(host)
                || "api.artic.edu".equals(host)
                || "www.artic.edu".equals(host)
                || "collectionapi.metmuseum.org".equals(host)
                || "images.metmuseum.org".equals(host)
                || "api.unsplash.com".equals(host)
                || "images.unsplash.com".equals(host);
        return "https".equalsIgnoreCase(url.getProtocol()) && (port == -1 || port == 443) && allowed;
    }
}
