package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Full-screen Movies & Series browser. Home itself is unchanged. */
public final class BrowseActivity extends Activity {
    private static final String[] TYPES = {"All", "Movies", "Series"};
    private static final String[] TYPE_KEYS = {"all", "movie", "tv"};
    private static final String[] SORTS = {"Newest", "Trending", "Popularity", "TMDB rating"};
    private static final String[] SORT_KEYS = {"newest", "trending", "popularity", "rating"};

    private final ArrayList<LinearLayout> filterRows = new ArrayList<>();
    private String type = "all";
    private String sort = "newest";
    private int windowDays = ContentWindow.DEFAULT_DAYS;
    private boolean[] languages = ContentLanguages.checked(ContentLanguages.DEFAULT);
    private int filterRow;
    private RecyclerView grid;
    private GridAdapter adapter;
    private TextView empty;
    private int columns = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.content.SharedPreferences prefs = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        windowDays = ContentWindow.normalize(prefs.getInt(ContentWindow.PREF, ContentWindow.DEFAULT_DAYS));
        languages = ContentLanguages.checked(prefs.getString(ContentLanguages.PREF, ContentLanguages.DEFAULT));
        float density = getResources().getDisplayMetrics().density;
        int width = getResources().getDisplayMetrics().widthPixels;
        columns = BrowseLayout.columns(width, density);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(RaiderTheme.theme(this).background);
        root.setPadding(dp(BrowseLayout.SIDE_DP), dp(BrowseLayout.FILTER_PAD_DP), dp(BrowseLayout.SIDE_DP), dp(8));
        root.setClipChildren(true);
        addFilterRow(root, TYPES, 0);
        addFilterRow(root, SORTS, 1);
        addFilterRow(root, ContentWindow.LABELS, 2);
        addFilterRow(root, ContentLanguages.LABELS, 3);
        FrameLayout stage = new FrameLayout(this);
        grid = new RecyclerView(this);
        grid.setLayoutManager(new GridLayoutManager(this, columns));
        grid.setClipToPadding(false);
        grid.setClipChildren(true);
        grid.setItemAnimator(null);
        adapter = new GridAdapter();
        grid.setAdapter(adapter);
        stage.addView(grid, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        empty = new TextView(this);
        empty.setText("Nothing in this window on your apps.");
        empty.setTextColor(RaiderTheme.theme(this).secondary);
        empty.setTextSize(16);
        empty.setTypeface(Fonts.of(this, Fonts.HOME, R.font.outfit_regular));
        empty.setVisibility(View.GONE);
        FrameLayout.LayoutParams emptyLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        emptyLp.gravity = android.view.Gravity.CENTER;
        stage.addView(empty, emptyLp);
        root.addView(stage, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        Fonts.apply(root, Fonts.HOME);
        paintFilters();
        if (filterRows.get(0).getChildCount() > 0) filterRows.get(0).getChildAt(0).requestFocus();
        reload();
    }

    @Override
    protected void onStop() {
        ContentImages.trim();
        super.onStop();
    }

    private void addFilterRow(LinearLayout root, String[] labels, int row) {
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setClipChildren(false);
        for (int i = 0; i < labels.length; i++) line.addView(chip(labels[i], row, i));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(BrowseLayout.CHIP_DP));
        if (row > 0) lp.topMargin = dp(BrowseLayout.CHIP_GAP_DP);
        root.addView(line, lp);
        filterRows.add(line);
    }

    private TextView chip(String label, int row, int index) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextSize(14);
        view.setTypeface(Fonts.of(this, Fonts.HOME, R.font.outfit_medium));
        view.setGravity(android.view.Gravity.CENTER);
        view.setFocusable(true);
        view.setClickable(true);
        view.setPadding(dp(12), 0, dp(12), 0);
        view.setMinHeight(dp(BrowseLayout.CHIP_DP));
        view.setOnFocusChangeListener((v, focused) -> {
            if (focused) filterRow = row;
            paintChip((TextView) v, selected(row, index), focused);
        });
        view.setOnClickListener(v -> choose(row, index));
        view.setOnKeyListener((v, key, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (!isDpad(key)) return false;
            int size = filterRows.get(row).getChildCount();
            apply(BrowseFocus.filter(row, index, size, BrowseLayout.FILTER_ROWS, key));
            return true;
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.setMarginEnd(dp(8));
        view.setLayoutParams(lp);
        return view;
    }

    private void choose(int row, int index) {
        if (row == 0) type = TYPE_KEYS[index];
        else if (row == 1) sort = SORT_KEYS[index];
        else if (row == 2) windowDays = ContentWindow.DAYS[index];
        else {
            if (languages[index] && ContentLanguages.any(without(languages, index))) languages[index] = false;
            else if (!languages[index]) languages[index] = true;
            else return;
        }
        paintFilters();
        reload();
    }

    private boolean selected(int row, int index) {
        if (row == 0) return TYPE_KEYS[index].equals(type);
        if (row == 1) return SORT_KEYS[index].equals(sort);
        if (row == 2) return ContentWindow.DAYS[index] == windowDays;
        return languages[index];
    }

    private void paintFilters() {
        for (int row = 0; row < filterRows.size(); row++) {
            LinearLayout line = filterRows.get(row);
            for (int i = 0; i < line.getChildCount(); i++) {
                TextView chip = (TextView) line.getChildAt(i);
                paintChip(chip, selected(row, i), chip.hasFocus());
            }
        }
    }

    private void paintChip(TextView chip, boolean selected, boolean focused) {
        int accent = RaiderTheme.accent(this);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(dp(4));
        if (selected || focused) {
            drawable.setColor(focused ? RaiderTheme.theme(this).focus : accent);
            chip.setTextColor(RaiderColors.inkOn(focused ? RaiderTheme.theme(this).focus : accent));
        } else {
            drawable.setColor(0x00000000);
            drawable.setStroke(dp(1), 0x66FFFFFF);
            chip.setTextColor(RaiderTheme.theme(this).onSurface);
        }
        chip.setBackground(drawable);
    }

    private boolean apply(BrowseFocus.Step step) {
        if (step.stay) return true;
        if (step.zone == BrowseFocus.FILTER) {
            int row = Math.max(0, Math.min(step.row, filterRows.size() - 1));
            LinearLayout line = filterRows.get(row);
            int index = Math.max(0, Math.min(step.index, line.getChildCount() - 1));
            line.getChildAt(index).requestFocus();
            return true;
        }
        focusPoster(step.index);
        return true;
    }

    private void focusPoster(int index) {
        if (adapter.getItemCount() == 0) return;
        int safe = Math.max(0, Math.min(index, adapter.getItemCount() - 1));
        grid.scrollToPosition(safe);
        RecyclerView.ViewHolder holder = grid.findViewHolderForAdapterPosition(safe);
        if (holder != null) {
            holder.itemView.requestFocus();
            return;
        }
        grid.post(() -> {
            RecyclerView.ViewHolder ready = grid.findViewHolderForAdapterPosition(safe);
            if (ready != null) ready.itemView.requestFocus();
        });
    }

    private void reload() {
        empty.setVisibility(View.GONE);
        TmdbRepository.loadBrowse(this, type, sort, windowDays, ContentLanguages.selected(ContentLanguages.join(languages)), titles -> {
            if (isFinishing()) return;
            adapter.submit(titles);
            empty.setVisibility(titles == null || titles.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private static boolean[] without(boolean[] values, int index) {
        boolean[] copy = values.clone();
        copy[index] = false;
        return copy;
    }

    private static boolean isDpad(int key) {
        return key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT
                || key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class GridAdapter extends RecyclerView.Adapter<GridAdapter.Holder> {
        private List<TmdbModels.Title> items = Collections.emptyList();

        void submit(List<TmdbModels.Title> next) {
            items = next == null ? Collections.emptyList() : next;
            notifyDataSetChanged();
        }

        @Override public int getItemCount() { return items.size(); }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int posterW = dp(BrowseLayout.POSTER_DP);
            int posterH = Math.round(posterW * 3f / 2f);
            LinearLayout item = new LinearLayout(BrowseActivity.this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setFocusable(true);
            item.setClickable(true);
            FrameLayout card = new FrameLayout(BrowseActivity.this);
            ImageView image = new ImageView(BrowseActivity.this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackgroundColor(0xFF1A1B2E);
            image.setClipToOutline(true);
            image.setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(4));
                }
            });
            card.addView(image, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            item.addView(card, new LinearLayout.LayoutParams(posterW, posterH));
            TextView name = label();
            name.setMaxLines(1);
            LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(posterW, dp(18));
            nameLp.topMargin = dp(4);
            item.addView(name, nameLp);
            TextView meta = label();
            meta.setTextColor(RaiderTheme.theme(BrowseActivity.this).secondary);
            meta.setTextSize(12);
            item.addView(meta, new LinearLayout.LayoutParams(posterW, dp(16)));
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(posterW + dp(BrowseLayout.COL_GAP_DP), posterH + dp(BrowseLayout.LABEL_DP));
            item.setLayoutParams(lp);
            return new Holder(item, image, card, name, meta);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            TmdbModels.Title title = items.get(position);
            holder.name.setText(title.name);
            holder.meta.setText(TmdbModels.cardMeta(title));
            holder.itemView.setContentDescription(title.name + ", " + TmdbModels.cardMeta(title));
            holder.itemView.setOnClickListener(v -> startActivity(new Intent(BrowseActivity.this, TitleActivity.class)
                    .putExtra("media", title.media).putExtra("id", title.id)));
            holder.itemView.setOnFocusChangeListener((v, focused) -> {
                GradientDrawable stroke = new GradientDrawable();
                stroke.setCornerRadius(dp(4));
                stroke.setColor(0);
                if (focused) stroke.setStroke(dp(2), RaiderTheme.accent(BrowseActivity.this));
                holder.card.setForeground(stroke);
            });
            holder.itemView.setOnKeyListener((v, key, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN || !isDpad(key)) return false;
                int pos = holder.getBindingAdapterPosition();
                if (pos < 0) return true;
                apply(BrowseFocus.grid(pos, columns, getItemCount(), BrowseLayout.FILTER_ROWS, key));
                return true;
            });
            ContentImages.load(BrowseActivity.this, holder.image, title.poster, dp(BrowseLayout.POSTER_DP));
        }

        private TextView label() {
            TextView view = new TextView(BrowseActivity.this);
            view.setTextColor(RaiderTheme.theme(BrowseActivity.this).onSurface);
            view.setTextSize(13);
            view.setTypeface(Fonts.of(BrowseActivity.this, Fonts.HOME, R.font.outfit_regular));
            view.setIncludeFontPadding(false);
            view.setMaxLines(1);
            view.setEllipsize(android.text.TextUtils.TruncateAt.END);
            return view;
        }

        final class Holder extends RecyclerView.ViewHolder {
            final ImageView image;
            final FrameLayout card;
            final TextView name;
            final TextView meta;
            Holder(View item, ImageView image, FrameLayout card, TextView name, TextView meta) {
                super(item);
                this.image = image;
                this.card = card;
                this.name = name;
                this.meta = meta;
            }
        }
    }
}
