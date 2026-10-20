package com.gathin.tvhome;

import java.net.URL;
import java.util.Locale;

/** Pure Java allow-list for photo network URLs, including every redirect target. */
final class PhotoUrlPolicy {
    private PhotoUrlPolicy() {}

    static boolean isAllowed(URL url) {
        int port = url.getPort();
        String host = url.getHost().toLowerCase(Locale.ROOT);
        boolean allowed = "picsum.photos".equals(host)
                || "fastly.picsum.photos".equals(host)
                || "www.bing.com".equals(host)
                || "api.unsplash.com".equals(host)
                || "images.unsplash.com".equals(host);
        return "https".equalsIgnoreCase(url.getProtocol()) && (port == -1 || port == 443) && allowed;
    }
}
