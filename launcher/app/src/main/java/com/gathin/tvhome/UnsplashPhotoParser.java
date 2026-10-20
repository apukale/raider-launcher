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
        final String description;
        final String altDescription;
        final String tags;
        Entry(String id, String rawUrl, String regularUrl, String author, String downloadLocation,
              String description, String altDescription, String tags) {
            this.id = id; this.rawUrl = rawUrl; this.regularUrl = regularUrl;
            this.author = author; this.downloadLocation = downloadLocation;
            this.description = description; this.altDescription = altDescription; this.tags = tags;
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
            String description = item.optString("description", "");
            String altDescription = item.optString("alt_description", "");
            StringBuilder tags = new StringBuilder();
            JSONArray tagItems = item.optJSONArray("tags");
            if (tagItems != null) for (int j = 0; j < tagItems.length(); j++) {
                JSONObject tag = tagItems.optJSONObject(j);
                if (tag != null) appendTag(tags, tag.optString("title", ""));
            }
            JSONArray previewItems = item.optJSONArray("tags_preview");
            if (previewItems != null) for (int j = 0; j < previewItems.length(); j++) {
                Object preview = previewItems.opt(j);
                if (preview instanceof JSONObject) appendTag(tags, ((JSONObject) preview).optString("title", ""));
                else if (preview instanceof String) appendTag(tags, (String) preview);
            }
            if (!id.isEmpty() && !raw.isEmpty() && !regular.isEmpty() && !download.isEmpty())
                out.add(new Entry(id, raw, regular, author, download, description, altDescription, tags.toString()));
        }
        return out;
    }

    private static void appendTag(StringBuilder tags, String value) {
        if (value == null || value.isEmpty()) return;
        if (tags.length() > 0) tags.append(' ');
        tags.append(value);
    }

    static String sizedUrl(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        return raw + (raw.contains("?") ? "&" : "?") + "w=1920&h=1080&fit=crop&q=80&fm=jpg";
    }
}
