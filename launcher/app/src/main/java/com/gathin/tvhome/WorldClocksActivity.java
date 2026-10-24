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
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            long now = System.currentTimeMillis();
            for (TextView time : times) {
                Object zone = time.getTag();
                if (zone instanceof String) time.setText(WorldClocks.formatTime((String) zone, now));
            }
            long delay = 60_000L - (now % 60_000L);
            main.postDelayed(this, Math.max(1000L, delay));
        }
    };
    private LinearLayout screenRoot, clocks;
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
        NightEffects.dim(screenRoot, NightState.isActive(getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE), System.currentTimeMillis()));
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
        root.setPadding(dp(48), dp(27), dp(48), dp(27));
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] {0xFF12132A, 0xFF1C1E3F}));
        setContentView(root);
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("World clocks", 28, R.font.outfit_medium, 0xFFF4F1EA);
        title.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        ImageButton back = iconButton(R.drawable.ic_back, "Back", v -> goHome());
        header.addView(back);
        changeBackground = iconButton(R.drawable.ic_wallpaper, "Change background", v -> nextBackground());
        header.addView(changeBackground);
        root.addView(header);
        clocks = new LinearLayout(this);
        clocks.setOrientation(LinearLayout.HORIZONTAL);
        clocks.setGravity(Gravity.CENTER);
        clocks.setBaselineAligned(false);
        LinearLayout.LayoutParams clocksLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
        clocksLp.topMargin = dp(16);
        root.addView(clocks, clocksLp);
        empty = text("No other cities set", 16, R.font.outfit_regular, 0xFFA9ABC7);
        empty.setVisibility(View.GONE);
        LinearLayout.LayoutParams emptyLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        emptyLp.bottomMargin = dp(8);
        root.addView(empty, emptyLp);
        back.requestFocus();
    }

    private void render() {
        clocks.removeAllViews();
        times.clear();
        List<WorldClocks.Clock> rows = WorldClocks.entries(getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE)::getString);
        boolean share = rows.size() > 1;
        int accent = RaiderTheme.accent(this);
        long now = System.currentTimeMillis();
        for (int i = 0; i < rows.size(); i++) {
            WorldClocks.Clock clock = rows.get(i);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER_HORIZONTAL);
            card.setPadding(dp(14), dp(18), dp(14), dp(18));
            card.setBackground(RaiderTheme.card(this));
            TextView label = text(clock.label, share ? 18 : 22, R.font.outfit_medium, accent);
            label.setMaxLines(2);
            label.setEllipsize(TextUtils.TruncateAt.END);
            card.addView(label);
            TextView time = text(WorldClocks.formatTime(clock.zone, now), share ? 32 : 52, R.font.outfit_light, Color.WHITE);
            time.setTag(clock.zone);
            LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            timeLp.topMargin = dp(8);
            card.addView(time, timeLp);
            times.add(time);
            if (!clock.label.equals(clock.zone)) {
                TextView zone = text(clock.zone, 13, R.font.outfit_regular, 0xFFA9ABC7);
                zone.setMaxLines(2);
                zone.setEllipsize(TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams zoneLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                zoneLp.topMargin = dp(6);
                card.addView(zone, zoneLp);
            }
            card.setContentDescription(clock.label + " " + time.getText());
            LinearLayout.LayoutParams lp = share
                    ? new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1)
                    : new LinearLayout.LayoutParams(dp(320), LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.setMarginStart(dp(12));
            clocks.addView(card, lp);
        }
        empty.setVisibility(rows.size() <= 1 ? View.VISIBLE : View.GONE);
    }

    private void nextBackground() {
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
