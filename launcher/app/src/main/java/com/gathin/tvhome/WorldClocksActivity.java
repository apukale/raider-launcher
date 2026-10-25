package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Full-screen world clocks. Back returns Home. Change background cycles photos, same as Panchang. */
public class WorldClocksActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ArrayList<TextView> times = new ArrayList<>();
    private final ArrayList<TextView> offsets = new ArrayList<>();
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            long now = System.currentTimeMillis();
            for (TextView time : times) {
                Object zone = time.getTag();
                if (zone instanceof String) {
                    time.setText(WorldClocks.formatTime((String) zone, now));
                    if (fillsWidth(time)) fitClockTime(time);
                }
            }
            for (TextView offset : offsets) {
                Object zone = offset.getTag();
                if (zone instanceof String) offset.setText(WorldClocks.formatOffset((String) zone, now));
            }
            long delay = 60_000L - (now % 60_000L);
            main.postDelayed(this, Math.max(1000L, delay));
        }
    };
    private LinearLayout screenRoot, stage;
    private WorldClockCards.Frame clockFrame;
    private TextView indiaTime, indiaOffset;
    private TextView empty;
    private View changeBackground;
    private Bitmap backgroundBitmap;
    private boolean backDown;
    private int bgFailures;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        Config.load(this);
        build();
        loadBackground();
    }

    @Override protected void onResume() {
        super.onResume();
        NightEffects.dim(this, screenRoot, NightState.isActive(getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE), System.currentTimeMillis()));
        render();
        main.removeCallbacks(tick);
        tick.run();
    }

    @Override protected void onPause() {
        main.removeCallbacks(tick);
        super.onPause();
    }

    @Override public void onBackPressed() { goHome(); }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_ESCAPE) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.getRepeatCount() == 0) backDown = true;
                return true;
            }
            if (event.getAction() == KeyEvent.ACTION_UP) {
                boolean go = backDown && !event.isCanceled();
                backDown = false;
                if (go) goHome();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void goHome() {
        backDown = false;
        Intent home = new Intent(this, MainActivity.class);
        home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try { startActivity(home); } catch (RuntimeException ignored) {}
        finish();
    }

    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + .5f); }

    private TextView text(String value, float sp, int font, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Fonts.of(this, Fonts.HOME, font));
        view.setIncludeFontPadding(false);
        view.setGravity(Gravity.CENTER);
        return view;
    }

    private ImageButton iconButton(int icon, String desc, View.OnClickListener listener) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(icon);
        button.setContentDescription(desc);
        button.setBackground(RaiderTheme.focusBackground(this, RaiderTheme.accent(this), false));
        RaiderTheme.applyFocusAnimation(button);
        button.setImageTintList(RaiderTheme.accentStateList(RaiderTheme.accent(this)));
        button.setScaleType(ImageView.ScaleType.CENTER);
        button.setFocusable(true);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(52), dp(42));
        lp.setMarginStart(dp(12));
        button.setLayoutParams(lp);
        return button;
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        screenRoot = root;
        root.setOrientation(LinearLayout.VERTICAL);
        float density = getResources().getDisplayMetrics().density;
        int heightDp = density <= 0f ? 540 : Math.round(getResources().getDisplayMetrics().heightPixels / density);
        clockFrame = WorldClockCards.frame(heightDp <= 0 ? 540 : heightDp);
        root.setPadding(dp(clockFrame.screenPadH), dp(clockFrame.screenPadV), dp(clockFrame.screenPadH), dp(clockFrame.screenPadV));
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] {0xFF12132A, 0xFF1C1E3F}));
        setContentView(root);
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("World clocks", 28, R.font.outfit_medium, 0xFFF4F1EA);
        title.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        header.addView(title, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        long now = System.currentTimeMillis();
        LinearLayout india = new LinearLayout(this);
        india.setOrientation(LinearLayout.VERTICAL);
        india.setGravity(Gravity.END);
        india.setPadding(dp(28), 0, 0, 0);
        LinearLayout indiaRow = new LinearLayout(this);
        indiaRow.setOrientation(LinearLayout.HORIZONTAL);
        indiaRow.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        TextView indiaLabel = text(WorldClocks.INDIA_LABEL, 16, R.font.outfit_medium, 0xFFA9ABC7);
        indiaLabel.setGravity(Gravity.CENTER_VERTICAL);
        indiaRow.addView(indiaLabel);
        indiaTime = text(WorldClocks.formatTime(WorldClocks.INDIA_ZONE, now), 26, R.font.outfit_light, Color.WHITE);
        indiaTime.setGravity(Gravity.CENTER_VERTICAL);
        indiaTime.setTag(WorldClocks.INDIA_ZONE);
        indiaTime.setContentDescription("India time");
        LinearLayout.LayoutParams indiaTimeLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        indiaTimeLp.setMarginStart(dp(10));
        indiaRow.addView(indiaTime, indiaTimeLp);
        india.addView(indiaRow);
        indiaOffset = text(WorldClocks.formatOffset(WorldClocks.INDIA_ZONE, now), 16, R.font.outfit_regular, 0xFFA9ABC7);
        indiaOffset.setTag(WorldClocks.INDIA_ZONE);
        indiaOffset.setContentDescription("India offset");
        LinearLayout.LayoutParams indiaOffsetLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        indiaOffsetLp.topMargin = dp(2);
        india.addView(indiaOffset, indiaOffsetLp);
        header.addView(india, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        ImageButton back = iconButton(R.drawable.ic_back, "Back", v -> goHome());
        header.addView(back);
        changeBackground = iconButton(R.drawable.ic_wallpaper, "Change background", v -> nextBackground());
        header.addView(changeBackground);
        TextView cities = text("Cities", 16, R.font.outfit_medium, 0xFFF4F1EA);
        cities.setContentDescription("Change cities");
        cities.setFocusable(true);
        cities.setClickable(true);
        cities.setPadding(dp(18), dp(10), dp(18), dp(10));
        cities.setBackground(RaiderTheme.focusBackground(this, RaiderTheme.accent(this), false));
        RaiderTheme.applyFocusAnimation(cities);
        cities.setOnClickListener(v -> openClockSettings());
        cities.setMinHeight(dp(42));
        cities.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams citiesLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        citiesLp.setMarginStart(dp(12));
        header.addView(cities, citiesLp);
        root.addView(header);
        stage = new LinearLayout(this);
        stage.setOrientation(LinearLayout.VERTICAL);
        stage.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams stageLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        stageLp.topMargin = dp(clockFrame.headerGap);
        root.addView(stage, stageLp);
        empty = text("No other cities set", 16, R.font.outfit_regular, 0xFFA9ABC7);
        back.requestFocus();
    }

    private void openClockSettings() {
        Intent settings = new Intent(this, LauncherSettingsActivity.class);
        settings.putExtra(SettingsMenu.OPEN_CATEGORY, SettingsMenu.CALENDAR);
        try { startActivity(settings); } catch (RuntimeException ignored) {}
    }

    private void render() {
        stage.removeAllViews();
        times.clear();
        offsets.clear();
        long now = System.currentTimeMillis();
        if (indiaTime != null) {
            indiaTime.setText(WorldClocks.formatTime(WorldClocks.INDIA_ZONE, now));
            indiaTime.setTag(WorldClocks.INDIA_ZONE);
            times.add(indiaTime);
        }
        if (indiaOffset != null) {
            indiaOffset.setText(WorldClocks.formatOffset(WorldClocks.INDIA_ZONE, now));
            indiaOffset.setTag(WorldClocks.INDIA_ZONE);
            offsets.add(indiaOffset);
        }
        java.util.function.BiFunction<String, String, String> prefs =
                getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE)::getString;
        int accent = RaiderTheme.accent(this);
        List<WorldClocks.Clock> cities = WorldClocks.cities(prefs);
        boolean none = cities.isEmpty();
        if (none) {
            stage.setGravity(Gravity.CENTER);
            stage.addView(empty);
            return;
        }
        stage.setGravity(Gravity.CENTER_HORIZONTAL);
        int columns = WorldClockCards.COLUMNS;
        int rowCount = WorldClockCards.rows(cities.size());
        int index = 0;
        for (int r = 0; r < rowCount; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setBaselineAligned(false);
            row.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
            if (r > 0) rowLp.topMargin = dp(clockFrame.gridGap);
            int inRow = Math.min(columns, cities.size() - index);
            for (int c = 0; c < inRow; c++) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
                if (c > 0) lp.setMarginStart(dp(clockFrame.gridGap));
                row.addView(clockCard(cities.get(index), accent, now), lp);
                index++;
            }
            if (inRow < columns) {
                View spacer = new View(this);
                spacer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, columns - inRow);
                spacerLp.setMarginStart(dp(clockFrame.gridGap));
                row.addView(spacer, spacerLp);
            }
            stage.addView(row, rowLp);
        }
    }

    private LinearLayout clockCard(WorldClocks.Clock clock, int accent, long now) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(clockFrame.cardPadH), dp(clockFrame.cardPadV), dp(clockFrame.cardPadH), dp(clockFrame.cardPadV));
        GradientDrawable fill = new GradientDrawable();
        fill.setColor(WorldClockCards.FILL);
        fill.setCornerRadius(dp(16));
        fill.setStroke(Math.max(1, dp(1)), WorldClockCards.STROKE);
        card.setBackground(fill);
        card.setFocusable(false);
        TextView label = text(clock.label, WorldClockCards.LABEL_SP, R.font.outfit_medium, accent);
        label.setMaxLines(2);
        label.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(label, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        card.addView(fixedGap(WorldClockCards.LABEL_GAP_DP));
        TextView time = text(WorldClocks.formatTime(clock.zone, now), clockFrame.timeSp, R.font.outfit_light, Color.WHITE);
        time.setIncludeFontPadding(true);
        time.setTag(clock.zone);
        time.setMaxLines(1);
        time.setEllipsize(null);
        time.setHorizontallyScrolling(false);
        time.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (right - left != oldRight - oldLeft) fitClockTime((TextView) v);
        });
        card.addView(time, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        times.add(time);
        card.addView(fixedGap(WorldClockCards.OFFSET_GAP_DP));
        String offsetText = WorldClocks.formatOffset(clock.zone, now);
        TextView offset = text(offsetText, WorldClockCards.OFFSET_SP, R.font.outfit_regular, 0xFFA9ABC7);
        offset.setTag(clock.zone);
        card.addView(offset, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        offsets.add(offset);
        card.setContentDescription(clock.label + " " + time.getText() + " " + offsetText);
        return card;
    }

    /** City times are match-parent. The India header time is wrap-content and keeps its size. */
    private static boolean fillsWidth(TextView time) {
        android.view.ViewGroup.LayoutParams lp = time.getLayoutParams();
        return lp != null && lp.width == android.view.ViewGroup.LayoutParams.MATCH_PARENT;
    }

    /** Shrink the clock face only as far as needed so the full time, including AM/PM, stays visible. */
    private void fitClockTime(TextView time) {
        int width = time.getWidth() - time.getPaddingLeft() - time.getPaddingRight();
        if (width <= 4) return;
        CharSequence value = time.getText();
        if (value == null || value.length() == 0) return;
        android.text.TextPaint paint = new android.text.TextPaint(time.getPaint());
        paint.setTextSize(1f);
        float measured = paint.measureText(value.toString());
        float maxPx = WorldClockCards.TIME_SP * getResources().getDisplayMetrics().scaledDensity;
        float sizePx = WorldClockCards.fitTextPx(width - 4f, measured, maxPx);
        if (Math.abs(time.getTextSize() - sizePx) < 0.5f) return;
        time.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, sizePx);
    }

    private View fixedGap(int dpValue) {
        View gap = new View(this);
        gap.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        gap.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(dpValue)));
        return gap;
    }

    private void nextBackground() {
        if (!PhotosMaster.on(this)) { changeBackground.setEnabled(true); changeBackground.setAlpha(1f); screenRoot.setBackground(PhotoGradients.drawable(new java.util.Random().nextInt())); return; }
        List<PhotoStore.Photo> photos = PhotoStore.list(this, PhotoStore.chosenSource(this));
        if (photos.isEmpty()) return;
        ArrayList<String> files = new ArrayList<>();
        for (PhotoStore.Photo photo : photos) files.add(photo.file);
        String current = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE).getString(WorldClocks.BACKGROUND, "");
        String selected = PhotoBackgroundCycle.nextFile(files, current);
        getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE).edit().putString(WorldClocks.BACKGROUND, selected).apply();
        loadBackground();
    }

    private void loadBackground() {
        if (!PhotosMaster.on(this)) { changeBackground.setEnabled(true); changeBackground.setAlpha(1f); screenRoot.setBackground(PhotoGradients.drawable(new java.util.Random().nextInt())); return; }
        List<PhotoStore.Photo> photos = PhotoStore.list(this, PhotoStore.chosenSource(this));
        if (photos.isEmpty()) {
            changeBackground.setEnabled(false);
            changeBackground.setAlpha(0.4f);
            screenRoot.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] {0xFF12132A, 0xFF1C1E3F}));
            return;
        }
        changeBackground.setEnabled(true);
        changeBackground.setAlpha(1f);
        String wanted = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE).getString(WorldClocks.BACKGROUND, "");
        boolean found = false;
        for (PhotoStore.Photo photo : photos) if (photo.file.equals(wanted)) found = true;
        if (!found) {
            wanted = photos.get(PhotoShuffle.pick(photos.size(), -1, new java.util.Random())).file;
            getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE).edit().putString(WorldClocks.BACKGROUND, wanted).apply();
        }
        final String file = wanted;
        worker.execute(() -> {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(new File(PhotoStore.dir(this), file).getAbsolutePath(), bounds);
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            int sample = 1;
            while (sample < 1024 && (bounds.outWidth / sample > width || bounds.outHeight / sample > height)) sample *= 2;
            bounds.inJustDecodeBounds = false;
            bounds.inSampleSize = sample;
            bounds.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bitmap = BitmapFactory.decodeFile(new File(PhotoStore.dir(this), file).getAbsolutePath(), bounds);
            main.post(() -> {
                if (isFinishing()) return;
                if (bitmap == null) {
                    android.util.Log.w("TvHomePhotos", "world clocks bg decode failed: " + file);
                    if (bgFailures++ < 3) { nextBackground(); return; }
                    changeBackground.setEnabled(false);
                    return;
                }
                bgFailures = 0;
                backgroundBitmap = bitmap;
                BitmapDrawable image = new BitmapDrawable(getResources(), bitmap);
                image.setAlpha(255);
                screenRoot.setBackground(new LayerDrawable(new android.graphics.drawable.Drawable[] {image, new ColorDrawable(0x99000000)}));
            });
        });
    }

    @Override protected void onDestroy() {
        main.removeCallbacks(tick);
        if (screenRoot != null) screenRoot.setBackground(null);
        backgroundBitmap = null;
        super.onDestroy();
        worker.shutdownNow();
    }
}
