package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Hero, Trending in India, and Top 10 in India in the band above the dock.
 * Titles that are not on an installed app are not added. Continue Watching is not part of this.
 */
public final class ContentShelf extends LinearLayout {
    interface Host {
        void onBackdrop(boolean showing);
        void onBound();
        void openTitle(String media, int id);
        void watch(ProviderMap.Hit hit, TmdbModels.Title title);
        void focusUp();
        void focusDown();
    }

    private Host host;
    private ImageView backdrop;
    private View scrim;
    private SharedPreferences prefs;
    private TmdbModels.Home home;
    private boolean parked = true;
    private boolean tooSmall;
    private int posterPx;
    private int accent;

    private final LinearLayout heroBlock;
    private final TextView eyebrow;
    private final TextView titleView;
    private final TextView metaView;
    private final TextView overviewView;
    private final TextView watch;
    private final LinearLayout trendBlock;
    private final PosterRow trendRow;
    private final LinearLayout topBlock;
    private final PosterRow topRow;
    private final PosterAdapter trendAdapter;
    private final PosterAdapter topAdapter;

    public ContentShelf(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setClipChildren(false);
        setClipToPadding(false);
        setFocusable(false);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        posterPx = dp(120);
        accent = RaiderTheme.accent(context);
        heroBlock = column();
        heroBlock.setGravity(android.view.Gravity.BOTTOM);
        eyebrow = text(12, true);
        eyebrow.setLetterSpacing(0.14f);
        eyebrow.setAllCaps(true);
        titleView = text(34, false);
        titleView.setTypeface(Fonts.of(context, Fonts.HOME, R.font.outfit_light));
        titleView.setMaxLines(2);
        metaView = text(14, false);
        overviewView = text(15, false);
        overviewView.setMaxLines(3);
        overviewView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        watch = text(16, false);
        watch.setTypeface(Fonts.of(context, Fonts.HOME, R.font.outfit_medium));
        watch.setFocusable(true);
        watch.setClickable(true);
        int padH = dp(16);
        int padV = dp(8);
        watch.setPadding(padH, padV, padH, padV);
        watch.setOnFocusChangeListener((v, focused) -> paintButton(focused));
        watch.setOnKeyListener((v, key, event) -> event.getAction() == KeyEvent.ACTION_DOWN
                && (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT));
        heroBlock.addView(eyebrow);
        heroBlock.addView(titleView, gapTop(4));
        heroBlock.addView(metaView, gapTop(6));
        heroBlock.addView(overviewView, gapTop(8));
        heroBlock.addView(watch, gapTop(12));
        addView(heroBlock, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        trendAdapter = new PosterAdapter(false);
        trendBlock = section("Trending in India");
        trendRow = row();
        trendRow.setAdapter(trendAdapter);
        trendBlock.addView(trendRow, rowParams(posterPx));
        addView(trendBlock, wrap());

        topAdapter = new PosterAdapter(true);
        topBlock = section("Top 10 in India");
        topRow = row();
        topRow.setAdapter(topAdapter);
        topBlock.addView(topRow, rowParams(posterPx));
        addView(topBlock, wrap());

        TextView credit = text(11, false);
        credit.setText(TmdbModels.CREDIT);
        credit.setAlpha(0.7f);
        LayoutParams creditLp = gapTop(4);
        creditLp.bottomMargin = dp(2);
        addView(credit, creditLp);
        paintButton(false);
        setVisibility(GONE);
    }

    void setHost(Host host) { this.host = host; }

    void attachBackdrop(ImageView backdrop, View scrim) {
        this.backdrop = backdrop;
        this.scrim = scrim;
        if (scrim != null) {
            GradientDrawable left = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0xF0101016, 0xC0101016, 0x66101016, 0x00101016});
            GradientDrawable bottom = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                    new int[]{0xF208080E, 0x9908080E, 0x0008080E});
            scrim.setBackground(new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{left, bottom}));
        }
    }

    void reload() {
        Context context = getContext();
        prefs = context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        accent = RaiderTheme.accent(context);
        paintButton(watch.hasFocus());
        if (!ContentPrefs.showsHome(prefs)) {
            home = null;
            applyVisibility();
            return;
        }
        TmdbRepository.loadHome(context, loaded -> {
            home = loaded;
            bind();
            if (host != null) host.onBound();
        });
    }

    void onHostStop() {
        ContentImages.trim();
        clearBitmaps(this);
        if (backdrop != null) {
            backdrop.setImageDrawable(null);
            backdrop.setVisibility(GONE);
        }
        if (scrim != null) scrim.setVisibility(GONE);
    }

    /** False while the remote is in the app rows under the dock, so those tiles are not covered. */
    void setParked(boolean onDock) {
        parked = onDock;
        applyVisibility();
        if (getVisibility() == VISIBLE && home != null && home.hero != null && heroBlock.getVisibility() == VISIBLE
                && backdrop != null && backdrop.getVisibility() != VISIBLE) {
            showArt(home.hero);
        }
    }

    boolean coversHero() {
        return getVisibility() == VISIBLE && heroBlock.getVisibility() == VISIBLE;
    }

    boolean offerFocusFromTop() {
        if (getVisibility() != VISIBLE) return false;
        if (watch.getVisibility() == VISIBLE) {
            watch.requestFocus();
            return true;
        }
        if (trendBlock.getVisibility() == VISIBLE) return focusRow(trendRow, 0);
        if (topBlock.getVisibility() == VISIBLE) return focusRow(topRow, 0);
        return false;
    }

    boolean offerFocusFromDock() {
        if (getVisibility() != VISIBLE) return false;
        if (topBlock.getVisibility() == VISIBLE) return focusRow(topRow, 0);
        if (trendBlock.getVisibility() == VISIBLE) return focusRow(trendRow, 0);
        if (watch.getVisibility() == VISIBLE) {
            watch.requestFocus();
            return true;
        }
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && move(event.getKeyCode())) return true;
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        tooSmall = h > 0 && h < dp(MemoryPolicy.MIN_SHELF_DP);
        int rows = (trendBlock.getVisibility() == VISIBLE ? 1 : 0) + (topBlock.getVisibility() == VISIBLE ? 1 : 0);
        if (rows == 0) rows = (ContentPrefs.on(prefs, ContentPrefs.TRENDING) ? 1 : 0) + (ContentPrefs.on(prefs, ContentPrefs.TOP10) ? 1 : 0);
        int reserve = dp(28) + rows * dp(26);
        if (ContentPrefs.on(prefs, ContentPrefs.HERO)) reserve += dp(112);
        int leftover = Math.max(0, h - reserve);
        int poster = rows == 0 ? posterPx : Math.max(dp(72), Math.min(dp(176), leftover / Math.max(1, rows)));
        if (Math.abs(poster - posterPx) > 3 && poster > 0) {
            posterPx = poster;
            setRowHeight(trendRow, posterPx);
            setRowHeight(topRow, posterPx);
            post(() -> {
                trendAdapter.notifyDataSetChanged();
                topAdapter.notifyDataSetChanged();
            });
        }
        applyVisibility();
    }

    private boolean move(int key) {
        if (key != KeyEvent.KEYCODE_DPAD_UP && key != KeyEvent.KEYCODE_DPAD_DOWN) return false;
        View focus = findFocus();
        if (focus == null || host == null) return false;
        boolean up = key == KeyEvent.KEYCODE_DPAD_UP;
        boolean inHero = inside(heroBlock, focus);
        boolean inTrend = inside(trendRow, focus);
        boolean inTop = inside(topRow, focus);
        if (up) {
            if (inTop && trendBlock.getVisibility() == VISIBLE) return focusRow(trendRow, 0);
            if ((inTop || inTrend) && watch.getVisibility() == VISIBLE) {
                watch.requestFocus();
                return true;
            }
            if (inHero || inTrend || inTop) {
                host.focusUp();
                return true;
            }
            return false;
        }
        if (inHero && trendBlock.getVisibility() == VISIBLE) return focusRow(trendRow, 0);
        if (inHero && topBlock.getVisibility() == VISIBLE) return focusRow(topRow, 0);
        if (inTrend && topBlock.getVisibility() == VISIBLE) return focusRow(topRow, 0);
        if (inHero || inTrend || inTop) {
            host.focusDown();
            return true;
        }
        return false;
    }

    private void bind() {
        prefs = getContext().getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        boolean heroOn = ContentPrefs.on(prefs, ContentPrefs.HERO) && home != null && home.hero != null;
        boolean trendOn = ContentPrefs.on(prefs, ContentPrefs.TRENDING) && home != null && !home.trending.isEmpty();
        boolean topOn = ContentPrefs.on(prefs, ContentPrefs.TOP10) && home != null && !home.top10.isEmpty();
        heroBlock.setVisibility(heroOn ? VISIBLE : GONE);
        trendBlock.setVisibility(trendOn ? VISIBLE : GONE);
        topBlock.setVisibility(topOn ? VISIBLE : GONE);
        if (trendOn) trendAdapter.submit(home.trending);
        if (topOn) topAdapter.submit(home.top10);
        if (heroOn) bindHero(home.hero);
        else hideBackdrop();
        LayoutParams heroLp = (LayoutParams) heroBlock.getLayoutParams();
        heroLp.height = heroOn ? 0 : 0;
        heroLp.weight = heroOn ? 1f : 0f;
        heroBlock.setLayoutParams(heroLp);
        applyVisibility();
    }

    private void bindHero(TmdbModels.Title hero) {
        eyebrow.setText("Trending in India");
        titleView.setText(hero.name);
        metaView.setText(meta(hero));
        overviewView.setText(hero.overview);
        overviewView.setVisibility(hero.overview.isEmpty() ? GONE : VISIBLE);
        ProviderMap providers = TmdbRepository.map(getContext());
        Set<String> installed = InstalledAppsIndex.enabled(getContext(), providers.packages());
        List<ProviderMap.Hit> hits = providers.forProviders(hero.providerIds, installed);
        if (!hits.isEmpty()) {
            ProviderMap.Hit hit = hits.get(0);
            watch.setVisibility(VISIBLE);
            watch.setText("\u25B6  Watch on " + hit.name);
            watch.setOnClickListener(v -> { if (host != null) host.watch(hit, hero); });
        } else if (ContentPrefs.on(prefs, ContentPrefs.TITLE)) {
            watch.setVisibility(VISIBLE);
            watch.setText("Details");
            watch.setOnClickListener(v -> { if (host != null) host.openTitle(hero.media, hero.id); });
        } else {
            watch.setVisibility(GONE);
        }
        showArt(hero);
    }

    private void showArt(TmdbModels.Title hero) {
        String art = !hero.backdrop.isEmpty() ? hero.backdrop : hero.poster;
        if (art.isEmpty() || backdrop == null) {
            hideBackdrop();
            return;
        }
        backdrop.setVisibility(VISIBLE);
        if (scrim != null) scrim.setVisibility(VISIBLE);
        ContentImages.load(getContext(), backdrop, art, MemoryPolicy.HERO_PX);
        if (host != null) host.onBackdrop(true);
    }

    private void hideBackdrop() {
        if (backdrop != null) {
            backdrop.setImageDrawable(null);
            backdrop.setVisibility(GONE);
        }
        if (scrim != null) scrim.setVisibility(GONE);
        if (host != null) host.onBackdrop(false);
    }

    private void applyVisibility() {
        boolean sections = heroBlock.getVisibility() == VISIBLE || trendBlock.getVisibility() == VISIBLE || topBlock.getVisibility() == VISIBLE;
        boolean show = parked && !tooSmall && sections && ContentPrefs.showsHome(prefs);
        setVisibility(show ? VISIBLE : GONE);
        if (!show || heroBlock.getVisibility() != VISIBLE) {
            if (backdrop != null && backdrop.getVisibility() == VISIBLE) hideBackdrop();
        }
    }

    private String meta(TmdbModels.Title hero) {
        StringBuilder line = new StringBuilder();
        append(line, hero.year());
        append(line, TmdbModels.runtimeLabel(hero.runtime));
        append(line, TmdbModels.genreLine(hero));
        String rating = TmdbModels.ratingLabel(hero.vote);
        if (!rating.isEmpty()) append(line, rating + " TMDB");
        return line.toString();
    }

    private static void append(StringBuilder line, String part) {
        if (part == null || part.isEmpty()) return;
        if (line.length() > 0) line.append("  \u00B7  ");
        line.append(part);
    }

    private boolean focusRow(PosterRow row, int index) {
        if (row.getAdapter() == null || row.getAdapter().getItemCount() == 0) return false;
        int safe = Math.max(0, Math.min(index, row.getAdapter().getItemCount() - 1));
        row.scrollToPosition(safe);
        row.post(() -> {
            RecyclerView.ViewHolder holder = row.findViewHolderForAdapterPosition(safe);
            if (holder != null) holder.itemView.requestFocus();
        });
        return true;
    }

    private void paintButton(boolean focused) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(4));
        d.setColor(focused ? RaiderTheme.theme(getContext()).focus : accent);
        watch.setBackground(d);
        watch.setTextColor(RaiderColors.inkOn(focused ? RaiderTheme.theme(getContext()).focus : accent));
    }

    private LinearLayout column() {
        LinearLayout box = new LinearLayout(getContext());
        box.setOrientation(VERTICAL);
        box.setClipChildren(false);
        return box;
    }

    private LinearLayout section(String label) {
        LinearLayout box = column();
        TextView name = text(16, false);
        name.setTypeface(Fonts.of(getContext(), Fonts.HOME, R.font.outfit_medium));
        name.setText(label);
        box.addView(name, gapTop(8));
        return box;
    }

    private PosterRow row() {
        PosterRow row = new PosterRow(getContext());
        row.setPadding(0, dp(6), dp(24), 0);
        return row;
    }

    private TextView text(int sp, boolean caps) {
        TextView view = new TextView(getContext());
        view.setTextColor(RaiderTheme.theme(getContext()).onSurface);
        view.setTextSize(sp);
        view.setTypeface(Fonts.of(getContext(), Fonts.HOME, caps ? R.font.outfit_medium : R.font.outfit_regular));
        view.setIncludeFontPadding(false);
        view.setShadowLayer(6f, 0f, 1f, 0xCC000000);
        return view;
    }

    private LayoutParams gapTop(int dp) {
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(dp);
        return lp;
    }

    private LayoutParams wrap() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    private LayoutParams rowParams(int height) {
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, height);
        return lp;
    }

    private void setRowHeight(PosterRow row, int height) {
        ViewGroup.LayoutParams lp = row.getLayoutParams();
        if (lp != null && lp.height != height) {
            lp.height = height;
            row.setLayoutParams(lp);
        }
    }

    private void clearBitmaps(View view) {
        if (view instanceof ImageView) ((ImageView) view).setImageDrawable(null);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) clearBitmaps(group.getChildAt(i));
        }
    }

    private static boolean inside(View parent, View view) {
        while (view != null) {
            if (view == parent) return true;
            android.view.ViewParent p = view.getParent();
            view = p instanceof View ? (View) p : null;
        }
        return false;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class PosterAdapter extends RecyclerView.Adapter<PosterAdapter.Holder> {
        private final boolean ranked;
        private List<TmdbModels.Title> items = Collections.emptyList();

        PosterAdapter(boolean ranked) { this.ranked = ranked; }

        void submit(List<TmdbModels.Title> next) {
            items = next == null ? Collections.emptyList() : next;
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() { return items.size(); }

        @Override
        public long getItemId(int position) {
            TmdbModels.Title title = items.get(position);
            return ((title.movie() ? 1L : 2L) << 32) | (title.id & 0xffffffffL);
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(parent.getContext());
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.bind(items.get(position), position);
        }

        @Override
        public void onViewRecycled(@NonNull Holder holder) {
            holder.image.setImageDrawable(null);
            holder.card.setTag(null);
        }

        final class Holder extends RecyclerView.ViewHolder {
            final ImageView image;
            final FrameLayout card;
            final TextView rank;

            Holder(Context context) {
                super(ranked ? rankedItem(context) : plainItem(context));
                card = itemView.findViewById(R.id.contentPoster);
                image = itemView.findViewById(R.id.contentPosterImage);
                rank = ranked ? itemView.findViewById(R.id.contentRank) : null;
                itemView.setFocusable(true);
                itemView.setClickable(true);
                itemView.setOnFocusChangeListener((v, focused) -> stroke(card, focused));
            }

            void bind(TmdbModels.Title title, int position) {
                int width = Math.round(posterPx * 2f / 3f);
                if (ranked) {
                    itemView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, posterPx));
                    ViewGroup.LayoutParams lp = card.getLayoutParams();
                    lp.width = width;
                    lp.height = posterPx;
                    card.setLayoutParams(lp);
                    rank.setText(String.valueOf(position + 1));
                    rank.setTextColor(accent);
                    rank.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, posterPx * 0.5f);
                    rank.getLayoutParams().width = Math.round(posterPx * 0.46f);
                    rank.getLayoutParams().height = posterPx;
                } else {
                    itemView.setLayoutParams(new RecyclerView.LayoutParams(width, posterPx));
                }
                itemView.setContentDescription(title.name);
                itemView.setOnClickListener(v -> {
                    if (host != null && ContentPrefs.on(prefs, ContentPrefs.TITLE)) host.openTitle(title.media, title.id);
                });
                ContentImages.load(getContext(), image, title.poster, MemoryPolicy.POSTER_PX);
                stroke(card, itemView.hasFocus());
            }
        }

        private FrameLayout plainItem(Context context) {
            FrameLayout card = posterCard(context);
            card.setId(R.id.contentPoster);
            return card;
        }

        private LinearLayout rankedItem(Context context) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(HORIZONTAL);
            row.setGravity(android.view.Gravity.BOTTOM);
            TextView number = new TextView(context);
            number.setId(R.id.contentRank);
            number.setTypeface(Fonts.of(context, Fonts.HOME, R.font.outfit_light));
            number.setIncludeFontPadding(false);
            number.setGravity(android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL);
            number.setShadowLayer(8f, 0f, 2f, 0xCC000000);
            row.addView(number, new LinearLayout.LayoutParams(dp(36), LayoutParams.MATCH_PARENT));
            FrameLayout card = posterCard(context);
            card.setId(R.id.contentPoster);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(80), posterPx);
            lp.setMarginStart(dp(2));
            row.addView(card, lp);
            return row;
        }

        private FrameLayout posterCard(Context context) {
            FrameLayout card = new FrameLayout(context);
            ImageView image = new ImageView(context);
            image.setId(R.id.contentPosterImage);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackgroundColor(0xFF1A1B2E);
            image.setClipToOutline(true);
            image.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(4));
                }
            });
            card.addView(image, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            return card;
        }

        private void stroke(FrameLayout card, boolean focused) {
            GradientDrawable d = new GradientDrawable();
            d.setCornerRadius(dp(4));
            d.setColor(0x00000000);
            if (focused) d.setStroke(dp(2), accent);
            card.setForeground(d);
        }
    }

    private static final class PosterRow extends RecyclerView {
        PosterRow(Context context) {
            super(context);
            setLayoutManager(new LinearLayoutManager(context, HORIZONTAL, false));
            setFocusable(false);
            setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
            setClipToPadding(false);
            setClipChildren(false);
            setItemAnimator(null);
            setOverScrollMode(OVER_SCROLL_NEVER);
            setHasFixedSize(false);
        }

        @Override
        public View focusSearch(View focused, int direction) {
            if (direction == FOCUS_LEFT || direction == FOCUS_RIGHT) {
                View next = super.focusSearch(focused, direction);
                if (next == null || !inside(this, next)) return focused;
                return next;
            }
            return super.focusSearch(focused, direction);
        }
    }
}
