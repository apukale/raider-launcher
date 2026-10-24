package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Which packages Free RAM may close. Foreground work, this launcher, system packages, and the
 * current default home are left alone. Cached processes go first, heaviest first.
 */
final class RamFree {
    /** {@link android.app.ActivityManager.RunningAppProcessInfo#IMPORTANCE_SERVICE}. */
    static final int SERVICE = 300;
    /** {@link android.app.ActivityManager.RunningAppProcessInfo#IMPORTANCE_CACHED}. */
    static final int CACHED = 400;

    static final class Proc {
        final String pkg;
        final int importance;
        final long pssBytes;
        final boolean system, self, home;

        Proc(String pkg, int importance, long pssBytes, boolean system, boolean self, boolean home) {
            this.pkg = pkg;
            this.importance = importance;
            this.pssBytes = pssBytes;
            this.system = system;
            this.self = self;
            this.home = home;
        }
    }

    private RamFree() {}

    static List<String> targets(List<Proc> procs) {
        Map<String, Acc> byPkg = new LinkedHashMap<>();
        if (procs != null) {
            for (Proc proc : procs) {
                if (proc == null || proc.pkg == null || proc.pkg.trim().isEmpty()) continue;
                String pkg = proc.pkg.trim();
                Acc acc = byPkg.get(pkg);
                if (acc == null) {
                    acc = new Acc();
                    byPkg.put(pkg, acc);
                }
                acc.pss += Math.max(0L, proc.pssBytes);
                if (proc.importance > acc.importance) acc.importance = proc.importance;
                if (proc.system || proc.self || proc.home) acc.blocked = true;
                if (proc.importance > 0 && proc.importance < SERVICE) acc.blocked = true;
            }
        }
        ArrayList<String> pkgs = new ArrayList<>();
        for (Map.Entry<String, Acc> entry : byPkg.entrySet()) {
            if (!entry.getValue().blocked) pkgs.add(entry.getKey());
        }
        pkgs.sort((a, b) -> {
            Acc left = byPkg.get(a);
            Acc right = byPkg.get(b);
            int group = Integer.compare(group(right.importance), group(left.importance));
            if (group != 0) return group;
            return Long.compare(right.pss, left.pss);
        });
        return pkgs;
    }

    /** Cached processes sort ahead of other background work. Unknown importance stays in the later group. */
    private static int group(int importance) {
        return importance >= CACHED ? 1 : 0;
    }

    private static final class Acc {
        long pss;
        int importance;
        boolean blocked;
    }
}
