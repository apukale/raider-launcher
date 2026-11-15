package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lists from discover (India, installed providers only), details, and Wikidata ids.
 * A missing key or no installed streaming app returns an empty home and the shelf stays hidden.
 */
final class TmdbRepository {
    interface HomeListener { void onHome(TmdbModels.Home home); }
    interface TitleListener { void onTitle(TmdbModels.Title title); }

    private static final String TAG = "TvHomeTmdb";
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "tmdb-net");
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });
    private static final AtomicInteger GEN = new AtomicInteger();
    private static ProviderMap map;

    private TmdbRepository() {}

    static ProviderMap map(Context context) {
        if (map == null) map = ProviderMap.parse(asset(context));
        return map;
    }

    static void loadHome(Context context, HomeListener listener) {
        Context app = context.getApplicationContext();
        int gen = GEN.incrementAndGet();
        Handler main = new Handler(Looper.getMainLooper());
        IO.execute(() -> {
            TmdbModels.Home home = empty();
            String error = "";
            String note = "";
            try {
                String[] status = new String[1];
                home = loadHomeNow(app, status);
                note = status[0] == null ? "" : status[0];
            } catch (Exception e) {
                error = e.getClass().getSimpleName();
                Log.w(TAG, "home " + error);
                home = staleHome(app);
            }
            if (ContentPrefs.available()) record(app, error.isEmpty() ? note : error);
            TmdbModels.Home delivered = home == null ? empty() : home;
            main.post(() -> {
                if (gen == GEN.get()) listener.onHome(delivered);
            });
        });
    }

    interface BrowseListener { void onTitles(java.util.List<TmdbModels.Title> titles); }

    /** Poster grid for the Movies & Series screen. Filters are the ones chosen on that screen. */
    static void loadBrowse(Context context, String type, String sort, int windowDays, String[] languages, BrowseListener listener) {
        Context app = context.getApplicationContext();
        Handler main = new Handler(Looper.getMainLooper());
        IO.execute(() -> {
            java.util.List<TmdbModels.Title> titles = java.util.Collections.emptyList();
            try {
                if (ContentPrefs.available()) titles = browseNow(app, type, sort, windowDays, languages);
            } catch (Exception e) {
                Log.w(TAG, "browse " + e.getClass().getSimpleName());
            }
            java.util.List<TmdbModels.Title> delivered = titles;
            main.post(() -> listener.onTitles(delivered));
        });
    }

    static void loadTitle(Context context, String media, int id, TitleListener listener) {
        Context app = context.getApplicationContext();
        Handler main = new Handler(Looper.getMainLooper());
        IO.execute(() -> {
            TmdbModels.Title title = null;
            try {
                if (ContentPrefs.available() && id > 0
                        && (TmdbModels.MOVIE.equals(media) || TmdbModels.TV.equals(media))) {
                    Set<String> installed = InstalledAppsIndex.enabled(app, map(app).packages());
                    title = loadTitleNow(app, media, id, map(app), installed);
                }
            } catch (Exception e) {
                Log.w(TAG, "title " + e.getClass().getSimpleName());
            }
            TmdbModels.Title delivered = title;
            main.post(() -> listener.onTitle(delivered));
        });
    }

    private static TmdbModels.Home empty() {
        return new TmdbModels.Home(null, null, null);
    }

    private static TmdbModels.Home loadHomeNow(Context app, String[] status) throws Exception {
        if (!ContentPrefs.available()) return empty();
        SharedPreferences prefs = app.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        if (!ContentPrefs.enabled(prefs)) return empty();
        ProviderMap providers = map(app);
        Set<String> installed = InstalledAppsIndex.enabled(app, providers.packages());
        String query = providers.providerQuery(installed);
        if (query.isEmpty()) {
            status[0] = "No supported streaming app is installed";
            return empty();
        }
        boolean wantTrend = true;
        boolean wantTop = true;
        boolean wantHero = false;
        String[] languages = ContentLanguages.selected(prefs.getString(ContentLanguages.PREF, ContentLanguages.DEFAULT));
        int window = ContentWindow.normalize(prefs.getInt(ContentWindow.PREF, ContentWindow.DEFAULT_DAYS));
        DiskCache cache = cache(app);
        long now = System.currentTimeMillis();
        String today = Recency.today();
        Lists lists = null;
        for (int days : Recency.steps(window)) {
            lists = collect(app, cache, now, today, days, languages, query, providers, installed, window);
            int trendSize = lists.trending.size();
            int topSize = lists.top.size();
            if (Recency.filled(trendSize, topSize, wantTrend, wantTop)) break;
        }
        if (lists == null) return empty();
        List<TmdbModels.Title> trending = wantTrend ? lists.trending : java.util.Collections.emptyList();
        List<TmdbModels.Title> top = wantTop ? lists.top : java.util.Collections.emptyList();
        TmdbModels.Title hero = null;
        if (wantHero) {
            List<TmdbModels.Title> heroPool = !trending.isEmpty() ? trending : top;
            int tries = 0;
            for (TmdbModels.Title candidate : heroPool) {
                if (tries++ == 3) break;
                TmdbModels.Title full = loadTitleNow(app, candidate.media, candidate.id, providers, installed);
                if (full == null) continue;
                if (providers.forProviders(full.providerIds, installed).isEmpty()) continue;
                hero = full.language.isEmpty() ? full.withLanguage(candidate.language) : full;
                break;
            }
        }
        return new TmdbModels.Home(trending, top, hero);
    }

    private static final class Lists {
        final List<TmdbModels.Title> trending;
        final List<TmdbModels.Title> top;
        Lists(List<TmdbModels.Title> trending, List<TmdbModels.Title> top) {
            this.trending = trending;
            this.top = top;
        }
    }

    private static Lists collect(Context app, DiskCache cache, long now, String today, int days, String[] languages,
                                 String query, ProviderMap providers, java.util.Set<String> installed, int window) {
        String from = Recency.minusDays(today, days);
        java.util.LinkedHashMap<String, List<TmdbModels.Title>> movies = new java.util.LinkedHashMap<>();
        java.util.LinkedHashMap<String, List<TmdbModels.Title>> shows = new java.util.LinkedHashMap<>();
        for (String language : languages) {
            movies.put(language, pages(cache, now, TmdbModels.MOVIE, language, from, query));
            shows.put(language, pages(cache, now, TmdbModels.TV, language, from, query));
        }
        java.util.ArrayList<TmdbModels.Title> known = new java.util.ArrayList<>();
        for (List<TmdbModels.Title> list : movies.values()) known.addAll(list);
        for (List<TmdbModels.Title> list : shows.values()) known.addAll(list);
        List<TmdbModels.Title> extras = trendingExtras(app, cache, now, from, window, known, providers, installed);
        java.util.LinkedHashMap<String, List<TmdbModels.Title>> newest = new java.util.LinkedHashMap<>();
        java.util.LinkedHashMap<String, List<TmdbModels.Title>> popular = new java.util.LinkedHashMap<>();
        for (String language : languages) {
            java.util.ArrayList<TmdbModels.Title> both = new java.util.ArrayList<>();
            both.addAll(movies.get(language));
            both.addAll(shows.get(language));
            newest.put(language, TitleMix.interleaveTypes(both, false));
            popular.put(language, TitleMix.interleaveTypes(both, true));
        }
        return new Lists(
                TitleMix.mix(newest, extras, languages, MemoryPolicy.TRENDING_CAP),
                TitleMix.mix(popular, TitleMix.interleaveTypes(extras, true), languages, MemoryPolicy.TOP_CAP));
    }

    private static List<TmdbModels.Title> pages(DiskCache cache, long now, String media, String language, String from, String providers) {
        return pages(cache, now, media, language, from, providers, null, 16);
    }

    private static List<TmdbModels.Title> pages(DiskCache cache, long now, String media, String language, String from, String providers, String sort, int cap) {
        java.util.ArrayList<TmdbModels.Title> out = new java.util.ArrayList<>();
        String sortParam = browseSort(sort, TmdbModels.MOVIE.equals(media));
        for (int page = 1; page <= 3; page++) {
            String key = pageKey(media, language, from, page, providers + "|" + sortParam);
            String body = cache.getText(key, now, CacheTtl.LISTS_MS);
            if (body == null) {
                try {
                    body = TmdbClient.discover(media, providers, language, from, page, sortParam);
                    cache.putText(key, body, now);
                } catch (Exception e) {
                    body = cache.getTextAnyAge(key);
                    Log.w(TAG, "discover " + language + " " + e.getClass().getSimpleName());
                }
            }
            if (body == null) break;
            List<TmdbModels.Title> found = TmdbModels.discover(body, media);
            int added = 0;
            for (TmdbModels.Title title : found) {
                if (title.poster.isEmpty()) continue;
                if (!title.language.isEmpty() && !language.equals(title.language)) continue;
                TmdbModels.Title tagged = title.language.isEmpty() ? title.withLanguage(language) : title;
                if (from != null && !from.isEmpty() && TmdbModels.MOVIE.equals(media) && !tagged.date.isEmpty() && !Recency.onOrAfter(tagged.date, from)) continue;
                out.add(tagged);
                added++;
            }
            if (found.size() < 20 || out.size() >= cap || added == 0) break;
        }
        return out;
    }

    private static String browseSort(String sort, boolean movie) {
        if ("popularity".equals(sort)) return "popularity.desc";
        if ("rating".equals(sort)) return "vote_average.desc";
        return movie ? "primary_release_date.desc" : "first_air_date.desc";
    }

    private static java.util.List<TmdbModels.Title> browseNow(Context app, String type, String sort, int windowDays, String[] languages) {
        ProviderMap providers = map(app);
        java.util.Set<String> installed = InstalledAppsIndex.enabled(app, providers.packages());
        String query = providers.providerQuery(installed);
        if (query.isEmpty() || languages == null || languages.length == 0) return java.util.Collections.emptyList();
        int window = ContentWindow.normalize(windowDays);
        String from = window <= 0 ? "" : Recency.minusDays(Recency.today(), window);
        DiskCache disk = cache(app);
        long now = System.currentTimeMillis();
        boolean movies = type == null || "all".equals(type) || TmdbModels.MOVIE.equals(type);
        boolean shows = type == null || "all".equals(type) || TmdbModels.TV.equals(type);
        if ("trending".equals(sort)) return browseTrending(disk, now, from, window, type, languages, providers, installed);
        java.util.LinkedHashMap<String, java.util.List<TmdbModels.Title>> byLanguage = new java.util.LinkedHashMap<>();
        for (String language : languages) {
            java.util.ArrayList<TmdbModels.Title> both = new java.util.ArrayList<>();
            if (movies) both.addAll(pages(disk, now, TmdbModels.MOVIE, language, from, query, sort, 20));
            if (shows) both.addAll(pages(disk, now, TmdbModels.TV, language, from, query, sort, 20));
            byLanguage.put(language, TitleMix.arrange(both, sort));
        }
        return TitleMix.mix(byLanguage, java.util.Collections.emptyList(), languages, 40);
    }

    private static java.util.List<TmdbModels.Title> browseTrending(DiskCache cache, long now, String from, int window, String type,
                                                                  String[] languages, ProviderMap providers, java.util.Set<String> installed) {
        boolean day = ContentWindow.dailyTrending(window);
        String key = day ? "t2-day" : "t2-week";
        String body = cache.getText(key, now, CacheTtl.LISTS_MS);
        if (body == null) {
            try {
                body = TmdbClient.trending(day);
                cache.putText(key, body, now);
            } catch (Exception e) {
                body = cache.getTextAnyAge(key);
            }
        }
        if (body == null) return java.util.Collections.emptyList();
        java.util.LinkedHashMap<String, java.util.List<TmdbModels.Title>> byLanguage = new java.util.LinkedHashMap<>();
        for (String language : languages) byLanguage.put(language, new java.util.ArrayList<>());
        int budget = 24;
        for (TmdbModels.Title title : TmdbModels.discover(body, TmdbModels.MOVIE)) {
            if (title.poster.isEmpty()) continue;
            if (TmdbModels.MOVIE.equals(type) && !title.movie()) continue;
            if (TmdbModels.TV.equals(type) && title.movie()) continue;
            if (!from.isEmpty() && !title.date.isEmpty() && !Recency.onOrAfter(title.date, from)) continue;
            String language = title.language;
            if (!byLanguage.containsKey(language)) continue;
            int[] left = new int[]{budget};
            if (!onInstalled(cache, now, title, providers, installed, left)) continue;
            budget = left[0];
            byLanguage.get(language).add(title);
            if (budget == 0) break;
        }
        return TitleMix.mix(byLanguage, java.util.Collections.emptyList(), languages, 40);
    }

    private static List<TmdbModels.Title> trendingExtras(Context app, DiskCache cache, long now, String from, int window,
                                                        List<TmdbModels.Title> known, ProviderMap providers, java.util.Set<String> installed) {
        java.util.ArrayList<TmdbModels.Title> out = new java.util.ArrayList<>();
        boolean day = ContentWindow.dailyTrending(window);
        String key = day ? "t2-day" : "t2-week";
        String body = cache.getText(key, now, CacheTtl.LISTS_MS);
        if (body == null) {
            try {
                body = TmdbClient.trending(day);
                cache.putText(key, body, now);
            } catch (Exception e) {
                body = cache.getTextAnyAge(key);
                Log.w(TAG, "trending " + e.getClass().getSimpleName());
            }
        }
        if (body == null) return out;
        int budget = 12;
        for (TmdbModels.Title title : TmdbModels.discover(body, TmdbModels.MOVIE)) {
            if (out.size() >= 12 || title.poster.isEmpty()) continue;
            if (!title.date.isEmpty() && !Recency.onOrAfter(title.date, from)) continue;
            if (contains(known, title) || contains(out, title)) continue;
            int[] left = new int[]{budget};
            if (!onInstalled(cache, now, title, providers, installed, left)) continue;
            budget = left[0];
            out.add(title);
        }
        return out;
    }

    private static boolean onInstalled(DiskCache cache, long now, TmdbModels.Title title, ProviderMap providers,
                                      java.util.Set<String> installed, int[] budget) {
        String key = "wp-" + title.media + "-" + title.id;
        String body = cache.getText(key, now, CacheTtl.DETAILS_MS);
        if (body == null) {
            if (budget[0] <= 0) return false;
            budget[0]--;
            try {
                body = TmdbClient.watchProviders(title.media, title.id);
                cache.putText(key, body, now);
            } catch (Exception e) {
                body = cache.getTextAnyAge(key);
                if (body == null) return false;
            }
        }
        return !providers.forProviders(TmdbModels.indiaProviders(body), installed).isEmpty();
    }

    private static boolean contains(List<TmdbModels.Title> items, TmdbModels.Title title) {
        for (TmdbModels.Title item : items) {
            if (item.id == title.id && item.media.equals(title.media)) return true;
        }
        return false;
    }

    private static String pageKey(String media, String language, String from, int page, String providers) {
        String raw = media + "|" + language + "|" + from + "|" + page + "|" + providers;
        return "d2-" + Integer.toHexString(raw.hashCode());
    }

    private static TmdbModels.Home staleHome(Context app) {
        try {
            if (!ContentPrefs.available()) return empty();
            SharedPreferences prefs = app.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
            ProviderMap providers = map(app);
            java.util.Set<String> installed = InstalledAppsIndex.enabled(app, providers.packages());
            String query = providers.providerQuery(installed);
            if (query.isEmpty()) return empty();
            String[] languages = ContentLanguages.selected(prefs.getString(ContentLanguages.PREF, ContentLanguages.DEFAULT));
            int window = ContentWindow.normalize(prefs.getInt(ContentWindow.PREF, ContentWindow.DEFAULT_DAYS));
            Lists lists = collect(app, cache(app), System.currentTimeMillis(), Recency.today(), window, languages, query, providers, installed, window);
            TmdbModels.Title hero = lists.trending.isEmpty() ? null : lists.trending.get(0);
            return new TmdbModels.Home(lists.trending, lists.top, hero);
        } catch (Exception e) {
            return empty();
        }
    }

    private static TmdbModels.Title loadTitleNow(Context app, String media, int id, ProviderMap providers, Set<String> installed) throws Exception {
        DiskCache cache = cache(app);
        long now = System.currentTimeMillis();
        String key = "detail-" + media + "-" + id;
        String body = cache.getText(key, now, CacheTtl.DETAILS_MS);
        if (body == null) {
            body = TmdbClient.detail(media, id);
            cache.putText(key, body, now);
        }
        TmdbModels.Title title = TmdbModels.detail(body, media);
        if (title == null) return null;
        if (!title.wikidataId.matches("Q[0-9]{1,12}")) return title;
        String extKey = "ext-" + title.wikidataId;
        String extBody = cache.getText(extKey, now, CacheTtl.EXTERNAL_MS);
        if (extBody == null) {
            try {
                extBody = TmdbClient.wikidata(title.wikidataId);
                cache.putText(extKey, extBody, now);
            } catch (Exception e) {
                extBody = cache.getTextAnyAge(extKey);
                Log.w(TAG, "wikidata " + e.getClass().getSimpleName());
            }
        }
        if (extBody == null) return title;
        Map<String, String> claims = WikidataIds.read(extBody, providers.wikidataProps());
        return claims.isEmpty() ? title : title.withExternal(claims);
    }

    private static void record(Context app, String error) {
        app.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE).edit()
                .putLong(ContentPrefs.FETCHED, System.currentTimeMillis())
                .putString(ContentPrefs.ERROR, error == null ? "" : error)
                .apply();
    }

    private static DiskCache cache(Context app) {
        return new DiskCache(new File(app.getCacheDir(), "tmdb"));
    }

    private static String asset(Context context) {
        try (InputStream in = context.getAssets().open("tmdb_providers.json")) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "{\"providers\":[]}";
        }
    }
}
