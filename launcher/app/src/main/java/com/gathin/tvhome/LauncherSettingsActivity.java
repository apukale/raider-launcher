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
import android.os.Build;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.provider.Settings.Global;
import android.provider.Settings;
import android.text.InputType;
import android.text.format.Formatter;
import android.view.WindowManager;
import android.view.View;
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
    private TextView screensaverStyleChoice, photoSourceChoice, photoStorageChoice, photoIntervalChoice, calendarChoice, homeCalendarChoice, refreshPhotosChoice, startAfterChoice;
    private final Runnable photoStatusPoll = new Runnable() { public void run() { updatePhotoSummary(); mainHandler.postDelayed(this, 2500); } };

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
        TextView moduleCards = findViewById(R.id.moduleCardsChoice);
        if (!config.moduleCards) moduleCards.setVisibility(View.GONE);
        moduleCards.setOnClickListener(v -> { setResult(RESULT_OK, new Intent().putExtra("open_module_cards", true)); finish(); });
        screensaverStyleChoice=findViewById(R.id.screensaverStyleChoice);
        screensaverStyleChoice.setOnClickListener(v -> chooseScreensaverStyle());
        photoSourceChoice=findViewById(R.id.photoSourceChoice);
        photoSourceChoice.setOnClickListener(v -> choosePhotoSource());
        photoStorageChoice=findViewById(R.id.photoStorageChoice);
        photoStorageChoice.setOnClickListener(v -> choosePhotoStorage());
        photoIntervalChoice=findViewById(R.id.photoIntervalChoice);
        photoIntervalChoice.setOnClickListener(v -> choosePhotoInterval());
        calendarChoice=findViewById(R.id.calendarChoice);
        calendarChoice.setOnClickListener(v -> { boolean on=choices.getBoolean("dream_calendar",true);choices.edit().putBoolean("dream_calendar",!on).apply();refresh(); });
        homeCalendarChoice=findViewById(R.id.homeCalendarChoice);
        homeCalendarChoice.setOnClickListener(v -> { boolean on=choices.getBoolean("home_hindu_calendar",true);choices.edit().putBoolean("home_hindu_calendar",!on).apply();refresh(); });
        refreshPhotosChoice=findViewById(R.id.refreshPhotosChoice);
        refreshPhotosChoice.setOnClickListener(v -> { PhotoStore.schedule(this,true); Toast.makeText(this,R.string.photo_refresh_scheduled,Toast.LENGTH_SHORT).show();updatePhotoSummary();mainHandler.postDelayed(photoStatusPoll,2500); });
        startAfterChoice=findViewById(R.id.startAfterChoice);
        if (Build.VERSION.SDK_INT < 23 || !Settings.System.canWrite(this)) startAfterChoice.setVisibility(View.GONE);
        else startAfterChoice.setOnClickListener(v -> chooseStartAfter());
        findViewById(R.id.startScreensaverChoice).setOnClickListener(v -> startActivity(new Intent(this,ScreensaverActivity.class)));
        refresh();
        columnChoices[Math.max(0, Math.min(columns - 4, 3))].requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStorageSummary();
        updatePhotoSummary();
        updateScreensaverRows();
    }

    @Override protected void onPause(){mainHandler.removeCallbacks(photoStatusPoll);super.onPause();}

    private void refreshScreensaverLabels(){updateScreensaverRows();}
    private void updateScreensaverRows(){if(screensaverStyleChoice==null)return;boolean photos=choices.getBoolean("dream_photos",false);screensaverStyleChoice.setText(getString(R.string.screensaver_style,getString(photos?R.string.screensaver_photos:R.string.screensaver_clock_only)));String src=PhotoStore.chosenSource(this);int srcLabel=PhotoStore.SOURCE_BING.equals(src)?R.string.photo_source_bing:PhotoStore.SOURCE_PICSUM.equals(src)?R.string.photo_source_picsum:R.string.photo_source_both;photoSourceChoice.setText(getString(R.string.photo_source,getString(srcLabel)));photoStorageChoice.setText(getString(R.string.photo_storage,(int)(PhotoStore.cap(this)/(1024L*1024L))));long ms=choices.getLong("photo_interval_ms",60_000L);int mins=ms<=30_000?30:ms<=60_000?1:ms<=120_000?2:5;photoIntervalChoice.setText(getString(R.string.photo_interval,getString(R.string.screensaver_minutes,mins)));calendarChoice.setText(getString(R.string.hindu_calendar,getString(choices.getBoolean("dream_calendar",true)?R.string.on:R.string.off)));homeCalendarChoice.setText(getString(R.string.home_hindu_calendar,getString(choices.getBoolean("home_hindu_calendar",true)?R.string.on:R.string.off)));if(startAfterChoice.getVisibility()==View.VISIBLE){int timeout=Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_OFF_TIMEOUT,600000);int current=Math.max(1,Math.round(timeout/60000f));startAfterChoice.setText(getString(R.string.start_after,getString(R.string.screensaver_minutes,current)));}}
    private void updatePhotoSummary(){if(refreshPhotosChoice!=null)refreshPhotosChoice.setText(getString(R.string.refresh_photos,PhotoStore.summary(this)));}
    private void chooseScreensaverStyle(){String[] labels={getString(R.string.screensaver_clock_only),getString(R.string.screensaver_photos)};new AlertDialog.Builder(this).setTitle(R.string.screensaver_settings).setSingleChoiceItems(labels,choices.getBoolean("dream_photos",false)?1:0,(d,w)->{choices.edit().putBoolean("dream_photos",w==1).apply();d.dismiss();refreshScreensaverLabels();}).setNegativeButton(android.R.string.cancel,null).show();}
    private void choosePhotoSource(){String[] values={PhotoStore.SOURCE_BING,PhotoStore.SOURCE_PICSUM,PhotoStore.SOURCE_BOTH};int[] labels={R.string.photo_source_bing,R.string.photo_source_picsum,R.string.photo_source_both};String current=PhotoStore.chosenSource(this);int selected=PhotoStore.SOURCE_BING.equals(current)?0:PhotoStore.SOURCE_PICSUM.equals(current)?1:2;String[] shown={getString(labels[0]),getString(labels[1]),getString(labels[2])};new AlertDialog.Builder(this).setTitle(R.string.photo_source_title).setSingleChoiceItems(shown,selected,(d,w)->{choices.edit().putString("photo_source",values[w]).apply();d.dismiss();updateScreensaverRows();updatePhotoSummary();PhotoStore.schedule(this,false);}).setNegativeButton(android.R.string.cancel,null).show();}
    private void choosePhotoStorage(){int[] values={100,200,500};String[] labels={"100 MB","200 MB","500 MB"};int current=(int)(PhotoStore.cap(this)/(1024L*1024L)),selected=current==100?0:current==500?2:1;new AlertDialog.Builder(this).setTitle(R.string.photo_storage_title).setSingleChoiceItems(labels,selected,(d,w)->{choices.edit().putInt("photo_storage_mb",values[w]).apply();d.dismiss();PhotoStore.enforceCap(this);updateScreensaverRows();updatePhotoSummary();}).setNegativeButton(android.R.string.cancel,null).show();}
    private void choosePhotoInterval(){int[] values={30_000,60_000,120_000,300_000};String[] labels={"30 s","1 min","2 min","5 min"};long current=choices.getLong("photo_interval_ms",60_000L);int selected=1;for(int i=0;i<values.length;i++)if(values[i]==current)selected=i;new AlertDialog.Builder(this).setTitle(R.string.photo_interval).setSingleChoiceItems(labels,selected,(d,w)->{choices.edit().putLong("photo_interval_ms",values[w]).apply();d.dismiss();refreshScreensaverLabels();}).setNegativeButton(android.R.string.cancel,null).show();}
    private void chooseStartAfter(){int[] values={5,10,15,30};String[] labels={"5 min","10 min","15 min","30 min"};int selected=0;int now=Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_OFF_TIMEOUT,600000);for(int i=0;i<values.length;i++)if(Math.abs(now-values[i]*60000)<60000)selected=i;new AlertDialog.Builder(this).setTitle(R.string.start_after_title).setSingleChoiceItems(labels,selected,(d,w)->{if(Settings.System.canWrite(this))Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_OFF_TIMEOUT,values[w]*60000);d.dismiss();updateScreensaverRows();}).setNegativeButton(android.R.string.cancel,null).show();}

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
        updateScreensaverRows();
        updatePhotoSummary();
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
