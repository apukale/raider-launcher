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
        if (!ContentPrefs.showsHome(prefs)) return empty();
        ProviderMap providers = map(app);
        Set<String> installed = InstalledAppsIndex.enabled(app, providers.packages());
        String query = providers.providerQuery(installed);
        if (query.isEmpty()) {
            status[0] = "No supported streaming app is installed";
            return empty();
        }
        boolean wantTrend = ContentPrefs.on(prefs, ContentPrefs.HERO) || ContentPrefs.on(prefs, ContentPrefs.TRENDING);
        boolean wantTop = ContentPrefs.on(prefs, ContentPrefs.TOP10);
        boolean wantHero = ContentPrefs.on(prefs, ContentPrefs.HERO);
        DiskCache cache = cache(app);
        long now = System.currentTimeMillis();
        List<TmdbModels.Title> trending = wantTrend
                ? TmdbModels.merge(list(cache, now, "movie-pop-" + query, TmdbModels.MOVIE, "popularity.desc", query),
                list(cache, now, "tv-pop-" + query, TmdbModels.TV, "popularity.desc", query), MemoryPolicy.TRENDING_CAP)
                : java.util.Collections.emptyList();
        List<TmdbModels.Title> top = wantTop
                ? TmdbModels.merge(list(cache, now, "movie-top-" + query, TmdbModels.MOVIE, "vote_count.desc", query),
                list(cache, now, "tv-top-" + query, TmdbModels.TV, "vote_count.desc", query), MemoryPolicy.TOP_CAP)
                : java.util.Collections.emptyList();
        TmdbModels.Title hero = null;
        if (wantHero) {
            int tries = 0;
            for (TmdbModels.Title candidate : trending) {
                if (tries++ == 3) break;
                TmdbModels.Title full = loadTitleNow(app, candidate.media, candidate.id, providers, installed);
                if (full == null) continue;
                if (providers.forProviders(full.providerIds, installed).isEmpty()) continue;
                hero = full;
                break;
            }
        }
        return new TmdbModels.Home(trending, top, hero);
    }

    private static TmdbModels.Home staleHome(Context app) {
        try {
            if (!ContentPrefs.available()) return empty();
            SharedPreferences prefs = app.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
            ProviderMap providers = map(app);
            String query = providers.providerQuery(InstalledAppsIndex.enabled(app, providers.packages()));
            if (query.isEmpty()) return empty();
            DiskCache cache = cache(app);
            List<TmdbModels.Title> trending = TmdbModels.merge(
                    discoverAny(cache, "movie-pop-" + query, TmdbModels.MOVIE),
                    discoverAny(cache, "tv-pop-" + query, TmdbModels.TV), MemoryPolicy.TRENDING_CAP);
            List<TmdbModels.Title> top = TmdbModels.merge(
                    discoverAny(cache, "movie-top-" + query, TmdbModels.MOVIE),
                    discoverAny(cache, "tv-top-" + query, TmdbModels.TV), MemoryPolicy.TOP_CAP);
            return new TmdbModels.Home(trending, top, trending.isEmpty() ? null : trending.get(0));
        } catch (Exception e) {
            return empty();
        }
    }

    private static List<TmdbModels.Title> list(DiskCache cache, long now, String key, String media, String sort, String query) throws Exception {
        String body = cache.getText(key, now, CacheTtl.LISTS_MS);
        if (body == null) {
            body = TmdbClient.discover(media, sort, query);
            cache.putText(key, body, now);
        }
        return TmdbModels.discover(body, media);
    }

    private static List<TmdbModels.Title> discoverAny(DiskCache cache, String key, String media) {
        return TmdbModels.discover(cache.getTextAnyAge(key), media);
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
