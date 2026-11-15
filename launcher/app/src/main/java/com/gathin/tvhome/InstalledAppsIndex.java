package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Installed, enabled packages from the provider map that can actually be launched. */
final class InstalledAppsIndex {
    private InstalledAppsIndex() {}

    static Set<String> enabled(Context context, String[] packages) {
        if (context == null || packages == null || packages.length == 0) return Collections.emptySet();
        PackageManager pm = context.getPackageManager();
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String pkg : packages) {
            if (pkg == null || pkg.isEmpty()) continue;
            try {
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                if (!info.enabled) continue;
                Intent launch = pm.getLeanbackLaunchIntentForPackage(pkg);
                if (launch == null) launch = pm.getLaunchIntentForPackage(pkg);
                if (launch == null) continue;
                out.add(pkg);
            } catch (PackageManager.NameNotFoundException ignored) {}
        }
        return out;
    }
}
