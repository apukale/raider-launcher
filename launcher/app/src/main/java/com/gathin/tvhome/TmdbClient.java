package com.gathin.tvhome;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** HTTPS reads for TMDB and Wikidata. The key stays in the query or the bearer header, never in a log line. */
final class TmdbClient {
    private static final String TAG = "TvHomeTmdb";
    private static final int JSON_CAP = 1_500_000;
    private static final int IMAGE_CAP = 2_000_000;

    private TmdbClient() {}

    static String discover(String media, String providers, String language, String dateGte, int page) throws IOException {
        return discover(media, providers, language, dateGte, page, null);
    }

    static String discover(String media, String providers, String language, String dateGte, int page, String sort) throws IOException {
        if (!TmdbModels.MOVIE.equals(media) && !TmdbModels.TV.equals(media)) throw new IOException("media");
        return getJson(TmdbUrls.discover(media, providers, language, dateGte, page, sort));
    }

    static String trending(boolean day) throws IOException {
        return getJson(TmdbUrls.trending(day));
    }

    static String watchProviders(String media, int id) throws IOException {
        if ((!TmdbModels.MOVIE.equals(media) && !TmdbModels.TV.equals(media)) || id <= 0) throw new IOException("title");
        return getJson(TmdbUrls.watchProviders(media, id));
    }

    static String detail(String media, int id) throws IOException {
        if ((!TmdbModels.MOVIE.equals(media) && !TmdbModels.TV.equals(media)) || id <= 0) throw new IOException("title");
        String path = "/3/" + media + "/" + id + "?language=en-IN&append_to_response="
                + enc("credits,external_ids,watch/providers");
        return getJson("https://api.themoviedb.org" + path);
    }

    static String wikidata(String qid) throws IOException {
        if (qid == null || !qid.matches("Q[0-9]{1,12}")) throw new IOException("wikidata id");
        return getJson("https://www.wikidata.org/w/api.php?action=wbgetentities&props=claims&format=json&ids=" + qid);
    }

    static byte[] image(int size, String path) throws IOException {
        if (path == null || !path.matches("/[A-Za-z0-9._~-]+\\.(jpg|jpeg|png)") || path.contains("..")) {
            throw new IOException("image path");
        }
        int width = size == MemoryPolicy.POSTER_LARGE_PX ? size
                : size == MemoryPolicy.HERO_PX ? size : MemoryPolicy.POSTER_PX;
        return getBytes("https://image.tmdb.org/t/p/w" + width + path, IMAGE_CAP);
    }

    private static String getJson(String url) throws IOException {
        return new String(getBytes(url, JSON_CAP), StandardCharsets.UTF_8);
    }

    private static byte[] getBytes(String url, int cap) throws IOException {
        return read(url, cap, 0);
    }

    private static byte[] read(String value, int cap, int redirects) throws IOException {
        URL url;
        try {
            url = withKey(new URL(value));
        } catch (Exception e) {
            throw new IOException("bad url");
        }
        if (!allowed(url) || redirects > 3) throw new IOException("host");
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setInstanceFollowRedirects(false);
        c.setConnectTimeout(10000);
        c.setReadTimeout(15000);
        c.setRequestProperty("User-Agent", "RaiderLauncher/" + BuildConfig.VERSION_NAME + " (Android TV)");
        c.setRequestProperty("Accept", "application/json,image/*");
        String token = BuildConfig.TMDB_READ_TOKEN.trim();
        if (!token.isEmpty() && "api.themoviedb.org".equalsIgnoreCase(url.getHost())) {
            c.setRequestProperty("Authorization", "Bearer " + token);
        }
        try {
            int code = c.getResponseCode();
            if (code >= 300 && code < 400) {
                String location = c.getHeaderField("Location");
                if (location == null) throw new IOException("redirect");
                return read(new URL(url, location).toString(), cap, redirects + 1);
            }
            if (code < 200 || code >= 300) throw new IOException("HTTP " + code);
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n, total = 0;
                while ((n = in.read(buf)) != -1) {
                    total += n;
                    if (total > cap) throw new IOException("response too large");
                    out.write(buf, 0, n);
                }
                return out.toByteArray();
            }
        } catch (IOException e) {
            String path = url.getPath() == null ? "" : url.getPath();
            Log.w(TAG, path + " " + e.getMessage());
            throw e;
        } finally {
            c.disconnect();
        }
    }

    private static URL withKey(URL url) throws Exception {
        if (!"api.themoviedb.org".equalsIgnoreCase(url.getHost())) return url;
        String key = BuildConfig.TMDB_API_KEY.trim();
        if (key.isEmpty()) return url;
        String query = url.getQuery();
        if (query != null && query.contains("api_key=")) return url;
        String sep = query == null ? "?" : "&";
        return new URL(url.toString() + sep + "api_key=" + URLEncoder.encode(key, "UTF-8"));
    }

    private static boolean allowed(URL url) {
        if (!"https".equalsIgnoreCase(url.getProtocol())) return false;
        String host = url.getHost();
        return "api.themoviedb.org".equalsIgnoreCase(host)
                || "image.tmdb.org".equalsIgnoreCase(host)
                || "www.wikidata.org".equalsIgnoreCase(host)
                || "wikidata.org".equalsIgnoreCase(host);
    }

    private static String enc(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}
