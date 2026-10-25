package com.gathin.tvhome;

import java.net.URL;

/** OpenWeatherMap Weather Maps 1.0 cloud cover, drawn on the Weather screen's existing India frame.
 *  The API key is passed in from BuildConfig (local.properties). This class has no real key. */
final class CloudCover {
    static final int TILE = 256;
    /** India window the weather map has used since 2.11.15. Cloud tiles use this same box. */
    static final double INDIA_NORTH = 35.5, INDIA_SOUTH = 8.0, INDIA_WEST = 68.1, INDIA_EAST = 97.4;
    /** Maps 1.0 cloud tiles move slowly; keep them on disk so a 2 GB TV does not refetch every open. */
    static final long CACHE_MS = 3L * 60 * 60 * 1000L;
    static final String HOST = "tile.openweathermap.org";
    static final String LAYER = "clouds_new";
    static final String NOTE_OK = "Cloud cover";
    static final String NOTE_NO_KEY = "Cloud cover: no API key";
    static final String NOTE_BAD_KEY = "Cloud cover: check API key";
    static final String NOTE_UNAVAILABLE = "Cloud cover unavailable";

    private CloudCover() {}

    /** Free-tier keys are hex. Reject anything that could change the URL. */
    static boolean keyUsable(String apiKey) {
        if (apiKey == null) return false;
        String key = apiKey.trim();
        if (key.length() < 16 || key.length() > 64) return false;
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            boolean digit = c >= '0' && c <= '9', lower = c >= 'a' && c <= 'z', upper = c >= 'A' && c <= 'Z';
            if (!digit && !lower && !upper) return false;
        }
        return true;
    }

    /** Null when the key is missing or unsafe to place on a tile URL. */
    static String tileUrl(int zoom, int x, int y, String apiKey) {
        if (!keyUsable(apiKey)) return null;
        return "https://" + HOST + "/map/" + LAYER + "/" + zoom + "/" + x + "/" + y + ".png?appid=" + apiKey.trim();
    }

    static boolean allowsTileUrl(String value) {
        if (value == null) return false;
        try {
            URL url = new URL(value);
            int port = url.getPort();
            if (!"https".equalsIgnoreCase(url.getProtocol()) || url.getUserInfo() != null || (port != -1 && port != 443)) return false;
            if (!HOST.equals(url.getHost())) return false;
            String path = url.getPath();
            if (path == null || !path.startsWith("/map/" + LAYER + "/") || path.contains("..")) return false;
            String query = url.getQuery();
            if (query == null || !query.startsWith("appid=") || query.indexOf('&') >= 0) return false;
            return keyUsable(query.substring("appid=".length()));
        } catch (Exception e) {
            return false;
        }
    }

    static double mercY(double lat, double n) {
        double lr = Math.toRadians(lat);
        return (1 - Math.log(Math.tan(lr) + 1 / Math.cos(lr)) / Math.PI) / 2 * n;
    }

    static double mercX(double lon, double n) { return (lon + 180) / 360 * n; }

    /** Lowest zoom at which India is at least as wide as the free area, so the map is never upscaled. */
    static int pickZoom(int freeWidthPx) {
        for (int z = 3; z < 9; z++) {
            double n = (double) (1 << z) * TILE;
            if (freeWidthPx / (mercX(INDIA_EAST, n) - mercX(INDIA_WEST, n)) <= 1.0) return z;
        }
        return 8;
    }

    /** Inclusive XYZ tile box for the India frame: west, east, north, south. */
    static int[] tileBox(int zoom) {
        double n = (double) (1 << zoom) * TILE;
        return new int[] {
            (int) (mercX(INDIA_WEST, n) / TILE), (int) (mercX(INDIA_EAST, n) / TILE),
            (int) (mercY(INDIA_NORTH, n) / TILE), (int) (mercY(INDIA_SOUTH, n) / TILE)
        };
    }
}
