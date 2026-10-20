package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.usage.StorageStatsManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.provider.Settings.Global;
import android.provider.Settings;
import android.text.InputType;
import android.text.format.Formatter;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Remote-friendly settings owned by the launcher; system settings stay one row away. */
public final class LauncherSettingsActivity extends Activity {
    private static final String PRIVATE_DNS_MODE = "private_dns_mode";
    private static final String PRIVATE_DNS_SPECIFIER = "private_dns_specifier";
    private static final String[] DNS_HOSTS = {
            "", "", "dns.adguard-dns.com", "family.adguard-dns.com", "dns.google",
            "one.one.one.one", "family-blocking.cloudflare-dns.com", "dns.quad9.net"
    };
    private static final String[] DNS_LABELS = {
            "Off", "Automatic", "AdGuard", "AdGuard Family", "Google", "Cloudflare",
            "Cloudflare Family", "Quad9"
    };
    private SharedPreferences choices;
    private final TextView[] columnChoices = new TextView[4];
    private TextView animationsChoice;
    private TextView advancedChoice;
    private TextView followThemeChoice;
    private TextView dnsChoice;
    private TextView defaultHomeChoice;
    private TextView clearCacheChoice;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean clearingCache;
    private int columns;
    private boolean animations;
    private boolean advancedEffects;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        Config config = Config.load(this);
        columns = config.columns;
        animations = config.animations;
        advancedEffects = config.animations && config.advancedEffects;

        int[] ids = {R.id.columns4, R.id.columns5, R.id.columns6, R.id.columns7};
        for (int i = 0; i < ids.length; i++) {
            final int value = i + 4;
            columnChoices[i] = findViewById(ids[i]);
            columnChoices[i].setOnClickListener(v -> {
                if (columns == value) return;
                columns = value;
                choices.edit().putInt("columns", value).apply();
                setResult(RESULT_OK);
                refresh();
            });
        }
        animationsChoice = findViewById(R.id.animationsChoice);
        animationsChoice.setOnClickListener(v -> {
            animations = !animations;
            if (!animations) advancedEffects = false;
            choices.edit().putBoolean("animations", animations)
                    .putBoolean("advanced_effects", advancedEffects).apply();
            setResult(RESULT_OK);
            refresh();
        });
        advancedChoice = findViewById(R.id.advancedChoice);
        advancedChoice.setOnClickListener(v -> {
            if (advancedEffects) {
                setAdvanced(false);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.advanced_effects)
                        .setMessage(R.string.advanced_effects_warning)
                        .setPositiveButton(R.string.enable, (dialog, which) -> setAdvanced(true))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
        followThemeChoice = findViewById(R.id.followThemeChoice);
        followThemeChoice.setOnClickListener(v -> {
            if (!hasOnTvChoices()) return;
            choices.edit().remove("columns").remove("animations").remove("advanced_effects").commit();
            Config current = Config.load(this);
            columns = current.columns;
            animations = current.animations;
            advancedEffects = current.animations && current.advancedEffects;
            setResult(RESULT_OK);
            refresh();
        });
        defaultHomeChoice = findViewById(R.id.defaultHomeChoice);
        defaultHomeChoice.setOnClickListener(v -> {
            Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
            open(new Intent(Settings.ACTION_HOME_SETTINGS));
        });
        dnsChoice = findViewById(R.id.dnsChoice);
        dnsChoice.setOnClickListener(v -> showDnsChoices());
        clearCacheChoice = findViewById(R.id.clearCacheChoice);
        clearCacheChoice.setOnClickListener(v -> clearAppCache());
        clearCacheChoice.setText(R.string.storage_summary_unavailable);
        findViewById(R.id.tvSettingsChoice).setOnClickListener(v ->
                open(new Intent(Settings.ACTION_SETTINGS)));
        findViewById(R.id.resetOrderChoice).setOnClickListener(v ->
                new AlertDialog.Builder(this).setTitle(R.string.reset_order)
                        .setMessage(R.string.reset_order_confirm)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            getSharedPreferences("app_layout", MODE_PRIVATE).edit()
                                    .remove("custom_order").remove("ordered_components").apply();
                            Toast.makeText(this, R.string.reset_order, Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                        }).setNegativeButton(android.R.string.cancel, null).show());
        findViewById(R.id.unhideAppsChoice).setOnClickListener(v -> showHiddenApps());
        refresh();
        columnChoices[Math.max(0, Math.min(columns - 4, 3))].requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStorageSummary();
    }

    private void setAdvanced(boolean enabled) {
        advancedEffects = enabled;
        choices.edit().putBoolean("advanced_effects", enabled).apply();
        setResult(RESULT_OK);
        refresh();
    }

    private void refresh() {
        for (int i = 0; i < columnChoices.length; i++) {
            columnChoices[i].setSelected(columns == i + 4);
        }
        animationsChoice.setText(getString(R.string.animations_state,
                getString(animations ? R.string.on : R.string.off)));
        advancedChoice.setText(getString(R.string.advanced_effects_state,
                getString(advancedEffects ? R.string.on : R.string.off)));
        advancedChoice.setEnabled(animations);
        advancedChoice.setAlpha(animations ? 1f : 0.5f);
        followThemeChoice.setSelected(!hasOnTvChoices());
        dnsChoice.setText(getString(R.string.network_dns_current, currentDnsValue()));
        defaultHomeChoice.setText(getString(R.string.default_home_current, currentHomeLabel()));
    }

    private boolean hasOnTvChoices() {
        return choices.contains("columns") || choices.contains("animations")
                || choices.contains("advanced_effects");
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }

    private static final class StorageSnapshot {
        final long freeBytes;
        final long totalBytes;
        final Long cacheBytes;

        StorageSnapshot(long freeBytes, long totalBytes, Long cacheBytes) {
            this.freeBytes = freeBytes;
            this.totalBytes = totalBytes;
            this.cacheBytes = cacheBytes;
        }
    }

    private void updateStorageSummary() {
        new Thread(() -> {
            StorageSnapshot snapshot = readStorageSnapshot();
            mainHandler.post(() -> {
                if (isFinishing() || clearCacheChoice == null || clearingCache) return;
                clearCacheChoice.setText(storageSummary(snapshot));
            });
        }, "launcher-storage-stats").start();
    }

    private StorageSnapshot readStorageSnapshot() {
        long freeBytes = -1L;
        long totalBytes = -1L;
        try {
            StorageStatsManager storageStatsManager = getSystemService(StorageStatsManager.class);
            if (storageStatsManager != null) {
                freeBytes = storageStatsManager.getFreeBytes(StorageManager.UUID_DEFAULT);
                totalBytes = storageStatsManager.getTotalBytes(StorageManager.UUID_DEFAULT);
            }
        } catch (Throwable ignored) {
            // Some TV builds do not expose storage stats consistently; use the data partition below.
        }
        if (freeBytes < 0 || totalBytes < 0) {
            try {
                StatFs stat = new StatFs(Environment.getDataDirectory().getAbsolutePath());
                if (freeBytes < 0) freeBytes = stat.getAvailableBytes();
                if (totalBytes < 0) totalBytes = stat.getTotalBytes();
            } catch (Throwable ignored) {
                // Keep whichever values were available.
            }
        }
        return new StorageSnapshot(freeBytes, totalBytes, readCacheBytes());
    }

    private Long readCacheBytes() {
        try {
            AppOpsManager appOps = getSystemService(AppOpsManager.class);
            if (appOps == null || appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(), getPackageName()) != AppOpsManager.MODE_ALLOWED) {
                return null;
            }
            StorageStatsManager stats = getSystemService(StorageStatsManager.class);
            if (stats == null) return null;
            return stats.queryStatsForUser(StorageManager.UUID_DEFAULT, Process.myUserHandle())
                    .getCacheBytes();
        } catch (SecurityException ignored) {
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String storageSummary(StorageSnapshot snapshot) {
        String cache = snapshot.cacheBytes == null
                ? getString(R.string.cache_size_unavailable)
                : Formatter.formatShortFileSize(this, snapshot.cacheBytes);
        if (snapshot.freeBytes < 0 || snapshot.totalBytes < 0) {
            return getString(R.string.storage_free_unavailable, cache);
        }
        return getString(R.string.storage_summary,
                Formatter.formatShortFileSize(this, snapshot.freeBytes),
                Formatter.formatShortFileSize(this, snapshot.totalBytes), cache);
    }

    private void clearAppCache() {
        if (clearingCache) return;
        clearingCache = true;
        clearCacheChoice.setText(R.string.clearing_cache);
        new Thread(() -> {
            StorageSnapshot before = readStorageSnapshot();
            boolean cleared = tryFreeStorageAndNotify();
            if (!cleared) cleared = tryAllocateStorage();
            // Allow package manager cache deletion to settle before measuring the result.
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            StorageSnapshot after = readStorageSnapshot();
            final boolean attempted = cleared;
            mainHandler.post(() -> {
                clearingCache = false;
                if (isFinishing()) return;
                clearCacheChoice.setText(storageSummary(after));
                showCacheClearResult(before, after, attempted);
            });
        }, "launcher-clear-cache").start();
    }

    private boolean tryFreeStorageAndNotify() {
        try {
            if (checkSelfPermission("android.permission.CLEAR_APP_CACHE")
                    != PackageManager.PERMISSION_GRANTED) return false;
            PackageManager pm = getPackageManager();
            for (Method method : pm.getClass().getMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if ("freeStorageAndNotify".equals(method.getName()) && params.length == 2
                        && (params[0] == long.class || params[0] == Long.class)) {
                    method.invoke(pm, Long.MAX_VALUE, null);
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // Hidden API enforcement varies by TV firmware. Try the public allocation API next.
        }
        return false;
    }

    private boolean tryAllocateStorage() {
        try {
            StorageManager storageManager = getSystemService(StorageManager.class);
            if (storageManager == null) return false;
            UUID volume = StorageManager.UUID_DEFAULT;
            long bytes = storageManager.getAllocatableBytes(volume);
            storageManager.allocateBytes(volume, bytes);
            return true;
        } catch (IOException | SecurityException ignored) {
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void showCacheClearResult(StorageSnapshot before, StorageSnapshot after, boolean attempted) {
        long beforeFree = before.freeBytes;
        long afterFree = after.freeBytes;
        long delta = beforeFree >= 0 && afterFree >= 0 ? afterFree - beforeFree : 0L;
        boolean negligible = !attempted || beforeFree < 0 || afterFree < 0 || delta < 1024L * 1024L;
        String message;
        if (negligible) {
            message = (beforeFree >= 0 && afterFree >= 0)
                    ? getString(R.string.cache_clear_no_change,
                    Formatter.formatShortFileSize(this, beforeFree),
                    Formatter.formatShortFileSize(this, afterFree))
                    : getString(R.string.cache_clearing_unavailable);
        } else {
            message = getString(R.string.cache_clear_result,
                    Formatter.formatShortFileSize(this, delta),
                    Formatter.formatShortFileSize(this, beforeFree),
                    Formatter.formatShortFileSize(this, afterFree));
        }
        new AlertDialog.Builder(this).setTitle(R.string.clear_app_cache).setMessage(message)
                .setNeutralButton(R.string.open_storage_settings,
                        (dialog, which) -> openStorageSettings())
                .setPositiveButton(android.R.string.ok, null).show();
    }

    private void openStorageSettings() {
        String[] actions = {Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
                Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS, Settings.ACTION_SETTINGS};
        for (String action : actions) {
            try {
                startActivity(new Intent(action));
                return;
            } catch (ActivityNotFoundException | SecurityException ignored) {
                // Try the next supported settings screen on this TV.
            }
        }
        Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
    }

    private String currentHomeLabel() {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolved = getPackageManager().resolveActivity(home,
                android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
        if (resolved == null || resolved.activityInfo == null) return getString(R.string.not_available);
        if (getPackageName().equals(resolved.activityInfo.packageName)) return getString(R.string.this_launcher_home);
        CharSequence label = resolved.loadLabel(getPackageManager());
        return label == null ? resolved.activityInfo.packageName : label.toString();
    }

    private String currentDnsValue() {
        String mode = Global.getString(getContentResolver(), PRIVATE_DNS_MODE);
        if ("off".equals(mode)) return getString(R.string.dns_off);
        if (!"hostname".equals(mode)) return getString(R.string.dns_automatic);
        String host = Global.getString(getContentResolver(), PRIVATE_DNS_SPECIFIER);
        if (host == null || host.trim().isEmpty()) return getString(R.string.dns_automatic);
        for (int i = 2; i < DNS_HOSTS.length; i++) {
            if (DNS_HOSTS[i].equalsIgnoreCase(host)) {
                return getString(R.string.dns_named_value, DNS_LABELS[i], host);
            }
        }
        return host;
    }

    private void showDnsChoices() {
        String mode = Global.getString(getContentResolver(), PRIVATE_DNS_MODE);
        String host = Global.getString(getContentResolver(), PRIVATE_DNS_SPECIFIER);
        int checked = 1;
        if ("off".equals(mode)) {
            checked = 0;
        } else if ("hostname".equals(mode) && host != null && !host.trim().isEmpty()) {
            checked = DNS_HOSTS.length;
            for (int i = 2; i < DNS_HOSTS.length; i++) {
                if (DNS_HOSTS[i].equalsIgnoreCase(host)) {
                    checked = i;
                    break;
                }
            }
        }
        final int checkedIndex = checked;
        CharSequence[] options = new CharSequence[DNS_HOSTS.length + 1];
        options[0] = getString(R.string.dns_off);
        options[1] = getString(R.string.dns_automatic);
        options[2] = getString(R.string.dns_adguard);
        options[3] = getString(R.string.dns_adguard_family);
        options[4] = getString(R.string.dns_google);
        options[5] = getString(R.string.dns_cloudflare);
        options[6] = getString(R.string.dns_cloudflare_family);
        options[7] = getString(R.string.dns_quad9);
        options[8] = checked == DNS_HOSTS.length
                ? getString(R.string.dns_manual_current, host) : getString(R.string.dns_manual);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.network_dns)
                .setSingleChoiceItems(options, checked, (choiceDialog, which) -> {
                    choiceDialog.dismiss();
                    if (which == DNS_HOSTS.length) showCustomDns();
                    else if (which == 0) writeDns("off", null);
                    else if (which == 1) writeDns("opportunistic", null);
                    else writeDns("hostname", DNS_HOSTS[which]);
                }).setNegativeButton(android.R.string.cancel, null).create();
        dialog.setOnShowListener(d -> {
            dialog.getListView().setSelection(checkedIndex);
            dialog.getListView().requestFocus();
        });
        dialog.show();
    }

    private void showCustomDns() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setHint(R.string.dns_hostname_hint);
        input.setText(Global.getString(getContentResolver(), PRIVATE_DNS_SPECIFIER));
        input.setSelection(input.length());
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.dns_custom)
                .setMessage(R.string.dns_custom_hint).setView(input)
                .setPositiveButton(android.R.string.ok, (d, which) -> {
                    String host = input.getText().toString().trim();
                    if (!validHostname(host)) {
                        Toast.makeText(this, R.string.dns_invalid_hostname, Toast.LENGTH_LONG).show();
                    } else {
                        writeDns("hostname", host);
                    }
                }).setNegativeButton(android.R.string.cancel, null).create();
        dialog.setOnShowListener(d -> {
            input.requestFocus();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        });
        dialog.show();
    }

    private boolean validHostname(String host) {
        if (host.isEmpty() || host.length() > 253 || host.startsWith(".") || host.endsWith(".")) return false;
        String[] labels = host.split("\\.", -1);
        for (String label : labels) {
            if (label.isEmpty() || label.length() > 63 || !label.matches("[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?")) return false;
        }
        return true;
    }

    private void writeDns(String mode, String hostname) {
        if (checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(this).setTitle(R.string.dns_permission_title)
                    .setMessage(R.string.dns_permission_message).setPositiveButton(android.R.string.ok, null).show();
            return;
        }
        try {
            if ("hostname".equals(mode)) Global.putString(getContentResolver(), PRIVATE_DNS_SPECIFIER, hostname);
            Global.putString(getContentResolver(), PRIVATE_DNS_MODE, mode);
            refresh();
            Toast.makeText(this, R.string.dns_saved, Toast.LENGTH_SHORT).show();
        } catch (SecurityException ignored) {
            new AlertDialog.Builder(this).setTitle(R.string.dns_permission_title)
                    .setMessage(R.string.dns_permission_message).setPositiveButton(android.R.string.ok, null).show();
        }
    }

    private void showHiddenApps() {
        SharedPreferences layout = getSharedPreferences("app_layout", MODE_PRIVATE);
        Set<String> hidden = new HashSet<>(layout.getStringSet("hidden", Collections.emptySet()));
        List<String> packages = new ArrayList<>(hidden);
        Collections.sort(packages, String.CASE_INSENSITIVE_ORDER);
        if (packages.isEmpty()) {
            Toast.makeText(this, R.string.no_hidden_apps, Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence[] labels = new CharSequence[packages.size()];
        for (int i = 0; i < packages.size(); i++) {
            String pkg = packages.get(i);
            try {
                ApplicationInfo info = getPackageManager().getApplicationInfo(pkg, 0);
                labels[i] = getPackageManager().getApplicationLabel(info);
            } catch (Exception ignored) {
                labels[i] = pkg;
            }
        }
        new AlertDialog.Builder(this).setTitle(R.string.unhide_apps).setItems(labels, (dialog, which) -> {
            Set<String> updated = new HashSet<>(layout.getStringSet("hidden", Collections.emptySet()));
            updated.remove(packages.get(which));
            layout.edit().putStringSet("hidden", updated).apply();
            setResult(RESULT_OK);
            showHiddenApps();
        }).setNegativeButton(android.R.string.cancel, null).show();
    }
}
