package com.gathin.tvhome;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.BiFunction;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * India time plus four named cities. India is fixed (Asia/Kolkata) and is not a preference.
 * The Hindu calendar master does not hide these clocks.
 */
final class WorldClocks {
    static final String INDIA_ZONE = "Asia/Kolkata";
    static final String INDIA_LABEL = "India";
    static final int SLOTS = 4;
    static final String DREAM = "dream_world_clocks";

    static final class Place {
        final String name, label, zone;
        Place(String name, String label, String zone) {
            this.name = name;
            this.label = label;
            this.zone = zone;
        }
    }

    private WorldClocks() {}

    static String nameKey(int slot) { return "world_clock_" + (slot + 1) + "_name"; }
    static String zoneKey(int slot) { return "world_clock_" + (slot + 1) + "_zone"; }

    static boolean validZone(String id) {
        if (id == null || id.isEmpty()) return false;
        return id.equals(TimeZone.getTimeZone(id).getID());
    }

    static String formatTime(String zoneId, long epochMs) {
        SimpleDateFormat format = new SimpleDateFormat("h:mm a", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone(zoneId));
        return format.format(new Date(epochMs)).replace('\u202F', ' ').replace('\u00A0', ' ');
    }

    /** One line for the Home clock: India, then each configured city. */
    static String homeDetail(BiFunction<String, String, String> prefs, long nowMs) {
        return join(read(prefs, true), read(prefs, false), nowMs, "  \u00B7  ");
    }

    /** One city per line for the screensaver. */
    static String dreamDetail(BiFunction<String, String, String> prefs, long nowMs) {
        return join(read(prefs, true), read(prefs, false), nowMs, "\n");
    }

    static String join(String[] names, String[] zones, long nowMs, String separator) {
        StringBuilder out = new StringBuilder();
        append(out, INDIA_LABEL, INDIA_ZONE, nowMs, separator);
        int n = names == null ? 0 : Math.min(SLOTS, names.length);
        for (int i = 0; i < n; i++) {
            String name = names[i] == null ? "" : names[i].trim();
            String zone = zones == null || i >= zones.length || zones[i] == null ? "" : zones[i].trim();
            if (name.isEmpty() || !validZone(zone)) continue;
            append(out, name, zone, nowMs, separator);
        }
        return out.toString();
    }

    private static void append(StringBuilder out, String label, String zone, long nowMs, String separator) {
        if (out.length() > 0) out.append(separator);
        out.append(label).append(' ').append(formatTime(zone, nowMs));
    }

    private static String[] read(BiFunction<String, String, String> prefs, boolean names) {
        String[] values = new String[SLOTS];
        for (int i = 0; i < SLOTS; i++) {
            String key = names ? nameKey(i) : zoneKey(i);
            String value = prefs == null ? "" : prefs.apply(key, "");
            values[i] = value == null ? "" : value;
        }
        return values;
    }

    /** Open-Meteo geocoding when the network works, otherwise the built-in city list. */
    static List<Place> search(String query) {
        try {
            List<Place> online = searchOnline(query);
            if (!online.isEmpty()) return online;
        } catch (Exception ignored) {
            // The TV can be offline. The typed name still matches the built-in list.
        }
        return matchLocal(query);
    }

    static List<Place> matchLocal(String query) {
        List<Place> out = new ArrayList<>();
        String raw = query == null ? "" : query.trim();
        String q = raw.toLowerCase(Locale.ROOT);
        if (q.length() < 2) return out;
        if (validZone(raw) && raw.indexOf('/') > 0) out.add(new Place(raw, raw, raw));
        for (String[] row : CITIES) {
            if (row[0].toLowerCase(Locale.ROOT).contains(q) || row[1].toLowerCase(Locale.ROOT).contains(q)) {
                out.add(new Place(row[0], row[0] + " \u00B7 " + row[1], row[1]));
            }
            if (out.size() >= 8) break;
        }
        return out;
    }

    static List<Place> searchOnline(String query) throws Exception {
        List<Place> out = new ArrayList<>();
        String q = query == null ? "" : query.trim();
        if (q.length() < 2) return out;
        URL url = new URL("https://geocoding-api.open-meteo.com/v1/search?count=8&language=en&format=json&name="
                + URLEncoder.encode(q, "UTF-8"));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        String body;
        try {
            int code = connection.getResponseCode();
            if (code != 200) throw new java.io.IOException("Geocoding HTTP " + code);
            try (InputStream in = connection.getInputStream(); ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
                byte[] chunk = new byte[4096];
                int n, total = 0;
                while ((n = in.read(chunk)) != -1) {
                    total += n;
                    if (total > 65536) throw new java.io.IOException("Response too large");
                    buf.write(chunk, 0, n);
                }
                body = buf.toString("UTF-8");
            }
        } finally {
            connection.disconnect();
        }
        JSONArray results = new JSONObject(body).optJSONArray("results");
        if (results == null) return out;
        for (int i = 0; i < results.length() && out.size() < 8; i++) {
            JSONObject item = results.getJSONObject(i);
            String zone = item.optString("timezone", "").trim();
            String name = item.optString("name", "").trim();
            if (name.isEmpty() || !validZone(zone)) continue;
            StringBuilder label = new StringBuilder(name);
            for (String key : new String[] {"admin1", "country"}) {
                String value = item.optString(key, "").trim();
                if (!value.isEmpty() && label.indexOf(value) < 0) label.append(", ").append(value);
            }
            label.append(" \u00B7 ").append(zone);
            out.add(new Place(name, label.toString(), zone));
        }
        return out;
    }

    private static final String[][] CITIES = {
            {"Mumbai", "Asia/Kolkata"}, {"Delhi", "Asia/Kolkata"}, {"Bengaluru", "Asia/Kolkata"},
            {"Chennai", "Asia/Kolkata"}, {"Hyderabad", "Asia/Kolkata"}, {"Kolkata", "Asia/Kolkata"},
            {"Dubai", "Asia/Dubai"}, {"Abu Dhabi", "Asia/Dubai"}, {"Doha", "Asia/Qatar"},
            {"Riyadh", "Asia/Riyadh"}, {"Muscat", "Asia/Muscat"}, {"Kuwait City", "Asia/Kuwait"},
            {"Colombo", "Asia/Colombo"}, {"Kathmandu", "Asia/Kathmandu"}, {"Dhaka", "Asia/Dhaka"},
            {"Karachi", "Asia/Karachi"}, {"Singapore", "Asia/Singapore"}, {"Kuala Lumpur", "Asia/Kuala_Lumpur"},
            {"Bangkok", "Asia/Bangkok"}, {"Jakarta", "Asia/Jakarta"}, {"Manila", "Asia/Manila"},
            {"Hong Kong", "Asia/Hong_Kong"}, {"Taipei", "Asia/Taipei"}, {"Shanghai", "Asia/Shanghai"},
            {"Beijing", "Asia/Shanghai"}, {"Seoul", "Asia/Seoul"}, {"Tokyo", "Asia/Tokyo"},
            {"London", "Europe/London"}, {"Dublin", "Europe/Dublin"}, {"Paris", "Europe/Paris"},
            {"Berlin", "Europe/Berlin"}, {"Amsterdam", "Europe/Amsterdam"}, {"Zurich", "Europe/Zurich"},
            {"Rome", "Europe/Rome"}, {"Madrid", "Europe/Madrid"}, {"Lisbon", "Europe/Lisbon"},
            {"Stockholm", "Europe/Stockholm"}, {"Oslo", "Europe/Oslo"}, {"Copenhagen", "Europe/Copenhagen"},
            {"Helsinki", "Europe/Helsinki"}, {"Warsaw", "Europe/Warsaw"}, {"Vienna", "Europe/Vienna"},
            {"Prague", "Europe/Prague"}, {"Athens", "Europe/Athens"}, {"Istanbul", "Europe/Istanbul"},
            {"Moscow", "Europe/Moscow"}, {"Tel Aviv", "Asia/Jerusalem"}, {"Cairo", "Africa/Cairo"},
            {"Nairobi", "Africa/Nairobi"}, {"Johannesburg", "Africa/Johannesburg"}, {"Lagos", "Africa/Lagos"},
            {"New York", "America/New_York"}, {"Washington", "America/New_York"}, {"Toronto", "America/Toronto"},
            {"Chicago", "America/Chicago"}, {"Denver", "America/Denver"}, {"Los Angeles", "America/Los_Angeles"},
            {"San Francisco", "America/Los_Angeles"}, {"Vancouver", "America/Vancouver"},
            {"Mexico City", "America/Mexico_City"}, {"Sao Paulo", "America/Sao_Paulo"},
            {"Buenos Aires", "America/Argentina/Buenos_Aires"}, {"Santiago", "America/Santiago"},
            {"Lima", "America/Lima"}, {"Bogota", "America/Bogota"},
            {"Sydney", "Australia/Sydney"}, {"Melbourne", "Australia/Melbourne"},
            {"Brisbane", "Australia/Brisbane"}, {"Perth", "Australia/Perth"},
            {"Auckland", "Pacific/Auckland"}, {"Honolulu", "Pacific/Honolulu"}
    };
}
