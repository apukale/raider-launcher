package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.animation.PathInterpolator;

/** 2.11.7 Launcher settings look (Google TV 2025 / tvOS 26 style): dark tonal surfaces, photo accent, inset focus glow. */
final class SettingsStyle {
    static final int SURFACE = 0xD11C1B1F;      // #1C1B1F at ~82%
    static final int ON_SURFACE = 0xFFE6E1E5;
    static final int SECONDARY = 0xFFCAC4D0;
    /** Accent taken from the background photo (Palette); 0 = none, use the saved theme accent. */
    static int photoAccent = 0;
    private static final PathInterpolator EASE = new PathInterpolator(0f, 0f, 0.2f, 1f);
    private SettingsStyle() {}

    static int accent(Context c) {
        String n = c.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE).getString("accent_color", RaiderTheme.DEFAULT);
        if (photoAccent != 0 && RaiderTheme.DEFAULT.equals(RaiderColors.theme(n).id)) return photoAccent;
        return RaiderTheme.accentColor(n);
    }
    /** Focus pill and outline. A photo accent, when one is set, still drives the default theme. */
    static int focusColor(Context c) {
        String n = c.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE).getString("accent_color", RaiderTheme.DEFAULT);
        RaiderColors.Theme theme = RaiderColors.theme(n);
        if (photoAccent != 0 && RaiderTheme.DEFAULT.equals(theme.id)) return photoAccent;
        return theme.focus;
    }
    /** Lighter accent for right-aligned values. */
    static int value(Context c) { return blend(accent(c), 0xFFFFFFFF, 0.45f); }
    static int withAlpha(int color, int alpha) { return (color & 0x00FFFFFF) | (alpha << 24); }
    static int blend(int a, int b, float t) {
        int r = (int) (Color.red(a) + (Color.red(b) - Color.red(a)) * t), g = (int) (Color.green(a) + (Color.green(b) - Color.green(a)) * t), bl = (int) (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t);
        return Color.rgb(r, g, bl);
    }
    /** Palette swatch -> an accent bright enough for dark surfaces; 0 when the photo is too grey. */
    static int fromSwatch(int rgb) {
        float[] hsv = new float[3];
        Color.colorToHSV(rgb, hsv);
        if (hsv[1] < 0.15f) return 0;
        hsv[1] = Math.max(hsv[1], 0.45f);
        hsv[2] = Math.max(hsv[2], 0.88f);
        return Color.HSVToColor(hsv);
    }
    static GradientDrawable card(Context c) { GradientDrawable d = new GradientDrawable(); d.setColor(RaiderTheme.theme(c).surface); d.setCornerRadius(dp(c, 16)); return d; }
    static GradientDrawable pill(Context c, int color) { GradientDrawable d = new GradientDrawable(); d.setColor(SURFACE); d.setCornerRadius(dp(c, 100)); return d; }
    static Drawable scrim() { return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x8C000000, 0xB3000000, 0xD9000000}); }
    static Drawable fallbackBackground() { return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xFF1C1B1F, 0xFF0E0E11}); }
    /**
     * Selected or focused left-rail item. The row is 36dp, so a capsule radius is about 18dp.
     * 4dp is a slight corner on both ends, not a pill.
     */
    static final int RAIL_CORNER_DP = 4;
    /**
     * Right-panel rows and pickers use the same slight corner as the left rail.
     * A capsule is only for a real pill.
     */
    static final int CONTENT_CORNER_DP = RAIL_CORNER_DP;
    /** Corner radius in dp. Negative means a capsule. */
    static float focusCornerDp(boolean capsule) { return capsule ? -1f : CONTENT_CORNER_DP; }
    /** Rows: transparent until focused, then a soft glow + 3 dp accent border drawn inside the row's bounds. */
    static Drawable focus(Context c, int color, boolean focused) { return focused ? new Glow(c, color, withAlpha(color, 0x24), true, false) : new ColorDrawable(0); }
    static Drawable rail(Context c, int color, boolean selected, boolean focused) { return new Glow(c, color, selected ? (color | 0xFF000000) : 0, focused, true, RAIL_CORNER_DP); }
    static Drawable glow(Context c, int color, int fill, boolean focused, boolean pill) { return new Glow(c, color, fill, focused, pill); }
    static Drawable toggle(Context c, boolean on, int color) { return RaiderTheme.toggleDrawable(c, on, color); }
    static void scale(View v, boolean focused) {
        v.animate().scaleX(focused ? 1.03f : 1f).scaleY(focused ? 1.03f : 1f).setDuration(300).setInterpolator(EASE).start();
    }
    static void focusAnim(View view) {
        view.setOnFocusChangeListener((v, focused) -> { v.setBackground(focus(v.getContext(), focusColor(v.getContext()), focused)); scale(v, focused); });
    }
    private static int dp(Context c, int v) { return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f); }

    private static final class Glow extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int color, fill; private final boolean focused, pill; private final float d;
        /** Corner radius in dp. Negative means a capsule (half the drawn height). */
        private final float radiusDp;
        Glow(Context c, int color, int fill, boolean focused, boolean pill) { this(c, color, fill, focused, pill, focusCornerDp(pill)); }
        Glow(Context c, int color, int fill, boolean focused, boolean pill, float radiusDp) { this.color = color; this.fill = fill; this.focused = focused; this.pill = pill; this.radiusDp = radiusDp; d = c.getResources().getDisplayMetrics().density; }
        @Override public void draw(Canvas canvas) {
            android.graphics.Rect b = getBounds();
            float inset = (pill ? 3 : 5) * d;
            RectF r = new RectF(b.left + inset, b.top + inset, b.right - inset, b.bottom - inset);
            float radius = radiusDp < 0f ? r.height() / 2f : radiusDp * d;
            if (focused) {
                paint.setStyle(Paint.Style.FILL);
                for (int k = 0; k < 4; k++) {        // soft halo, entirely inside the bounds
                    float o = inset * (4 - k) / 4f;
                    paint.setColor(withAlpha(color, 0x0E + k * 0x06));
                    canvas.drawRoundRect(new RectF(r.left - o, r.top - o, r.right + o, r.bottom + o), radius + o, radius + o, paint);
                }
            }
            if (fill != 0) { paint.setStyle(Paint.Style.FILL); paint.setColor(fill); canvas.drawRoundRect(r, radius, radius, paint); }
            if (focused) {
                float sw = 3 * d; paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(sw); paint.setColor(color);
                RectF s = new RectF(r.left + sw / 2, r.top + sw / 2, r.right - sw / 2, r.bottom - sw / 2);
                canvas.drawRoundRect(s, radius - sw / 2, radius - sw / 2, paint);
            }
        }
        @Override public void setAlpha(int a) { paint.setAlpha(a); }
        @Override public void setColorFilter(android.graphics.ColorFilter f) { paint.setColorFilter(f); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
