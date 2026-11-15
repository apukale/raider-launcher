package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Backdrop, poster, meta, overview, cast, and one Watch button per installed India provider. */
public final class TitleActivity extends Activity {
    private ImageView backdrop;
    private LinearLayout buttons;
    private LinearLayout castRow;
    private TextView back;
    private View firstButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(RaiderTheme.theme(this).background);
        backdrop = new ImageView(this);
        backdrop.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(backdrop, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        View scrim = new View(this);
        GradientDrawable left = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xF2101016, 0xCC101016, 0x80101016, 0x00101016});
        GradientDrawable bottom = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{0xF208080E, 0x0008080E});
        scrim.setBackground(new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{left, bottom}));
        root.addView(scrim, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(48);
        page.setPadding(pad, dp(28), pad, dp(20));
        page.addView(header());
        page.addView(body(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        TextView castLabel = label("Cast", 16);
        castLabel.setPadding(0, dp(8), 0, dp(6));
        page.addView(castLabel);
        HorizontalScrollView castScroll = new HorizontalScrollView(this);
        castScroll.setHorizontalScrollBarEnabled(false);
        castScroll.setFocusable(false);
        castRow = new LinearLayout(this);
        castRow.setOrientation(LinearLayout.HORIZONTAL);
        castScroll.addView(castRow);
        page.addView(castScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(108)));
        TextView credit = label(TmdbModels.CREDIT, 12);
        credit.setAlpha(0.7f);
        credit.setPadding(0, dp(6), 0, 0);
        page.addView(credit);
        root.addView(page, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
        Fonts.apply(root, Fonts.HOME);

        String media = getIntent().getStringExtra("media");
        int id = getIntent().getIntExtra("id", 0);
        TmdbRepository.loadTitle(this, media, id, this::show);
    }

    @Override
    protected void onStop() {
        ContentImages.trim();
        if (backdrop != null) backdrop.setImageDrawable(null);
        super.onStop();
    }

    private LinearLayout header() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        back = button("\u2039  Back", false);
        back.setOnClickListener(v -> finish());
        row.addView(back);
        TextClock clock = new TextClock(this);
        clock.setFormat12Hour("h:mm");
        clock.setFormat24Hour("HH:mm");
        clock.setTextColor(RaiderTheme.theme(this).onSurface);
        clock.setTextSize(28);
        clock.setTypeface(Fonts.of(this, Fonts.HOME, R.font.outfit_light));
        clock.setIncludeFontPadding(false);
        LinearLayout.LayoutParams clockLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clockLp.setMarginStart(dp(28));
        row.addView(clock, clockLp);
        TextView date = label(new SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(new Date()).toUpperCase(Locale.getDefault()), 12);
        date.setLetterSpacing(0.08f);
        date.setPadding(dp(16), dp(8), 0, 0);
        row.addView(date);
        return row;
    }

    private LinearLayout body() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView poster = new ImageView(this);
        poster.setId(R.id.contentPosterImage);
        poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        poster.setBackgroundColor(0xFF1A1B2E);
        poster.setClipToOutline(true);
        poster.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(4));
            }
        });
        LinearLayout.LayoutParams posterLp = new LinearLayout.LayoutParams(dp(150), dp(225));
        posterLp.setMarginEnd(dp(28));
        row.addView(poster, posterLp);
        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.VERTICAL);
        meta.setId(R.id.contentHeroChoice);
        TextView name = label("", 36);
        name.setTypeface(Fonts.of(this, Fonts.HOME, R.font.outfit_light));
        name.setMaxLines(2);
        name.setId(android.R.id.title);
        meta.addView(name);
        TextView line = label("", 15);
        line.setId(android.R.id.text1);
        line.setPadding(0, dp(6), 0, 0);
        meta.addView(line);
        LinearLayout chips = new LinearLayout(this);
        chips.setId(android.R.id.text2);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(0, dp(8), 0, 0);
        meta.addView(chips);
        TextView overview = label("", 16);
        overview.setId(android.R.id.message);
        overview.setMaxLines(4);
        overview.setEllipsize(android.text.TextUtils.TruncateAt.END);
        overview.setPadding(0, dp(10), 0, 0);
        meta.addView(overview);
        HorizontalScrollView buttonScroll = new HorizontalScrollView(this);
        buttonScroll.setHorizontalScrollBarEnabled(false);
        buttonScroll.setFocusable(false);
        buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setPadding(0, dp(14), 0, 0);
        buttonScroll.addView(buttons);
        meta.addView(buttonScroll);
        row.addView(meta, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private void show(TmdbModels.Title title) {
        TextView name = findViewById(android.R.id.title);
        TextView line = findViewById(android.R.id.text1);
        LinearLayout chips = findViewById(android.R.id.text2);
        TextView overview = findViewById(android.R.id.message);
        ImageView poster = findViewById(R.id.contentPosterImage);
        buttons.removeAllViews();
        castRow.removeAllViews();
        chips.removeAllViews();
        firstButton = null;
        if (title == null) {
            name.setText("Title unavailable");
            line.setText("");
            overview.setText("This title could not be loaded.");
            back.requestFocus();
            return;
        }
        name.setText(title.name);
        StringBuilder meta = new StringBuilder();
        add(meta, title.year());
        add(meta, TmdbModels.runtimeLabel(title.runtime));
        String rating = TmdbModels.ratingLabel(title.vote);
        if (!rating.isEmpty()) add(meta, rating + " TMDB");
        line.setText(meta.toString());
        String[] genres = title.genres.length > 0 ? title.genres : split(TmdbModels.genreLine(title));
        int shown = 0;
        for (String genre : genres) {
            if (genre == null || genre.isEmpty() || shown == 3) continue;
            TextView chip = label(genre, 13);
            chip.setPadding(dp(10), dp(4), dp(10), dp(4));
            GradientDrawable box = new GradientDrawable();
            box.setCornerRadius(dp(4));
            box.setStroke(dp(1), 0x66FFFFFF);
            chip.setBackground(box);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            chips.addView(chip, lp);
            shown++;
        }
        overview.setText(title.overview);
        String art = !title.backdrop.isEmpty() ? title.backdrop : title.poster;
        if (!art.isEmpty()) ContentImages.load(this, backdrop, art, MemoryPolicy.HERO_PX);
        if (!title.poster.isEmpty()) ContentImages.load(this, poster, title.poster, MemoryPolicy.POSTER_LARGE_PX);
        ProviderMap providers = TmdbRepository.map(this);
        Set<String> installed = InstalledAppsIndex.enabled(this, providers.packages());
        List<ProviderMap.Hit> hits = providers.forProviders(title.providerIds, installed);
        int index = 0;
        for (ProviderMap.Hit hit : hits) {
            TextView action = button("\u25B6  Watch on " + hit.name, index == 0);
            action.setOnClickListener(v -> DeepLinkLauncher.openTitle(this, hit, title));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(10));
            buttons.addView(action, lp);
            if (firstButton == null) firstButton = action;
            index++;
        }
        if (hits.isEmpty()) {
            TextView none = label("Not on an installed app", 15);
            buttons.addView(none);
        }
        for (TmdbModels.Cast member : title.cast) castRow.addView(castItem(member));
        View focus = firstButton != null ? firstButton : back;
        focus.post(focus::requestFocus);
    }

    private View castItem(TmdbModels.Cast member) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setFocusable(true);
        item.setClickable(true);
        item.setContentDescription(member.name + (member.character.isEmpty() ? "" : ", " + member.character));
        ImageView photo = new ImageView(this);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photo.setBackgroundColor(0xFF1A1B2E);
        photo.setClipToOutline(true);
        photo.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(4));
            }
        });
        item.addView(photo, new LinearLayout.LayoutParams(dp(64), dp(64)));
        TextView name = label(member.name, 13);
        name.setMaxLines(1);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setPadding(0, dp(4), dp(8), 0);
        item.addView(name, new LinearLayout.LayoutParams(dp(92), ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView role = label(member.character, 12);
        role.setTextColor(RaiderTheme.theme(this).secondary);
        role.setMaxLines(1);
        role.setEllipsize(android.text.TextUtils.TruncateAt.END);
        item.addView(role, new LinearLayout.LayoutParams(dp(92), ViewGroup.LayoutParams.WRAP_CONTENT));
        item.setOnFocusChangeListener((v, focused) -> {
            GradientDrawable d = new GradientDrawable();
            d.setCornerRadius(dp(4));
            d.setColor(0);
            if (focused) d.setStroke(dp(2), RaiderTheme.accent(this));
            photo.setForeground(d);
        });
        if (!member.profile.isEmpty()) ContentImages.load(this, photo, member.profile, MemoryPolicy.POSTER_PX);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(14));
        item.setLayoutParams(lp);
        return item;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            View focus = getCurrentFocus();
            int key = event.getKeyCode();
            if (key == KeyEvent.KEYCODE_DPAD_UP && focus != null && focus != back && back != null) {
                if (inside(castRow, focus) && firstButton != null) {
                    firstButton.requestFocus();
                    return true;
                }
                if (inside(buttons, focus)) {
                    back.requestFocus();
                    return true;
                }
            }
            if (key == KeyEvent.KEYCODE_DPAD_DOWN && focus == back) {
                View next = firstButton != null ? firstButton : firstCast();
                if (next != null) {
                    next.requestFocus();
                    return true;
                }
            }
            if (key == KeyEvent.KEYCODE_DPAD_DOWN && focus != null && inside(buttons, focus)) {
                View next = firstCast();
                if (next != null) {
                    next.requestFocus();
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private View firstCast() {
        return castRow.getChildCount() == 0 ? null : castRow.getChildAt(0);
    }

    private TextView button(String text, boolean filled) {
        TextView view = label(text, 16);
        view.setTypeface(Fonts.of(this, Fonts.HOME, R.font.outfit_medium));
        view.setFocusable(true);
        view.setClickable(true);
        view.setPadding(dp(16), dp(8), dp(16), dp(8));
        int accent = RaiderTheme.accent(this);
        view.setOnFocusChangeListener((v, focused) -> paint((TextView) v, filled, focused, accent));
        paint(view, filled, false, accent);
        return view;
    }

    private void paint(TextView view, boolean filled, boolean focused, int accent) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(4));
        if (filled || focused) {
            int color = focused ? RaiderTheme.theme(this).focus : accent;
            d.setColor(filled || focused ? color : 0);
            if (!filled && focused) d.setColor(color);
            view.setTextColor(RaiderColors.inkOn(color));
        } else {
            d.setColor(0x00000000);
            d.setStroke(dp(1), 0x99FFFFFF);
            view.setTextColor(RaiderTheme.theme(this).onSurface);
        }
        if (filled && !focused) {
            d.setColor(accent);
            view.setTextColor(RaiderColors.inkOn(accent));
        }
        view.setBackground(d);
    }

    private TextView label(String text, int sp) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(RaiderTheme.theme(this).onSurface);
        view.setTextSize(sp);
        view.setTypeface(Fonts.of(this, Fonts.HOME, R.font.outfit_regular));
        view.setIncludeFontPadding(false);
        view.setShadowLayer(6f, 0f, 1f, 0xCC000000);
        return view;
    }

    private static void add(StringBuilder line, String part) {
        if (part == null || part.isEmpty()) return;
        if (line.length() > 0) line.append("  \u00B7  ");
        line.append(part);
    }

    private static String[] split(String line) {
        if (line == null || line.isEmpty()) return new String[0];
        return line.split(" \\u00B7 ");
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
}
