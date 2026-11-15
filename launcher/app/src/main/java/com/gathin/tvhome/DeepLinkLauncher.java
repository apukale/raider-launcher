package com.gathin.tvhome;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

/**
 * Opens a title in the pinned app. CLEAR_TASK is required on this TCL: a warm task
 * swallows the URI and lands on the app's home. A null URI opens the app itself.
 */
final class DeepLinkLauncher {
    private DeepLinkLauncher() {}

    static String uriFor(ProviderMap.Hit hit, TmdbModels.Title title) {
        if (hit == null || title == null) return null;
        String raw = null;
        for (String prop : hit.wikidata) {
            String value = title.external.get(prop);
            if (value != null && !value.isEmpty()) {
                raw = value;
                break;
            }
        }
        return DeepLinkUris.build(hit.kind, title.movie(), title.name, raw);
    }

    static boolean open(Context context, String pkg, String uri) {
        if (context == null || pkg == null || pkg.isEmpty()) return false;
        try {
            Intent intent;
            if (uri == null || uri.isEmpty()) {
                PackageManager pm = context.getPackageManager();
                intent = pm.getLeanbackLaunchIntentForPackage(pkg);
                if (intent == null) intent = pm.getLaunchIntentForPackage(pkg);
                if (intent == null) return false;
            } else {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage(pkg);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }

    /** Deep link when an id is known, otherwise the app. A second miss tells the user. */
    static void openTitle(Context context, ProviderMap.Hit hit, TmdbModels.Title title) {
        if (context == null || hit == null) return;
        String uri = uriFor(hit, title);
        if (open(context, hit.packageName, uri)) return;
        if (uri != null && open(context, hit.packageName, null)) return;
        Toast.makeText(context, R.string.not_available, Toast.LENGTH_SHORT).show();
    }
}
