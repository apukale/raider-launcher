package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Home recommendations preference and the cap for a Watch Next row. */
final class HomeRecs {
    static final String PREF = "home_recommendations";
    static final int MAX = 6;

    static final class Item {
        final String title, poster, intent;
        Item(String title, String poster, String intent) {
            this.title = title;
            this.poster = poster;
            this.intent = intent;
        }
    }

    private HomeRecs() {}

    /** Missing key stays on, matching the other Home toggles. */
    static boolean shown(boolean contains, boolean stored) { return !contains || stored; }

    static List<Item> cap(List<Item> items) {
        if (items == null || items.isEmpty()) return Collections.emptyList();
        ArrayList<Item> out = new ArrayList<>();
        for (Item item : items) {
            if (item == null || item.title == null || item.title.trim().isEmpty()) continue;
            out.add(item);
            if (out.size() >= MAX) break;
        }
        return out;
    }
}
