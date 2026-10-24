package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Home recommendations: Watch Next when the TV has it, screensaver photos otherwise. */
final class HomeRecs {
    static final String PREF = "home_recommendations";
    static final int MAX = 6;
    /** Click starts the screensaver on the photo strip. */
    static final String SCREENSAVER = "raider:screensaver";
    /** Click opens Launcher Settings on the Photos category. */
    static final String PHOTOS = "raider:photos";
    static final String PLACEHOLDER_TITLE = "No recommendations yet";

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

    /** Caption, then the source name, then a generic title. */
    static String photoTitle(String title, String sourceLabel) {
        if (title != null && !title.trim().isEmpty()) return title.trim();
        if (sourceLabel != null && !sourceLabel.trim().isEmpty()) return sourceLabel.trim();
        return "Photo";
    }

    static Item photoCard(String title, String sourceLabel, String filePath) {
        return new Item(photoTitle(title, sourceLabel), filePath, SCREENSAVER);
    }

    /** Watch Next wins. Photos fill the row when it is empty. A placeholder remains when both are empty. */
    static List<Item> choose(List<Item> watchNext, List<Item> photos) {
        List<Item> programs = cap(watchNext);
        if (!programs.isEmpty()) return programs;
        List<Item> cards = cap(photos);
        if (!cards.isEmpty()) return cards;
        ArrayList<Item> one = new ArrayList<>();
        one.add(new Item(PLACEHOLDER_TITLE, null, PHOTOS));
        return one;
    }
}
