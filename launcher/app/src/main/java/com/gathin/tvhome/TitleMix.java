package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds one row. Telugu and Hindi take about 40% and 35% when they are selected.
 * The rest is the other selected languages, then anything else. Movies and series
 * in a language list are already interleaved by the caller.
 */
final class TitleMix {
    private TitleMix() {}

    static List<TmdbModels.Title> mix(Map<String, List<TmdbModels.Title>> byLanguage,
                                      List<TmdbModels.Title> others, String[] selected, int cap) {
        ArrayList<TmdbModels.Title> out = new ArrayList<>();
        if (cap <= 0) return out;
        String[] langs = selected == null ? new String[0] : selected;
        boolean te = has(langs, "te");
        boolean hi = has(langs, "hi");
        int teQuota = te ? round(cap, 40) : 0;
        int hiQuota = hi ? round(cap, 35) : 0;
        if (teQuota + hiQuota > cap) hiQuota = Math.max(0, cap - teQuota);
        int rest = cap - teQuota - hiQuota;
        ArrayList<String> othersSelected = new ArrayList<>();
        for (String lang : langs) {
            if ("te".equals(lang) || "hi".equals(lang)) continue;
            othersSelected.add(lang);
        }
        LinkedHashMap<String, Integer> quota = new LinkedHashMap<>();
        LinkedHashMap<String, List<TmdbModels.Title>> queues = new LinkedHashMap<>();
        if (te) put(quota, queues, "te", teQuota, byLanguage);
        if (hi) put(quota, queues, "hi", hiQuota, byLanguage);
        if (!othersSelected.isEmpty() && rest > 0) {
            int each = rest / othersSelected.size();
            int extra = rest % othersSelected.size();
            for (int i = 0; i < othersSelected.size(); i++) {
                put(quota, queues, othersSelected.get(i), each + (i < extra ? 1 : 0), byLanguage);
            }
        }
        put(quota, queues, "*", rest == 0 || !othersSelected.isEmpty() ? 0 : rest, null);
        if (others != null) queues.put("*", new ArrayList<>(others));
        else if (!queues.containsKey("*")) queues.put("*", new ArrayList<>());
        if (othersSelected.isEmpty() && rest > 0) quota.put("*", rest);
        takeRoundRobin(out, quota, queues, cap);
        spill(out, queues, cap);
        return out;
    }

    /** Movies and series alternating, ordered by the browse sort inside each type. */
    static List<TmdbModels.Title> arrange(List<TmdbModels.Title> items, String sort) {
        return interleave(items, (a, b) -> compareSort(a, b, sort));
    }

    /** Newest first, movies and series alternating, starting with whichever is newer. */
    static List<TmdbModels.Title> interleaveTypes(List<TmdbModels.Title> items, boolean byPopularity) {
        ArrayList<TmdbModels.Title> movies = new ArrayList<>();
        ArrayList<TmdbModels.Title> shows = new ArrayList<>();
        if (items != null) {
            for (TmdbModels.Title title : items) {
                if (title == null || title.poster.isEmpty()) continue;
                if (title.movie()) movies.add(title);
                else shows.add(title);
            }
        }
        movies.sort((a, b) -> compare(a, b, byPopularity));
        shows.sort((a, b) -> compare(a, b, byPopularity));
        return zip(movies, shows, (a, b) -> compare(a, b, byPopularity));
    }

    private static List<TmdbModels.Title> interleave(List<TmdbModels.Title> items, java.util.Comparator<TmdbModels.Title> order) {
        ArrayList<TmdbModels.Title> movies = new ArrayList<>();
        ArrayList<TmdbModels.Title> shows = new ArrayList<>();
        if (items != null) {
            for (TmdbModels.Title title : items) {
                if (title == null || title.poster.isEmpty()) continue;
                if (title.movie()) movies.add(title);
                else shows.add(title);
            }
        }
        movies.sort(order);
        shows.sort(order);
        return zip(movies, shows, order);
    }

    private static List<TmdbModels.Title> zip(ArrayList<TmdbModels.Title> movies, ArrayList<TmdbModels.Title> shows,
                                             java.util.Comparator<TmdbModels.Title> order) {
        ArrayList<TmdbModels.Title> out = new ArrayList<>();
        int m = 0, s = 0;
        boolean showFirst = !shows.isEmpty() && (movies.isEmpty() || order.compare(shows.get(0), movies.get(0)) < 0);
        boolean movieTurn = !showFirst;
        while (m < movies.size() || s < shows.size()) {
            if (movieTurn && m < movies.size()) out.add(movies.get(m++));
            else if (!movieTurn && s < shows.size()) out.add(shows.get(s++));
            else if (m < movies.size()) out.add(movies.get(m++));
            else if (s < shows.size()) out.add(shows.get(s++));
            movieTurn = !movieTurn;
        }
        return out;
    }

    private static void put(Map<String, Integer> quota, Map<String, List<TmdbModels.Title>> queues,
                            String lang, int count, Map<String, List<TmdbModels.Title>> source) {
        quota.put(lang, Math.max(0, count));
        List<TmdbModels.Title> list = source == null ? null : source.get(lang);
        queues.put(lang, list == null ? new ArrayList<>() : new ArrayList<>(list));
    }

    private static void takeRoundRobin(List<TmdbModels.Title> out, Map<String, Integer> quota,
                                       Map<String, List<TmdbModels.Title>> queues, int cap) {
        boolean moved = true;
        while (out.size() < cap && moved) {
            moved = false;
            for (String lang : queues.keySet()) {
                if (out.size() >= cap) return;
                int allowed = quota.containsKey(lang) ? quota.get(lang) : 0;
                int have = countLang(out, lang);
                if (have >= allowed) continue;
                TmdbModels.Title next = poll(queues.get(lang), out);
                if (next == null) continue;
                out.add(next);
                moved = true;
            }
        }
    }

    private static void spill(List<TmdbModels.Title> out, Map<String, List<TmdbModels.Title>> queues, int cap) {
        boolean moved = true;
        while (out.size() < cap && moved) {
            moved = false;
            for (List<TmdbModels.Title> queue : queues.values()) {
                if (out.size() >= cap) return;
                TmdbModels.Title next = poll(queue, out);
                if (next == null) continue;
                out.add(next);
                moved = true;
            }
        }
    }

    private static TmdbModels.Title poll(List<TmdbModels.Title> queue, List<TmdbModels.Title> used) {
        if (queue == null) return null;
        while (!queue.isEmpty()) {
            TmdbModels.Title title = queue.remove(0);
            if (title == null || title.poster.isEmpty()) continue;
            if (contains(used, title)) continue;
            return title;
        }
        return null;
    }

    private static int countLang(List<TmdbModels.Title> items, String lang) {
        if ("*".equals(lang)) {
            int n = 0;
            for (TmdbModels.Title title : items) {
                if (!"te".equals(title.language) && !"hi".equals(title.language)) n++;
            }
            return n;
        }
        int n = 0;
        for (TmdbModels.Title title : items) if (lang.equals(title.language)) n++;
        return n;
    }

    private static boolean contains(List<TmdbModels.Title> items, TmdbModels.Title title) {
        for (TmdbModels.Title item : items) {
            if (item.id == title.id && item.media.equals(title.media)) return true;
        }
        return false;
    }

    private static int compareSort(TmdbModels.Title a, TmdbModels.Title b, String sort) {
        if ("popularity".equals(sort) || "trending".equals(sort)) return Double.compare(b.popularity, a.popularity);
        if ("rating".equals(sort)) return Double.compare(b.vote, a.vote);
        return compare(a, b, false);
    }

    private static int compare(TmdbModels.Title a, TmdbModels.Title b, boolean byPopularity) {
        if (byPopularity) return Double.compare(b.popularity, a.popularity);
        String ad = a.date == null ? "" : a.date;
        String bd = b.date == null ? "" : b.date;
        int date = bd.compareTo(ad);
        if (date != 0) return date;
        return Double.compare(b.popularity, a.popularity);
    }

    private static boolean has(String[] langs, String code) {
        for (String lang : langs) if (code.equals(lang)) return true;
        return false;
    }

    private static int round(int cap, int percent) {
        return (cap * percent + 50) / 100;
    }
}
