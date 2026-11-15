package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * TMDB watch-provider id to the Android package that can open it, for region IN.
 * Titles whose providers are not installed are left out of the home rows.
 * JioTV+ is not a TMDB provider: it has no entry, so it never keeps a title on screen.
 */
final class ProviderMap {
    static final class Hit {
        final int id;
        final String name;
        final String kind;
        final String packageName;
        final int priority;
        final String[] wikidata;

        Hit(int id, String name, String kind, String packageName, int priority, String[] wikidata) {
            this.id = id;
            this.name = name;
            this.kind = kind;
            this.packageName = packageName;
            this.priority = priority;
            this.wikidata = wikidata == null ? new String[0] : wikidata;
        }
    }

    private static final class Provider {
        final int id;
        final String name;
        final String kind;
        final int priority;
        final String[] packages;
        final String[] wikidata;

        Provider(int id, String name, String kind, int priority, String[] packages, String[] wikidata) {
            this.id = id;
            this.name = name;
            this.kind = kind;
            this.priority = priority;
            this.packages = packages;
            this.wikidata = wikidata;
        }
    }

    private final List<Provider> providers;

    private ProviderMap(List<Provider> providers) {
        this.providers = providers;
    }

    static ProviderMap parse(String json) {
        ArrayList<Provider> list = new ArrayList<>();
        if (json == null || json.isEmpty()) return new ProviderMap(list);
        try {
            JSONArray arr = new JSONObject(json).optJSONArray("providers");
            if (arr == null) return new ProviderMap(list);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                int id = o.optInt("id", 0);
                String name = o.optString("name", "").trim();
                String kind = o.optString("kind", "").trim();
                if (id <= 0 || name.isEmpty() || kind.isEmpty()) continue;
                String[] packages = strings(o.optJSONArray("packages"));
                if (packages.length == 0) continue;
                list.add(new Provider(id, name, kind, o.optInt("priority", 100), packages, strings(o.optJSONArray("wikidata"))));
            }
        } catch (Exception ignored) {
            return new ProviderMap(new ArrayList<>());
        }
        return new ProviderMap(list);
    }

    /** The preferred provider entry for an installed package, or null. */
    Hit forPackage(String packageName) {
        if (packageName == null) return null;
        Provider best = null;
        for (Provider p : providers) {
            if (!lists(p, packageName)) continue;
            if (best == null || p.priority < best.priority) best = p;
        }
        return best == null ? null : hit(best, packageName);
    }

    /** One button per installed app, best priority first. Unknown ids are dropped. */
    List<Hit> forProviders(int[] ids, Set<String> installed) {
        if (ids == null || installed == null || installed.isEmpty()) return Collections.emptyList();
        ArrayList<Hit> out = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        ArrayList<Provider> ordered = new ArrayList<>();
        for (int id : ids) {
            Provider p = find(id);
            if (p != null) ordered.add(p);
        }
        ordered.sort(Comparator.comparingInt(p -> p.priority));
        for (Provider p : ordered) {
            String pkg = firstInstalled(p, installed);
            if (pkg == null || !seen.add(pkg)) continue;
            out.add(hit(p, pkg));
        }
        return out;
    }

    /**
     * Pipe-joined provider ids to pass as {@code with_watch_providers}.
     * Empty when none of the mapped apps are installed, which hides every title.
     */
    String providerQuery(Set<String> installed) {
        if (installed == null || installed.isEmpty()) return "";
        ArrayList<Integer> ids = new ArrayList<>();
        for (Provider p : providers) {
            if (firstInstalled(p, installed) != null && !ids.contains(p.id)) ids.add(p.id);
        }
        Collections.sort(ids);
        StringBuilder b = new StringBuilder();
        for (int id : ids) {
            if (b.length() > 0) b.append('|');
            b.append(id);
        }
        return b.toString();
    }

    String[] wikidataProps() {
        LinkedHashSet<String> props = new LinkedHashSet<>();
        for (Provider p : providers) props.addAll(Arrays.asList(p.wikidata));
        return props.toArray(new String[0]);
    }

    /** Every package named in the map, in file order, without duplicates. */
    String[] packages() {
        LinkedHashSet<String> all = new LinkedHashSet<>();
        for (Provider p : providers) Collections.addAll(all, p.packages);
        return all.toArray(new String[0]);
    }

    private Provider find(int id) {
        for (Provider p : providers) if (p.id == id) return p;
        return null;
    }

    private static boolean lists(Provider p, String packageName) {
        for (String pkg : p.packages) if (packageName.equals(pkg)) return true;
        return false;
    }

    private static String firstInstalled(Provider p, Set<String> installed) {
        for (String pkg : p.packages) if (installed.contains(pkg)) return pkg;
        return null;
    }

    private static Hit hit(Provider p, String packageName) {
        return new Hit(p.id, p.name, p.kind, packageName, p.priority, p.wikidata);
    }

    private static String[] strings(JSONArray array) {
        if (array == null) return new String[0];
        ArrayList<String> out = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            String s = array.optString(i, "").trim();
            if (!s.isEmpty() && !"null".equals(s)) out.add(s);
        }
        return out.toArray(new String[0]);
    }
}
