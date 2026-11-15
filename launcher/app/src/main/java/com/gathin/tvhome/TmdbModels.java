package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** TMDB JSON in, plain titles out. No Android types, so the parsers run in unit tests. */
final class TmdbModels {
    static final String CREDIT = "Data from TMDB \u00B7 JustWatch";
    static final String MOVIE = "movie";
    static final String TV = "tv";
    /** Rent and buy are not "on an installed app" for these rows. */
    private static final String[] WATCH_BUCKETS = {"flatrate", "free", "ads"};
    private static final Map<Integer, String> GENRES = genres();

    static final class Cast {
        final String name;
        final String character;
        final String profile;

        Cast(String name, String character, String profile) {
            this.name = name == null ? "" : name;
            this.character = character == null ? "" : character;
            this.profile = cleanPath(profile);
        }
    }

    static final class Title {
        final String media;
        final int id;
        final String name;
        final String overview;
        final String poster;
        final String backdrop;
        final String date;
        final double vote;
        final double popularity;
        final int runtime;
        final int[] genreIds;
        final String[] genres;
        final int[] providerIds;
        final Cast[] cast;
        final String wikidataId;
        final String language;
        final Map<String, String> external;

        Title(String media, int id, String name, String overview, String poster, String backdrop,
              String date, double vote, double popularity, int runtime, int[] genreIds, String[] genres,
              int[] providerIds, Cast[] cast, String wikidataId, String language, Map<String, String> external) {
            this.media = MOVIE.equals(media) ? MOVIE : TV;
            this.id = id;
            this.name = name == null ? "" : name;
            this.overview = overview == null ? "" : overview.trim();
            this.poster = cleanPath(poster);
            this.backdrop = cleanPath(backdrop);
            this.date = date == null ? "" : date;
            this.vote = vote;
            this.popularity = popularity;
            this.runtime = Math.max(0, runtime);
            this.genreIds = genreIds == null ? new int[0] : genreIds;
            this.genres = genres == null ? new String[0] : genres;
            this.providerIds = providerIds == null ? new int[0] : providerIds;
            this.cast = cast == null ? new Cast[0] : cast;
            this.wikidataId = wikidataId == null ? "" : wikidataId;
            this.language = language == null ? "" : language.trim().toLowerCase(Locale.US);
            this.external = external == null || external.isEmpty()
                    ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(external));
        }

        boolean movie() { return MOVIE.equals(media); }

        String year() { return yearOf(date); }

        Title withExternal(Map<String, String> more) {
            return new Title(media, id, name, overview, poster, backdrop, date, vote, popularity,
                    runtime, genreIds, genres, providerIds, cast, wikidataId, language, more);
        }

        Title withLanguage(String code) {
            return new Title(media, id, name, overview, poster, backdrop, date, vote, popularity,
                    runtime, genreIds, genres, providerIds, cast, wikidataId, code, external);
        }
    }

    static final class Home {
        final List<Title> trending;
        final List<Title> top10;
        final Title hero;

        Home(List<Title> trending, List<Title> top10, Title hero) {
            this.trending = trending == null ? Collections.emptyList() : trending;
            this.top10 = top10 == null ? Collections.emptyList() : top10;
            this.hero = hero;
        }

        boolean empty() {
            return trending.isEmpty() && top10.isEmpty() && hero == null;
        }
    }

    private TmdbModels() {}

    static List<Title> discover(String json, String media) {
        ArrayList<Title> out = new ArrayList<>();
        if (json == null || json.isEmpty()) return out;
        try {
            JSONArray results = new JSONObject(json).optJSONArray("results");
            if (results == null) return out;
            for (int i = 0; i < results.length(); i++) {
                JSONObject o = results.optJSONObject(i);
                Title title = light(o, media);
                if (title != null) out.add(title);
            }
        } catch (Exception ignored) {}
        return out;
    }

    static Title detail(String json, String media) {
        if (json == null || json.isEmpty()) return null;
        try {
            JSONObject o = new JSONObject(json);
            String name = text(o, MOVIE.equals(media) ? "title" : "name");
            if (name.isEmpty()) name = text(o, "title");
            if (name.isEmpty()) name = text(o, "name");
            int id = o.optInt("id", 0);
            if (id <= 0 || name.isEmpty()) return null;
            JSONArray genres = o.optJSONArray("genres");
            String[] names = genreNames(genres);
            int[] genreIds = new int[genres == null ? 0 : genres.length()];
            if (genres != null) {
                for (int i = 0; i < genres.length(); i++) {
                    JSONObject g = genres.optJSONObject(i);
                    genreIds[i] = g == null ? 0 : g.optInt("id", 0);
                }
            }
            int runtime = o.optInt("runtime", 0);
            if (runtime <= 0) {
                JSONArray runs = o.optJSONArray("episode_run_time");
                if (runs != null && runs.length() > 0) runtime = runs.optInt(0, 0);
            }
            String date = text(o, MOVIE.equals(media) ? "release_date" : "first_air_date");
            if (date.isEmpty()) date = text(o, "release_date");
            if (date.isEmpty()) date = text(o, "first_air_date");
            String wiki = "";
            JSONObject external = o.optJSONObject("external_ids");
            if (external != null) wiki = text(external, "wikidata_id");
            return new Title(media, id, name, text(o, "overview"), text(o, "poster_path"), text(o, "backdrop_path"),
                    date, o.optDouble("vote_average", 0), o.optDouble("popularity", 0), runtime, genreIds, names,
                    providers(o.optJSONObject("watch/providers")), cast(o.optJSONObject("credits")), wiki,
                    text(o, "original_language"), null);
        } catch (Exception ignored) {
            return null;
        }
    }

    static List<Title> merge(List<Title> movies, List<Title> shows, int cap) {
        ArrayList<Title> all = new ArrayList<>();
        if (movies != null) all.addAll(movies);
        if (shows != null) all.addAll(shows);
        all.sort((a, b) -> Double.compare(b.popularity, a.popularity));
        ArrayList<Title> out = new ArrayList<>();
        for (Title title : all) {
            if (title.poster.isEmpty()) continue;
            boolean dup = false;
            for (Title kept : out) {
                if (kept.id == title.id && kept.media.equals(title.media)) { dup = true; break; }
            }
            if (dup) continue;
            out.add(title);
            if (cap > 0 && out.size() >= cap) break;
        }
        return out;
    }

    static String runtimeLabel(int minutes) {
        if (minutes <= 0) return "";
        int h = minutes / 60;
        int m = minutes % 60;
        if (h == 0) return m + "m";
        if (m == 0) return h + "h";
        return h + "h " + m + "m";
    }

    static String yearOf(String date) {
        if (date == null || date.length() < 4) return "";
        String year = date.substring(0, 4);
        return year.matches("[0-9]{4}") ? year : "";
    }

    static String ratingLabel(double vote) {
        if (vote <= 0) return "";
        return String.format(Locale.US, "%.1f", vote);
    }

    static String cardCaption(Title title) {
        if (title == null) return "";
        String kind = title.movie() ? "Movie" : "Series";
        String year = title.year();
        return year.isEmpty() ? kind : kind + " \u00B7 " + year;
    }

    /** Subscription providers in India. Rent and buy are ignored. The root is a watch-provider payload. */
    static int[] indiaProviders(String json) {
        if (json == null || json.isEmpty()) return new int[0];
        try {
            return providers(new JSONObject(json));
        } catch (Exception ignored) {
            return new int[0];
        }
    }

    static String genreLine(Title title) {
        if (title == null) return "";
        ArrayList<String> names = new ArrayList<>();
        if (title.genres.length > 0) {
            for (String name : title.genres) if (name != null && !name.isEmpty()) names.add(name);
        } else {
            for (int id : title.genreIds) {
                String name = GENRES.get(id);
                if (name != null) names.add(name);
            }
        }
        StringBuilder b = new StringBuilder();
        int n = 0;
        for (String name : names) {
            if (n == 3) break;
            if (b.length() > 0) b.append(" \u00B7 ");
            b.append(name);
            n++;
        }
        return b.toString();
    }

    private static Title light(JSONObject o, String media) {
        if (o == null) return null;
        if ("person".equals(text(o, "media_type"))) return null;
        int id = o.optInt("id", 0);
        String name = text(o, "title");
        if (name.isEmpty()) name = text(o, "name");
        if (id <= 0 || name.isEmpty()) return null;
        String date = text(o, "release_date");
        if (date.isEmpty()) date = text(o, "first_air_date");
        JSONArray ids = o.optJSONArray("genre_ids");
        int[] genreIds = new int[ids == null ? 0 : ids.length()];
        if (ids != null) for (int i = 0; i < ids.length(); i++) genreIds[i] = ids.optInt(i, 0);
        String kind = media;
        String listed = text(o, "media_type");
        if (MOVIE.equals(listed) || TV.equals(listed)) kind = listed;
        return new Title(kind, id, name, text(o, "overview"), text(o, "poster_path"), text(o, "backdrop_path"),
                date, o.optDouble("vote_average", 0), o.optDouble("popularity", 0), 0, genreIds, null,
                null, null, "", text(o, "original_language"), null);
    }

    private static int[] providers(JSONObject watch) {
        if (watch == null) return new int[0];
        JSONObject results = watch.optJSONObject("results");
        if (results == null) return new int[0];
        JSONObject india = results.optJSONObject("IN");
        if (india == null) return new int[0];
        ArrayList<Integer> ids = new ArrayList<>();
        for (String bucket : WATCH_BUCKETS) {
            JSONArray arr = india.optJSONArray(bucket);
            if (arr == null) continue;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                int id = o.optInt("provider_id", 0);
                if (id > 0 && !ids.contains(id)) ids.add(id);
            }
        }
        int[] out = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) out[i] = ids.get(i);
        return out;
    }

    private static Cast[] cast(JSONObject credits) {
        if (credits == null) return new Cast[0];
        JSONArray arr = credits.optJSONArray("cast");
        if (arr == null) return new Cast[0];
        ArrayList<Cast> out = new ArrayList<>();
        for (int i = 0; i < arr.length() && out.size() < MemoryPolicy.CAST_CAP; i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String name = text(o, "name");
            if (name.isEmpty()) continue;
            out.add(new Cast(name, text(o, "character"), text(o, "profile_path")));
        }
        return out.toArray(new Cast[0]);
    }

    private static String[] genreNames(JSONArray genres) {
        if (genres == null) return new String[0];
        ArrayList<String> names = new ArrayList<>();
        for (int i = 0; i < genres.length(); i++) {
            JSONObject g = genres.optJSONObject(i);
            if (g == null) continue;
            String name = text(g, "name");
            if (!name.isEmpty()) names.add(name);
        }
        return names.toArray(new String[0]);
    }

    private static String text(JSONObject o, String key) {
        if (o == null || o.isNull(key)) return "";
        String s = o.optString(key, "");
        return "null".equals(s) ? "" : s.trim();
    }

    private static String cleanPath(String path) {
        if (path == null) return "";
        String s = path.trim();
        if (s.isEmpty() || "null".equals(s) || s.contains("..") || !s.startsWith("/")) return "";
        if (!s.matches("/[A-Za-z0-9._~-]+\\.(jpg|jpeg|png)")) return "";
        return s;
    }

    private static Map<Integer, String> genres() {
        HashMap<Integer, String> map = new HashMap<>();
        map.put(28, "Action");
        map.put(12, "Adventure");
        map.put(16, "Animation");
        map.put(35, "Comedy");
        map.put(80, "Crime");
        map.put(99, "Documentary");
        map.put(18, "Drama");
        map.put(10751, "Family");
        map.put(14, "Fantasy");
        map.put(36, "History");
        map.put(27, "Horror");
        map.put(10402, "Music");
        map.put(9648, "Mystery");
        map.put(10749, "Romance");
        map.put(878, "Science Fiction");
        map.put(10770, "TV Movie");
        map.put(53, "Thriller");
        map.put(10752, "War");
        map.put(37, "Western");
        map.put(10759, "Action & Adventure");
        map.put(10762, "Kids");
        map.put(10763, "News");
        map.put(10764, "Reality");
        map.put(10765, "Sci-Fi & Fantasy");
        map.put(10766, "Soap");
        map.put(10767, "Talk");
        map.put(10768, "War & Politics");
        return map;
    }
}
