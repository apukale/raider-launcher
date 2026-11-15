package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Hero, Trending in India, and Top 10 above the dock. The band scrolls so the
 * larger posters can move into view. Up and down are handled here, one zone at
 * a time, and each row remembers the poster that last had focus.
 */
public final class ContentShelf extends ScrollView {
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
    private int accent;
    private int trendFocus;
    private int topFocus;
    private final int posterW;
    private final int posterH;

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
        setFillViewport(true);
        setFocusable(false);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setSmoothScrollingEnabled(false);
        setVerticalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        posterW = dp(MemoryPolicy.POSTER_WIDTH_DP);
        posterH = Math.round(posterW * 3f / 2f);
        accent = RaiderTheme.accent(context);

        LinearLayout body = new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setClipChildren(false);
        addView(body, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        heroBlock = column();
        heroBlock.setGravity(android.view.Gravity.BOTTOM);
        eyebrow = text(12, true);
        eyebrow.setLetterSpacing(0.14f);
        eyebrow.setAllCaps(true);
        titleView = text(32, false);
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
        watch.setPadding(dp(16), dp(8), dp(16), dp(8));
        watch.setOnFocusChangeListener((v, focused) -> {
            paintButton(focused);
            if (focused) reveal(heroBlock);
        });
        watch.setOnKeyListener((v, key, event) -> event.getAction() == KeyEvent.ACTION_DOWN
                && (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT));
        heroBlock.addView(eyebrow);
        heroBlock.addView(titleView, gap(4));
        heroBlock.addView(metaView, gap(6));
        heroBlock.addView(overviewView, gap(8));
        heroBlock.addView(watch, gap(12));
        body.addView(heroBlock, matchWrap());

        trendAdapter = new PosterAdapter(false);
        trendBlock = section("Trending in India");
        trendRow = row();
        trendRow.setAdapter(trendAdapter);
        trendBlock.addView(trendRow, rowHeight());
        body.addView(trendBlock, matchWrap());

        topAdapter = new PosterAdapter(true);
        topBlock = section("Top 10 in India");
        topRow = row();
        topRow.setAdapter(topAdapter);
        topBlock.addView(topRow, rowHeight());
        body.addView(topBlock, matchWrap());

        TextView credit = text(11, false);
        credit.setText(TmdbModels.CREDIT);
        credit.setAlpha(0.7f);
        body.addView(credit, gap(6));
        paintButton(false);
        setVisibility(GONE);
    }

    void setHost(Host host) { this.host = host; }

    void attachBackdrop(ImageView backdrop, View scrim) {
        this.backdrop = backdrop;
        this.scrim = scrim;
        if (scrim == null) return;
        GradientDrawable left = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xF0101016, 0xC0101016, 0x66101016, 0x00101016});
        GradientDrawable bottom = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{0xF208080E, 0x9908080E, 0x0008080E});
        scrim.setBackground(new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{left, bottom}));
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
        int next = HomeFocus.next(HomeFocus.TOP, false, watchVisible(), trendVisible(), topVisible());
        return focusZone(next);
    }

    boolean offerFocusFromDock() {
        if (getVisibility() != VISIBLE) return false;
        int next = HomeFocus.next(HomeFocus.DOCK, true, watchVisible(), trendVisible(), topVisible());
        return focusZone(next);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int key = event.getKeyCode();
            if ((key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN)
                    && moveVertical(key == KeyEvent.KEYCODE_DPAD_UP)) {
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private boolean moveVertical(boolean up) {
        int zone = zoneOf(findFocus());
        if (zone < 0 || host == null) return false;
        int next = HomeFocus.next(zone, up, watchVisible(), trendVisible(), topVisible());
        if (next < 0) {
            if (up) host.focusUp();
            else host.focusDown();
            return true;
        }
        return focusZone(next);
    }

    private boolean focusZone(int zone) {
        if (zone == HomeFocus.HERO) {
            if (watch.getVisibility() != VISIBLE || !watch.requestFocus()) return false;
            reveal(heroBlock);
            return true;
        }
        if (zone == HomeFocus.TRENDING) return focusIndex(trendRow, trendFocus, trendBlock);
        if (zone == HomeFocus.TOP10) return focusIndex(topRow, topFocus, topBlock);
        return false;
    }

    private boolean focusIndex(PosterRow row, int index, View block) {
        if (row.getAdapter() == null || row.getAdapter().getItemCount() == 0 || row.getWidth() <= 0) return false;
        int safe = Math.max(0, Math.min(index, row.getAdapter().getItemCount() - 1));
        RecyclerView.LayoutManager manager = row.getLayoutManager();
        View child = manager == null ? null : manager.findViewByPosition(safe);
        if (child != null && child.requestFocus()) {
            reveal(block);
            return true;
        }
        if (manager != null) manager.scrollToPosition(safe);
        final int target = safe;
        row.post(() -> {
            RecyclerView.LayoutManager ready = row.getLayoutManager();
            View view = ready == null ? null : ready.findViewByPosition(target);
            if (view != null && view.requestFocus()) reveal(block);
        });
        return row.getHeight() > 0;
    }

    private int zoneOf(View focus) {
        if (inside(topRow, focus)) return HomeFocus.TOP10;
        if (inside(trendRow, focus)) return HomeFocus.TRENDING;
        if (inside(heroBlock, focus)) return HomeFocus.HERO;
        return -1;
    }

    private void reveal(View block) {
        if (block == null) return;
        block.post(() -> {
            Rect rect = new Rect(0, -dp(12), Math.max(1, block.getWidth()), block.getHeight() + dp(12));
            block.requestRectangleOnScreen(rect, false);
        });
    }

    private void remember(boolean ranked, int position) {
        if (position < 0) return;
        if (ranked) topFocus = position;
        else trendFocus = position;
    }

    private boolean watchVisible() { return watch.getVisibility() == VISIBLE; }
    private boolean trendVisible() { return trendBlock.getVisibility() == VISIBLE; }
    private boolean topVisible() { return topBlock.getVisibility() == VISIBLE; }

    private void bind() {
        prefs = getContext().getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        boolean heroOn = ContentPrefs.on(prefs, ContentPrefs.HERO) && home != null && home.hero != null;
        boolean trendOn = ContentPrefs.on(prefs, ContentPrefs.TRENDING) && home != null && !home.trending.isEmpty();
        boolean topOn = ContentPrefs.on(prefs, ContentPrefs.TOP10) && home != null && !home.top10.isEmpty();
        heroBlock.setVisibility(heroOn ? VISIBLE : GONE);
        trendBlock.setVisibility(trendOn ? VISIBLE : GONE);
        topBlock.setVisibility(topOn ? VISIBLE : GONE);
        if (trendOn) trendAdapter.submit(home.trending);
        else trendFocus = 0;
        if (topOn) topAdapter.submit(home.top10);
        else topFocus = 0;
        if (heroOn) bindHero(home.hero);
        else hideBackdrop();
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
        boolean show = parked && sections && ContentPrefs.showsHome(prefs);
        setVisibility(show ? VISIBLE : GONE);
        if (!show || heroBlock.getVisibility() != VISIBLE) {
            if (backdrop != null && backdrop.getVisibility() == VISIBLE) hideBackdrop();
        }
    }

    private String meta(TmdbModels.Title hero) {
        StringBuilder line = new StringBuilder();
        append(line, TmdbModels.cardCaption(hero));
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

    private void paintButton(boolean focused) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(dp(4));
        int color = focused ? RaiderTheme.theme(getContext()).focus : accent;
        drawable.setColor(color);
        watch.setBackground(drawable);
        watch.setTextColor(RaiderColors.inkOn(color));
    }

    private LinearLayout column() {
        LinearLayout box = new LinearLayout(getContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setClipChildren(false);
        return box;
    }

    private LinearLayout section(String label) {
        LinearLayout box = column();
        TextView name = text(16, false);
        name.setTypeface(Fonts.of(getContext(), Fonts.HOME, R.font.outfit_medium));
        name.setText(label);
        box.addView(name, gap(8));
        return box;
    }

    private PosterRow row() {
        PosterRow row = new PosterRow(getContext());
        row.setPadding(0, dp(6), dp(24), 0);
        row.setNestedScrollingEnabled(false);
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

    private LinearLayout.LayoutParams gap(int value) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(value);
        return lp;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams rowHeight() {
        return new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, posterH + dp(28));
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
            android.view.ViewParent owner = view.getParent();
            view = owner instanceof View ? (View) owner : null;
        }
        return false;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class PosterAdapter extends RecyclerView.Adapter<PosterAdapter.Holder> {
        private final boolean ranked;
        private List<TmdbModels.Title> items = Collections.emptyList();

        PosterAdapter(boolean ranked) {
            this.ranked = ranked;
            setHasStableIds(true);
        }

        void submit(List<TmdbModels.Title> next) {
            items = next == null ? Collections.emptyList() : next;
            notifyDataSetChanged();
        }

        @Override public int getItemCount() { return items.size(); }

        @Override
        public long getItemId(int position) {
            TmdbModels.Title title = items.get(position);
            return ((title.movie() ? 1L : 2L) << 32) | (title.id & 0xffffffffL);
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(itemView(parent.getContext()));
        }

        private View itemView(Context context) {
            LinearLayout posterColumn = new LinearLayout(context);
            posterColumn.setOrientation(LinearLayout.VERTICAL);
            FrameLayout poster = posterCard(context);
            poster.setId(R.id.contentPoster);
            posterColumn.addView(poster, new LinearLayout.LayoutParams(posterW, posterH));
            TextView line = text(12, false);
            line.setId(R.id.contentStatus);
            line.setMaxLines(1);
            line.setEllipsize(android.text.TextUtils.TruncateAt.END);
            line.setTextColor(RaiderTheme.theme(context).secondary);
            LinearLayout.LayoutParams captionLp = new LinearLayout.LayoutParams(posterW, dp(20));
            captionLp.topMargin = dp(4);
            posterColumn.addView(line, captionLp);
            if (!ranked) return posterColumn;
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.TOP);
            TextView number = new TextView(context);
            number.setId(R.id.contentRank);
            number.setTypeface(Fonts.of(context, Fonts.HOME, R.font.outfit_light));
            number.setIncludeFontPadding(false);
            number.setGravity(android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL);
            number.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, posterH * 0.42f);
            number.setShadowLayer(8f, 0f, 2f, 0xCC000000);
            row.addView(number, new LinearLayout.LayoutParams(dp(72), posterH));
            LinearLayout.LayoutParams columnLp = new LinearLayout.LayoutParams(posterW, ViewGroup.LayoutParams.WRAP_CONTENT);
            columnLp.setMarginStart(dp(4));
            row.addView(posterColumn, columnLp);
            return row;
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.bind(items.get(position), position);
        }

        @Override
        public void onViewRecycled(@NonNull Holder holder) {
            holder.image.setImageDrawable(null);
        }

        final class Holder extends RecyclerView.ViewHolder {
            final ImageView image;
            final FrameLayout card;
            final TextView rank;
            final TextView caption;

            Holder(View item) {
                super(item);
                card = itemView.findViewById(R.id.contentPoster);
                image = itemView.findViewById(R.id.contentPosterImage);
                rank = ranked ? itemView.findViewById(R.id.contentRank) : null;
                caption = itemView.findViewById(R.id.contentStatus);
                itemView.setFocusable(true);
                itemView.setClickable(true);
                itemView.setOnFocusChangeListener((view, focused) -> {
                    stroke(card, focused);
                    if (!focused) return;
                    int position = getBindingAdapterPosition();
                    remember(ranked, position);
                    reveal(ranked ? topBlock : trendBlock);
                });
                itemView.setOnKeyListener((view, key, event) -> {
                    if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                    if (key == KeyEvent.KEYCODE_DPAD_UP) return moveVertical(true);
                    if (key == KeyEvent.KEYCODE_DPAD_DOWN) return moveVertical(false);
                    return false;
                });
            }

            void bind(TmdbModels.Title title, int position) {
                if (rank != null && position >= 0) {
                    rank.setText(String.valueOf(position + 1));
                    rank.setTextColor(accent);
                }
                caption.setText(TmdbModels.cardCaption(title));
                itemView.setContentDescription(title.name + ", " + TmdbModels.cardCaption(title));
                itemView.setOnClickListener(v -> {
                    if (host != null && prefs != null && ContentPrefs.on(prefs, ContentPrefs.TITLE)) {
                        host.openTitle(title.media, title.id);
                    }
                });
                ContentImages.load(getContext(), image, title.poster, posterW);
                stroke(card, itemView.hasFocus());
            }
        }

        private FrameLayout posterCard(Context context) {
            FrameLayout card = new FrameLayout(context);
            ImageView picture = new ImageView(context);
            picture.setId(R.id.contentPosterImage);
            picture.setScaleType(ImageView.ScaleType.CENTER_CROP);
            picture.setBackgroundColor(0xFF1A1B2E);
            picture.setClipToOutline(true);
            picture.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(4));
                }
            });
            card.addView(picture, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return card;
        }

        private void stroke(FrameLayout card, boolean focused) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setCornerRadius(dp(4));
            drawable.setColor(0x00000000);
            if (focused) drawable.setStroke(dp(2), accent);
            card.setForeground(drawable);
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
        }

        @Override
        public View focusSearch(View focused, int direction) {
            if (direction == FOCUS_LEFT || direction == FOCUS_RIGHT) {
                View next = super.focusSearch(focused, direction);
                if (next == null || !inside(this, next)) return focused;
                return next;
            }
            return null;
        }
    }
}
