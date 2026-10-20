package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Small parsing helper for the Unsplash random photo response. */
final class UnsplashPhotoParser {
    static final class Entry {
        final String id;
        final String rawUrl;
        final String regularUrl;
        final String author;
        final String downloadLocation;
        Entry(String id, String rawUrl, String regularUrl, String author, String downloadLocation) {
            this.id = id; this.rawUrl = rawUrl; this.regularUrl = regularUrl;
            this.author = author; this.downloadLocation = downloadLocation;
        }
    }

    private UnsplashPhotoParser() {}

    static List<Entry> parse(String json) throws Exception {
        JSONArray items = new JSONArray(json);
        ArrayList<Entry> out = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            JSONObject urls = item.optJSONObject("urls");
            JSONObject user = item.optJSONObject("user");
            JSONObject links = item.optJSONObject("links");
            if (urls == null || user == null || links == null) continue;
            String id = item.optString("id", "");
            String raw = urls.optString("raw", "");
            String regular = urls.optString("regular", "");
            String author = user.optString("name", "");
            String download = links.optString("download_location", "");
            if (!id.isEmpty() && !raw.isEmpty() && !regular.isEmpty() && !download.isEmpty())
                out.add(new Entry(id, raw, regular, author, download));
        }
        return out;
    }

    static String sizedUrl(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        return raw + (raw.contains("?") ? "&" : "?") + "w=1920&h=1080&fit=crop&q=80&fm=jpg";
    }
}
