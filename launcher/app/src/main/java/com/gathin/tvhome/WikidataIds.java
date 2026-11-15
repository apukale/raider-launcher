package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Pulls string claims out of a wbgetentities response. Non-string values are ignored. */
final class WikidataIds {
    private WikidataIds() {}

    static Map<String, String> read(String json, String[] properties) {
        if (json == null || properties == null || properties.length == 0) return Collections.emptyMap();
        try {
            JSONObject entities = new JSONObject(json).optJSONObject("entities");
            if (entities == null) return Collections.emptyMap();
            Iterator<String> keys = entities.keys();
            if (!keys.hasNext()) return Collections.emptyMap();
            JSONObject entity = entities.optJSONObject(keys.next());
            if (entity == null) return Collections.emptyMap();
            JSONObject claims = entity.optJSONObject("claims");
            if (claims == null) return Collections.emptyMap();
            LinkedHashMap<String, String> out = new LinkedHashMap<>();
            for (String prop : properties) {
                if (prop == null || prop.isEmpty()) continue;
                JSONArray arr = claims.optJSONArray(prop);
                if (arr == null || arr.length() == 0) continue;
                JSONObject snak = arr.optJSONObject(0);
                if (snak == null) continue;
                JSONObject main = snak.optJSONObject("mainsnak");
                if (main == null) continue;
                JSONObject value = main.optJSONObject("datavalue");
                if (value == null) continue;
                Object raw = value.opt("value");
                if (raw instanceof String && !((String) raw).isEmpty()) out.put(prop, (String) raw);
            }
            return out;
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }
}
