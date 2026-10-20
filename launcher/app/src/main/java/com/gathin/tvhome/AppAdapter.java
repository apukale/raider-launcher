package com.gathin.tvhome;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.ColorDrawable;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * App tiles. The first {@link MainActivity#COLUMNS} positions are the pinned dock (favourites, fixed order),
 * then the module cards the owner chose to show (by label), the rest is alphabetical.
 */
public class AppAdapter extends RecyclerView.Adapter<AppAdapter.Holder> {

    interface TileListener {
        void onTileFocused(int position, View card);

        /** Called for a D-pad direction key on a tile. Return true when handled (focus stays put). */
        boolean onTileVerticalKey(int position, int keyCode);
        void onTileLongPressed(int position, View card);
    }

    static final class Entry {
        static final int APP = 0, HOME_SETTINGS = 1, CARD = 2;

        final int kind;
        final String label;
        final ComponentName component;
        final String pkg;
        final Drawable banner;
        final Drawable icon;
        /** Text under the tile when it is focused in the grid: the label, plus the caption for a card. */
        final String name;
        /** Ambient glow colour; 0 until sampled from the banner, or the card's accent from the start. */
        int ambient;
        final boolean logo;

        Entry(String label, ComponentName component, Drawable banner, Drawable icon, boolean logo) {
            this(APP, label, component, banner, icon, null, 0, logo);
        }

        Entry(int kind, String label, ComponentName component, Drawable banner, Drawable icon, String caption, int accent) {
            this(kind, label, component, banner, icon, caption, accent, false);
        }

        Entry(int kind, String label, ComponentName component, Drawable banner, Drawable icon, String caption, int accent, boolean logo) {
            this.kind = kind;
            this.label = label;
            this.component = component;
            this.pkg = component.getPackageName();
            this.banner = banner;
            this.icon = icon;
            this.name = caption == null ? label : label + " \u00B7 " + caption;
            this.ambient = accent;
            this.logo = logo;
        }
    }

    static final float FOCUS_SCALE = 1.10f;
    /** Design size of a tile (dp); tiles shrink from here when the columns don't fit, see tileWidthPx. */
    static final float DESIGN_W_DP = 180f, DESIGN_H_DP = 101f;

    /**
     * Tile width for a grid of this inner width: the cell minus room for the focus zoom and the glass
     * highlight, capped at the design size so 4 columns on a 1080p TV look exactly as before.
     */
    static int tileWidthPx(int innerWidthPx, int columns, float density) {
        int design = Math.round(DESIGN_W_DP * density);
        if (innerWidthPx <= 0 || columns <= 0) return design;
        int cell = innerWidthPx / columns;
        int fit = (int) ((cell - 12 * density) / FOCUS_SCALE);
        return Math.max(Math.round(80 * density), Math.min(design, fit));
    }

    static int tileHeightPx(int tileWidthPx) {
        return Math.round(tileWidthPx * DESIGN_H_DP / DESIGN_W_DP);
    }

    /** Height of one grid item: the tile, the name label under it and the item's own padding. */
    static int itemHeightPx(int tileHeightPx, float density) {
        return tileHeightPx + Math.round(45 * density);
    }

    private final Context ctx;
    private final TileListener listener;
    private final int dockSize;
    /** Dock order (config.xml dock_apps, or a theme's config.json). */
    private final List<String> favourites;
    /** Never shown as tiles (res/values/config.xml, hidden_apps). */
    private final Set<String> configuredHidden;
    private final Set<String> configuredLogo;
    /** Appearance overrides keyed by target package; entries never change component identity. */
    private final Map<String, TileOverride> tileOverrides = new HashMap<>();
    private final Set<String> hidden = new HashSet<>();
    private final boolean animations;
    private final boolean advancedEffects;
    private final boolean showHomeSettings;
    /** Calmer motion for weak GPUs (config.xml, reduced_motion): no overshoot, shorter fades. */
    private final boolean reducedMotion;
    private final List<Entry> items = new ArrayList<>();
    private String movingKey;

    AppAdapter(Context ctx, TileListener listener, int dockSize, Config cfg) {
        this.ctx = ctx;
        this.listener = listener;
        this.dockSize = dockSize;
        this.favourites = cfg.dockApps;
        this.configuredHidden = new HashSet<>(cfg.hiddenApps);
        this.configuredLogo = new HashSet<>(cfg.logoTileApps);
        for (String value : cfg.tileOverrides) {
            int equals = value.indexOf('=');
            if (equals <= 0) continue;
            String target = value.substring(0, equals).trim();
            String spec = value.substring(equals + 1).trim();
            int pipe = spec.indexOf('|');
            String source = (pipe < 0 ? spec : spec.substring(0, pipe)).trim();
            String label = pipe < 0 ? null : spec.substring(pipe + 1).trim();
            if (!target.isEmpty() && !source.isEmpty()) {
                tileOverrides.put(target, new TileOverride(source, label == null || label.isEmpty() ? null : label));
            }
        }
        this.animations = cfg.animations;
        this.advancedEffects = cfg.animations && cfg.advancedEffects;
        this.showHomeSettings = cfg.showHomeSettingsShortcut;
        this.reducedMotion = cfg.reducedMotion;
        // Tiles keep their identity across reload(), so a focused tile stays on its app instead of
        // its view being rebound to whatever app now sits in that slot.
        setHasStableIds(true);
    }

    /**
     * Rebuilds the tiles. A shown module card stands in for every tile of its package (same component,
     * so focus follows it); hidden_apps wins over a card, and a card's package in dock_apps takes that slot.
     */
    void reload(List<Cards.Card> cards) {
        hidden.clear();
        hidden.addAll(configuredHidden);
        hidden.addAll(ctx.getSharedPreferences("app_layout", Context.MODE_PRIVATE)
                .getStringSet("hidden", Collections.emptySet()));
        PackageManager pm = ctx.getPackageManager();
        Intent probe = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> found = pm.queryIntentActivities(probe, 0);
        Map<String, Cards.Card> cardByPkg = new HashMap<>();
        for (Cards.Card c : cards) cardByPkg.put(c.pkg, c);
        List<Entry> fresh = new ArrayList<>();
        for (ResolveInfo ri : found) {
            if (ri.activityInfo == null) continue;
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(ctx.getPackageName()) || hidden.contains(pkg)) continue;
            ComponentName component = new ComponentName(pkg, ri.activityInfo.name);
            Cards.Card card = cardByPkg.get(pkg);
            if (card != null && !card.component.equals(component)) continue;   // the card stands in for this tile
            String label = Cards.cleanLabel(String.valueOf(ri.loadLabel(pm)), pkg);
            boolean logo = useLogo(pkg);
            Drawable banner = logo ? null : loadBanner(pm, ri);
            boolean artworkAsLogo = !logo && !explicitBanner(pkg) && isNonBannerArtwork(banner);
            if (artworkAsLogo) logo = true;
            Drawable icon = artworkAsLogo ? banner : ri.loadIcon(pm);
            TileOverride override = tileOverrides.get(pkg);
            if (override != null) {
                try {
                    ResolvedAppearance appearance = loadOverride(pm, override, label, banner, icon, !logo);
                    label = appearance.label;
                    banner = appearance.banner;
                    icon = appearance.icon;
                } catch (Exception ignored) {
                    // Broken/missing sources silently retain the target's own label and artwork.
                }
            }
            if (card != null) {
                fresh.add(new Entry(Entry.CARD, override == null ? card.label : label,
                        component, logo ? renderLogo(icon) : compact(banner, tileWidth(), tileHeightPx(tileWidth())),
                        logo ? null : compact(icon, iconSize(), iconSize()), card.caption, card.accent,
                        logo));
            } else {
                // Same cleaner as the card prompt: an app label with a bidi override or line separator must not
                // redraw the tile name as something else (#117).
                fresh.add(new Entry(label, component, logo ? renderLogo(icon) : compact(banner, tileWidth(), tileHeightPx(tileWidth())),
                        logo ? null : compact(icon, iconSize(), iconSize()), logo));
            }
        }
        Collections.sort(fresh, (a, b) -> {
            int fa = favourites.indexOf(a.pkg), fb = favourites.indexOf(b.pkg);
            if (fa >= 0 || fb >= 0) {
                if (fa < 0) return 1;
                if (fb < 0) return -1;
                return Integer.compare(fa, fb);
            }
            // Cards come right after the configured dock apps, before the alphabetical grid.
            boolean ca = a.kind == Entry.CARD, cb = b.kind == Entry.CARD;
            if (ca != cb) return ca ? -1 : 1;
            return a.label.compareToIgnoreCase(b.label);
        });
        android.content.SharedPreferences prefs = ctx.getSharedPreferences("app_layout", Context.MODE_PRIVATE);
        if (prefs.getBoolean("custom_order", false)) {
            List<String> order = new ArrayList<>();
            String encoded = prefs.getString("ordered_components", "");
            if (!encoded.isEmpty()) Collections.addAll(order, encoded.split("\\n"));
            Map<String, Entry> byKey = new HashMap<>();
            for (Entry e : fresh) byKey.put(e.component.flattenToShortString(), e);
            List<Entry> ordered = new ArrayList<>();
            for (String key : order) { Entry e = byKey.remove(key); if (e != null) ordered.add(e); }
            // Apps not present when the custom order was saved go at the end, alphabetically.
            List<Entry> added = new ArrayList<>();
            for (Entry e : fresh) if (byKey.containsKey(e.component.flattenToShortString())) added.add(e);
            Collections.sort(added, (a, b) -> a.label.compareToIgnoreCase(b.label));
            ordered.addAll(added);
            fresh.clear(); fresh.addAll(ordered);
        }
        // Retain the optional Default-home tile until dependent tile-ordering work is merged.
        if (showHomeSettings
                && pm.resolveActivity(new Intent(Settings.ACTION_HOME_SETTINGS), 0) != null) {
            fresh.add(new Entry(Entry.HOME_SETTINGS, ctx.getString(R.string.home_settings),
                    new ComponentName(ctx, HomeSettingsActivity.class), null,
                    compact(ctx.getDrawable(R.drawable.ic_home_settings), iconSize(), iconSize()), null, 0));
        }
        items.clear();
        items.addAll(fresh);
        notifyDataSetChanged();
    }

    private int tileWidth() {
        DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        int width = dm.widthPixels;
        if (ctx instanceof Activity) {
            View grid = ((Activity) ctx).findViewById(R.id.grid);
            if (grid != null && grid.getWidth() > 0) width = grid.getWidth() - grid.getPaddingLeft() - grid.getPaddingRight();
        }
        return tileWidthPx(width, dockSize, dm.density);
    }

    private int iconSize() { return Math.round(48 * ctx.getResources().getDisplayMetrics().density); }

    private boolean useLogo(String pkg) {
        android.content.SharedPreferences p = ctx.getSharedPreferences("app_layout", Context.MODE_PRIVATE);
        if (p.getStringSet("banner_tiles", Collections.emptySet()).contains(pkg)) return false;
        return p.getStringSet("logo_tiles", Collections.emptySet()).contains(pkg) || configuredLogo.contains(pkg);
    }

    private boolean explicitBanner(String pkg) {
        return ctx.getSharedPreferences("app_layout", Context.MODE_PRIVATE)
                .getStringSet("banner_tiles", Collections.emptySet()).contains(pkg);
    }

    private static boolean isNonBannerArtwork(Drawable banner) {
        if (banner instanceof AdaptiveIconDrawable) return true;
        if (banner == null) return false;
        int width = banner.getIntrinsicWidth(), height = banner.getIntrinsicHeight();
        return width > 0 && height > 0 && (float) width / height < 1.5f;
    }

    private Drawable compact(Drawable source, int width, int height) {
        if (source == null || width <= 0 || height <= 0) return source;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Rect old = new Rect(source.getBounds());
        source.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
        source.draw(canvas);
        source.setBounds(old);
        bitmap.setDensity(ctx.getResources().getDisplayMetrics().densityDpi);
        return new BitmapDrawable(ctx.getResources(), bitmap);
    }

    /** Builds logo-mode artwork once as an opaque tile bitmap; no app drawable is retained by the entry. */
    private Drawable renderLogo(Drawable icon) {
        int width = tileWidth();
        int height = tileHeightPx(width);
        if (width <= 0 || height <= 0) return null;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        if (icon instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable adaptive = (AdaptiveIconDrawable) icon;
            Drawable background = adaptive.getBackground();
            if (background instanceof ColorDrawable) {
                canvas.drawColor(((ColorDrawable) background).getColor());
            } else if (background != null) {
                drawInBounds(background, canvas, 0, 0, width, height);
            } else {
                canvas.drawColor(Color.BLACK);
            }
            Drawable foreground = adaptive.getForeground();
            if (foreground != null) {
                float inset = AdaptiveIconDrawable.getExtraInsetFraction();
                float safeFraction = Math.max(0.01f, 1f - 2f * inset);
                int intrinsicWidth = foreground.getIntrinsicWidth();
                int intrinsicHeight = foreground.getIntrinsicHeight();
                if (intrinsicWidth <= 0) intrinsicWidth = Math.max(width, height);
                if (intrinsicHeight <= 0) intrinsicHeight = intrinsicWidth;
                float scale = Math.min(height * 0.85f / (intrinsicHeight * safeFraction),
                        width * 0.90f / (intrinsicWidth * safeFraction));
                float drawWidth = intrinsicWidth * scale;
                float drawHeight = intrinsicHeight * scale;
                // Enlarge past the tile edge so the transparent adaptive-icon padding is cropped away.
                drawInBounds(foreground, canvas, (width - drawWidth) / 2f, (height - drawHeight) / 2f,
                        (width + drawWidth) / 2f, (height + drawHeight) / 2f);
            }
        } else {
            canvas.drawColor(icon == null ? Color.rgb(32, 32, 32) : logoColor(icon));
            if (icon != null) {
                int iw = icon.getIntrinsicWidth(), ih = icon.getIntrinsicHeight();
                if (iw <= 0) iw = iconSize();
                if (ih <= 0) ih = iconSize();
                float scale = Math.min(height * 0.70f / ih, width * 0.70f / iw);
                float dw = iw * scale, dh = ih * scale;
                drawInBounds(icon, canvas, (width - dw) / 2f, (height - dh) / 2f,
                        (width + dw) / 2f, (height + dh) / 2f);
            }
        }
        bitmap.setDensity(ctx.getResources().getDisplayMetrics().densityDpi);
        return new BitmapDrawable(ctx.getResources(), bitmap);
    }

    private static void drawInBounds(Drawable drawable, Canvas canvas, float left, float top, float right, float bottom) {
        Rect old = new Rect(drawable.getBounds());
        drawable.setBounds(Math.round(left), Math.round(top), Math.round(right), Math.round(bottom));
        drawable.draw(canvas);
        drawable.setBounds(old);
    }

    private Drawable loadBanner(PackageManager pm, ResolveInfo ri) {
        int id = ri.activityInfo.banner;
        if (id == 0) id = ri.activityInfo.applicationInfo.banner;
        if (id != 0) {
            try {
                Resources resources = pm.getResourcesForApplication(ri.activityInfo.applicationInfo);
                // Keep at least xhdpi detail rather than downsampling artwork before the focus zoom.
                int density = Math.max(DisplayMetrics.DENSITY_XHIGH, ctx.getResources().getDisplayMetrics().densityDpi);
                return resources.getDrawableForDensity(id, density, null);
            } catch (PackageManager.NameNotFoundException | Resources.NotFoundException ignored) {
            }
        }
        return ri.activityInfo.loadBanner(pm);
    }

    private static final class TileOverride {
        final String sourcePackage;
        final String label;
        TileOverride(String sourcePackage, String label) {
            this.sourcePackage = sourcePackage;
            this.label = label;
        }
    }

    private static final class ResolvedAppearance {
        final String label;
        final Drawable banner;
        final Drawable icon;
        ResolvedAppearance(String label, Drawable banner, Drawable icon) {
            this.label = label;
            this.banner = banner;
            this.icon = icon;
        }
    }

    /** Load source resources directly so Android does not add suspended/disabled badging. */
    private ResolvedAppearance loadOverride(PackageManager pm, TileOverride override, String fallbackLabel,
                                             Drawable fallbackBanner, Drawable fallbackIcon, boolean includeBanner) throws Exception {
        int flags = PackageManager.MATCH_DISABLED_COMPONENTS | PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS;
        ApplicationInfo app = pm.getApplicationInfo(override.sourcePackage, flags);
        Intent probe = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                .setPackage(override.sourcePackage);
        ResolveInfo resolved = null;
        for (ResolveInfo candidate : pm.queryIntentActivities(probe, flags)) {
            if (candidate.activityInfo != null) { resolved = candidate; break; }
        }
        Resources resources = pm.getResourcesForApplication(app);
        int bannerId = 0;
        int iconId = app.icon;
        CharSequence sourceLabel = pm.getApplicationLabel(app);
        if (resolved != null) {
            ActivityInfo activity = resolved.activityInfo;
            if (includeBanner) {
                if (activity.banner != 0) bannerId = activity.banner;
                else bannerId = app.banner;
            }
            if (activity.icon != 0) iconId = activity.icon;
            sourceLabel = activity.loadLabel(pm);
        } else {
            if (includeBanner) bannerId = app.banner;
        }
        Drawable banner = rawDrawable(resources, bannerId);
        Drawable icon = rawDrawable(resources, iconId);
        if (includeBanner && banner == null && resolved != null) {
            try { banner = resolved.activityInfo.loadBanner(pm); } catch (Exception ignored) { }
        }
        if (includeBanner && banner == null) {
            try { banner = app.loadBanner(pm); } catch (Exception ignored) { }
        }
        if (icon == null && resolved != null) {
            try { icon = resolved.activityInfo.loadIcon(pm); } catch (Exception ignored) { }
        }
        if (icon == null) {
            try { icon = app.loadIcon(pm); } catch (Exception ignored) { }
        }
        if (banner == null) banner = fallbackBanner;
        if (icon == null) icon = fallbackIcon;
        String label = override.label != null ? override.label
                : (sourceLabel == null ? fallbackLabel : Cards.cleanLabel(sourceLabel.toString(), override.sourcePackage));
        return new ResolvedAppearance(label, banner, icon);
    }

    private Drawable rawDrawable(Resources resources, int id) {
        if (id == 0) return null;
        try {
            int density = Math.max(DisplayMetrics.DENSITY_XHIGH, ctx.getResources().getDisplayMetrics().densityDpi);
            return resources.getDrawableForDensity(id, density, null);
        } catch (Resources.NotFoundException ignored) {
            return null;
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        float density = parent.getResources().getDisplayMetrics().density;
        int width = parent.getWidth() > 0 ? parent.getWidth() : parent.getMeasuredWidth();
        int inner = width - parent.getPaddingLeft() - parent.getPaddingRight();
        int w = tileWidthPx(inner, dockSize, density), h = tileHeightPx(w);
        View card = v.findViewById(R.id.card);
        ViewGroup.LayoutParams cardLp = card.getLayoutParams();
        cardLp.width = w;
        cardLp.height = h;
        card.setLayoutParams(cardLp);
        View name = v.findViewById(R.id.name);
        ViewGroup.MarginLayoutParams nameLp = (ViewGroup.MarginLayoutParams) name.getLayoutParams();
        nameLp.width = w;
        nameLp.topMargin = h + Math.round(7 * density);
        name.setLayoutParams(nameLp);
        return new Holder(v, listener, animations, advancedEffects, reducedMotion);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Entry e = items.get(position);
        h.name.setText(e.name);
        h.name.setVisibility(position < dockSize ? View.INVISIBLE : View.VISIBLE);   // the dock has no labels
        boolean focused = h.card.isFocused();   // a rebind after reload() can hit the focused tile
        h.name.setAlpha(focused ? 1f : 0f);
        h.card.setScaleX(focused && animations ? focusScale() : 1f);
        h.card.setScaleY(focused && animations ? focusScale() : 1f);
        // Same end state as the focus listener: centre pivot, no lift, no shadow (the glass ring is centred on the layout bounds).
        centrePivot(h.card);
        h.card.setTranslationY(0f);
        h.card.setTranslationZ(0f);
        h.card.setElevation(0f);
        h.banner.setScaleX(focused && advancedEffects ? 1.035f : 1f);
        h.banner.setScaleY(focused && advancedEffects ? 1.035f : 1f);
        if (e.banner != null) {
            h.banner.setImageDrawable(e.banner);
            h.banner.setVisibility(View.VISIBLE);
            h.icon.setVisibility(View.GONE);
            h.label.setVisibility(View.GONE);
            h.card.setBackgroundResource(R.drawable.tile_bg);
        } else {
            h.banner.setImageDrawable(null);
            h.banner.setVisibility(View.GONE);
            h.icon.setImageDrawable(e.icon);
            h.icon.setVisibility(View.VISIBLE);
            h.icon.setTranslationY(e.logo ? 7f * ctx.getResources().getDisplayMetrics().density : 0f);
            h.label.setText(e.label);
            h.label.setVisibility(View.VISIBLE);
            h.card.setBackgroundResource(R.drawable.tile_bg);
        }
        h.card.setOnClickListener(v -> launch(e));
        boolean moving = e.component.flattenToShortString().equals(movingKey);
        h.card.setAlpha(moving ? 0.78f : 1f);
        h.card.setScaleX(focused && animations ? focusScale() : moving ? 1.08f : 1f);
        h.card.setScaleY(focused && animations ? focusScale() : moving ? 1.08f : 1f);
        h.card.setTranslationZ(moving ? 18f * ctx.getResources().getDisplayMetrics().density : 0f);
        h.card.setOnLongClickListener(v -> { listener.onTileLongPressed(h.getBindingAdapterPosition(), v); return true; });
    }

    private static int logoColor(Drawable icon) {
        int color = vividColor(icon);
        return Color.rgb((int)(Color.red(color) * .58f), (int)(Color.green(color) * .58f), (int)(Color.blue(color) * .58f));
    }

    boolean logoAt(int position) {
        if (position < 0 || position >= items.size()) return false;
        Entry e = items.get(position);
        return e.logo || e.banner == null;
    }

    void setLogoChoice(String pkg, boolean logo) {
        android.content.SharedPreferences p = ctx.getSharedPreferences("app_layout", Context.MODE_PRIVATE);
        Set<String> logos = new HashSet<>(p.getStringSet("logo_tiles", Collections.emptySet()));
        Set<String> banners = new HashSet<>(p.getStringSet("banner_tiles", Collections.emptySet()));
        if (logo) {
            banners.remove(pkg); logos.add(pkg);
        } else {
            logos.remove(pkg); banners.add(pkg);
        }
        p.edit().putStringSet("logo_tiles", logos).putStringSet("banner_tiles", banners).apply();
    }

    /** Opens the tile's activity by explicit component, with no extras: a card can name nothing else. */
    private void launch(Entry e) {
        if (e.kind == Entry.HOME_SETTINGS) {
            ctx.startActivity(new Intent(ctx, HomeSettingsActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }
        Intent i = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                .setComponent(e.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            ctx.startActivity(i);
            if (advancedEffects && ctx instanceof Activity) {
                ((Activity) ctx).overridePendingTransition(R.anim.app_open_enter, R.anim.app_open_exit);
            }
        } catch (Exception ex) {
            Toast.makeText(ctx, ctx.getString(R.string.cant_open, e.label), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).component.flattenToShortString().hashCode();
    }

    @Override
    public void onViewRecycled(@NonNull Holder h) {
        h.banner.setImageDrawable(null);
        h.icon.setImageDrawable(null);
        super.onViewRecycled(h);
    }

    /** Stable key for the app at a position (its component), or null. */
    String keyAt(int position) {
        return position >= 0 && position < items.size() ? items.get(position).component.flattenToShortString() : null;
    }

    String packageAt(int position) {
        return position >= 0 && position < items.size() ? items.get(position).pkg : null;
    }

    /** Position of the app with this key, or -1 when it is gone. */
    int positionOf(String key) {
        if (key == null) return -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).component.flattenToShortString().equals(key)) return i;
        }
        return -1;
    }

    String labelAt(int position) {
        return position >= 0 && position < items.size() ? items.get(position).label : "";
    }

    void move(int from, int to) {
        if (from < 0 || to < 0 || from >= items.size() || to >= items.size()) return;
        Entry e = items.remove(from); items.add(to, e);
        android.content.SharedPreferences prefs = ctx.getSharedPreferences("app_layout", Context.MODE_PRIVATE);
        List<String> visible = new ArrayList<>();
        for (Entry item : items) if (item.kind != Entry.HOME_SETTINGS) visible.add(item.component.flattenToShortString());
        Set<String> visibleSet = new HashSet<>(visible);
        String encoded = prefs.getString("ordered_components", "");
        List<String> previous = new ArrayList<>();
        if (!encoded.isEmpty()) Collections.addAll(previous, encoded.split("\\n"));
        // Keep hidden or temporarily unavailable apps in their saved slots while the visible apps move.
        List<String> order = new ArrayList<>();
        int nextVisible = 0;
        for (String key : previous) {
            if (visibleSet.contains(key)) {
                if (nextVisible < visible.size()) order.add(visible.get(nextVisible++));
            } else {
                order.add(key);
            }
        }
        while (nextVisible < visible.size()) order.add(visible.get(nextVisible++));
        prefs.edit().putBoolean("custom_order", true)
                .putString("ordered_components", android.text.TextUtils.join("\n", order)).apply();
        notifyItemMoved(from, to);
    }

    void setMovingKey(String key) {
        String previous = movingKey;
        movingKey = key;
        int old = positionOf(previous);
        int current = positionOf(key);
        if (old >= 0) notifyItemChanged(old);
        if (current >= 0 && current != old) notifyItemChanged(current);
    }

    void clearArtwork() {
        for (Entry e : items) {
            if (e.banner instanceof BitmapDrawable) e.banner.setCallback(null);
            if (e.icon instanceof BitmapDrawable) e.icon.setCallback(null);
        }
        items.clear();
        notifyDataSetChanged();
    }

    /** Pivot at the tile centre; skipped until the tile has a size so an explicit 0,0 pivot never sticks. */
    static void centrePivot(View tile) {
        if (tile.getWidth() <= 0 || tile.getHeight() <= 0) return;
        tile.setPivotX(tile.getWidth() / 2f);
        tile.setPivotY(tile.getHeight() / 2f);
    }

    float focusScale() {
        return !animations ? 1f : advancedEffects ? 1.12f : 1.10f;
    }

    /** A vivid colour sampled from the app's banner (or icon), cached per entry; a card's accent when it set one. */
    int ambientColorAt(int position) {
        if (position < 0 || position >= items.size()) return 0xFF444444;
        Entry e = items.get(position);
        if (e.ambient == 0) e.ambient = vividColor(e.banner != null ? e.banner : e.icon);
        return e.ambient;
    }

    private static int vividColor(Drawable live) {
        if (live == null) return 0xFF444444;
        // Draw a private copy: changing the bounds of the drawable an ImageView shows would blank the tile.
        Drawable d = live.getConstantState() != null ? live.getConstantState().newDrawable().mutate() : live;
        Rect saved = new Rect(d.getBounds());
        int n = 12;
        Bitmap bmp = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        d.setBounds(0, 0, n, n);
        d.draw(c);
        d.setBounds(saved);
        float[] hsv = new float[3];
        int best = 0xFF444444;
        float bestScore = -1f;
        long r = 0, g = 0, b = 0;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                int p = bmp.getPixel(x, y);
                r += Color.red(p);
                g += Color.green(p);
                b += Color.blue(p);
                Color.colorToHSV(p, hsv);
                float score = hsv[1] * Math.min(1f, hsv[2] * 1.4f);   // saturated and not too dark
                if (score > bestScore) {
                    bestScore = score;
                    best = p;
                }
            }
        }
        bmp.recycle();
        if (bestScore < 0.25f) {   // nearly monochrome banner (e.g. white): use its average, lifted
            int avg = Color.rgb((int) (r / (n * n)), (int) (g / (n * n)), (int) (b / (n * n)));
            Color.colorToHSV(avg, hsv);
            hsv[2] = Math.max(hsv[2], 0.55f);
            return Color.HSVToColor(hsv);
        }
        Color.colorToHSV(best, hsv);
        hsv[1] = Math.min(1f, hsv[1] * 1.1f);
        hsv[2] = Math.max(0.5f, hsv[2]);
        return Color.HSVToColor(hsv);
    }

    /** Corner radius for a tile of this width: 16dp at the design width, proportionally less when smaller. */
    static float cornerRadius(int tileWidthPx, float density) {
        return 16f * density * Math.min(1f, tileWidthPx / (DESIGN_W_DP * density));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final View card;
        final ImageView banner;
        final ImageView icon;
        final TextView label;
        final TextView name;
        final boolean animations;
        final boolean advancedEffects;
        final boolean reducedMotion;

        Holder(View v, TileListener listener, boolean animations, boolean advancedEffects,
               boolean reducedMotion) {
            super(v);
            card = v.findViewById(R.id.card);
            // Keep the scale pivot at the centre whenever the tile is (re)laid out; a pivot set before layout would be 0,0.
            card.addOnLayoutChangeListener((pv, p1, p2, p3, p4, p5, p6, p7, p8) -> centrePivot(pv));
            banner = v.findViewById(R.id.banner);
            icon = v.findViewById(R.id.icon);
            label = v.findViewById(R.id.label);
            name = v.findViewById(R.id.name);
            this.animations = animations;
            this.advancedEffects = advancedEffects;
            this.reducedMotion = reducedMotion;

            final float density = v.getResources().getDisplayMetrics().density;
            card.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    // 16dp at the design width, scaled down with the tile so small tiles keep the same shape.
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadius(view.getWidth(), density));
                }
            });
            card.setClipToOutline(true);

            final boolean[] consumeCenterUp = {false};
            card.setOnKeyListener((view, keyCode, event) -> {
                boolean center = keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER;
                if (center && event.getAction() == KeyEvent.ACTION_UP) {
                    if (!consumeCenterUp[0]) return false;
                    consumeCenterUp[0] = false;
                    return true;
                }
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (keyCode != KeyEvent.KEYCODE_DPAD_UP && keyCode != KeyEvent.KEYCODE_DPAD_DOWN
                        && keyCode != KeyEvent.KEYCODE_DPAD_LEFT && keyCode != KeyEvent.KEYCODE_DPAD_RIGHT
                        && !center) return false;
                int pos = getBindingAdapterPosition();
                boolean handled = listener != null && pos != RecyclerView.NO_POSITION
                        && listener.onTileVerticalKey(pos, keyCode);
                if (center && handled) consumeCenterUp[0] = true;
                return handled;
            });

            card.setOnFocusChangeListener((view, focused) -> {
                // Scale about the tile centre so the glass ring (centred on the tile) wraps it exactly.
                centrePivot(view);
                view.animate().cancel();
                banner.animate().cancel();
                name.animate().cancel();
                float scale = !animations ? 1f : advancedEffects ? 1.12f : 1.10f;
                float s = focused ? scale : 1f;
                if (!animations) {
                    view.setScaleX(1f);
                    view.setScaleY(1f);
                    view.setTranslationY(0f);
                    view.setTranslationZ(0f);
                    banner.setScaleX(1f);
                    banner.setScaleY(1f);
                    name.setAlpha(focused ? 1f : 0f);
                    name.setTranslationY(0f);
                } else if (reducedMotion) {
                    view.animate().scaleX(s).scaleY(s)
                            .translationY(0f).translationZ(0f)
                            .setDuration(focused ? 120 : 80)
                            .setInterpolator(new DecelerateInterpolator())
                            .withLayer().start();
                    name.animate().alpha(focused ? 1f : 0f).setDuration(100).start();
                } else {
                    view.animate().scaleX(s).scaleY(s)
                            .translationY(0f).translationZ(0f)
                            .setDuration(advancedEffects ? (focused ? 220 : 170) : (focused ? 260 : 180))
                            .setInterpolator(new OvershootInterpolator(focused ? (advancedEffects ? 0.7f : 1.6f) : 0f))
                            .withLayer().start();
                    if (advancedEffects) {
                        banner.animate().scaleX(focused ? 1.035f : 1f).scaleY(focused ? 1.035f : 1f)
                                .setDuration(280).start();
                    }
                    name.animate().alpha(focused ? 1f : 0f)
                            .translationY(focused ? density * 4f : -density * 6f)
                            .setDuration(220).start();
                }
                int pos = getBindingAdapterPosition();
                if (focused && listener != null && pos != RecyclerView.NO_POSITION) {
                    listener.onTileFocused(pos, view);
                }
            });
        }
    }
}
