package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.content.res.ColorStateList;
import android.view.View;

/** Runtime palette and lightweight focus treatments shared by settings and Panchang. */
final class RaiderTheme {
    static final String MARIGOLD = "Marigold";
    private RaiderTheme() { }
    static int accentColor(String name) { return RaiderColors.accentColor(name); }
    static int accent(Context context) { return accentColor(context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE).getString("accent_color", MARIGOLD)); }
    static GradientDrawable card(Context context) { GradientDrawable d = new GradientDrawable(); d.setColor(0xCC1A1B2E); d.setCornerRadius(dp(context,16)); d.setStroke(dp(context,1), 0x26FFFFFF); return d; }
    static android.graphics.drawable.Drawable focusBackground(Context context,int color, boolean focused) {
        GradientDrawable inside = new GradientDrawable(); inside.setCornerRadius(dp(context,16));
        inside.setColor(focused ? 0xEE30324A : 0xCC1A1B2E);
        inside.setStroke(dp(context,focused?2:1), focused ? color : 0x26FFFFFF);
        if(!focused)return inside;
        GradientDrawable glow=new GradientDrawable();glow.setCornerRadius(dp(context,19));glow.setColor(0x00000000);glow.setStroke(dp(context,5),(color&0x00FFFFFF)|0x59000000);
        android.graphics.drawable.LayerDrawable layers=new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{glow,inside});
        int inset=dp(context,3);layers.setLayerInset(1,inset,inset,inset,inset);return layers;
    }
    static android.graphics.drawable.Drawable toggleDrawable(Context context, boolean on, int color) { return new ToggleDrawable(context.getResources().getDisplayMetrics().density,on,color); }
    static GradientDrawable pillBackground(Context context,int color) { GradientDrawable d=new GradientDrawable();d.setColor(0xCC1A1B2E);d.setCornerRadius(dp(context,22));d.setStroke(dp(context,1),color);return d; }
    private static int dp(Context context,int value){return (int)(value*context.getResources().getDisplayMetrics().density+0.5f);}
    static android.graphics.drawable.Drawable chevronDrawable(Context context){return new ChevronDrawable(context.getResources().getDisplayMetrics().density,0xFFA9ABC7);}
    private static final class ChevronDrawable extends android.graphics.drawable.Drawable {private final float density;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);ChevronDrawable(float d,int color){density=d;paint.setColor(color);paint.setStrokeWidth(2*d);paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);}@Override public int getIntrinsicWidth(){return (int)(16*density);}@Override public int getIntrinsicHeight(){return (int)(24*density);}@Override public void draw(Canvas c){android.graphics.Rect b=getBounds();float x=b.exactCenterX(),y=b.exactCenterY();c.drawLine(x-2*density,y-5*density,x+2*density,y,paint);c.drawLine(x+2*density,y,x-2*density,y+5*density,paint);}@Override public void setAlpha(int a){paint.setAlpha(a);}@Override public void setColorFilter(android.graphics.ColorFilter f){paint.setColorFilter(f);}@Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}}
    private static final class ToggleDrawable extends android.graphics.drawable.Drawable {private final float density;private final boolean on;private final int accent;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);ToggleDrawable(float d,boolean state,int color){density=d;on=state;accent=color;}@Override public int getIntrinsicWidth(){return (int)(52*density);}@Override public int getIntrinsicHeight(){return (int)(30*density);}@Override public void draw(Canvas canvas){android.graphics.Rect b=getBounds();float h=b.height(),w=b.width();paint.setColor(on?accent:0xFF3A3C5A);canvas.drawRoundRect(new RectF(b.left,b.top,b.right,b.bottom),h/2,h/2,paint);paint.setColor(0xFFFFFFFF);float radius=h*.38f;float cx=on?b.right-h/2:b.left+h/2;canvas.drawCircle(cx,b.exactCenterY(),radius,paint);}@Override public void setAlpha(int alpha){paint.setAlpha(alpha);}@Override public void setColorFilter(android.graphics.ColorFilter filter){paint.setColorFilter(filter);}@Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}}
    static GradientDrawable toggleBackground(int color, boolean on) {
        GradientDrawable d = new GradientDrawable(); d.setShape(GradientDrawable.RECTANGLE); d.setCornerRadius(30f); d.setColor(on ? color : 0xFF3A3C5A); return d;
    }
    static ColorStateList accentStateList(int color) { return new ColorStateList(new int[][]{new int[]{android.R.attr.state_focused}, new int[]{}}, new int[]{color, 0xFFF4F1EA}); }
    static void applyFocusAnimation(View view) {
        view.setOnFocusChangeListener((v, focused) -> {
            int color = accent(v.getContext());
            v.setBackground(focusBackground(v.getContext(),color, focused));
            v.animate().scaleX(focused ? 1.03f : 1f).scaleY(focused ? 1.03f : 1f).setDuration(150L).start();
        });
    }
}
