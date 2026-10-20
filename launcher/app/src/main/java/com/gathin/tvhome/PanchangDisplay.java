package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Shared live Panchang formatting and day cache for Home and the screensaver. */
final class PanchangDisplay {
    private static int cachedYear=-1,cachedDay=-1;
    private static Panchang.Result cached;
    private PanchangDisplay(){}
    static synchronized Panchang.Result day(Calendar c){int y=c.get(Calendar.YEAR),d=c.get(Calendar.DAY_OF_YEAR);if(cached==null||y!=cachedYear||d!=cachedDay){cached=Panchang.forDate(y,c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH));cachedYear=y;cachedDay=d;}return cached;}
    static double jdNow(){return System.currentTimeMillis()/86400000.0+2440587.5;}
    static String time(double jd){SimpleDateFormat f=new SimpleDateFormat("h:mm a",Locale.getDefault()); return f.format(new Date((long)((jd-2440587.5)*86400000)));}
    static String moon(double jd){return "Moon in "+Panchang.moonRashi(jd)+" till "+time(Panchang.moonRashiEndJd(jd));}
    static final class MoonDrawable extends Drawable {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); private final double elongation;
        MoonDrawable(double jd){elongation=Panchang.elongation(jd);}
        @Override public void draw(Canvas c){android.graphics.Rect b=getBounds();float r=Math.min(b.width(),b.height())/2f,cx=b.exactCenterX(),cy=b.exactCenterY();p.setColor(0x66888888);c.drawCircle(cx,cy,r,p);p.setColor(Color.WHITE);boolean waxing=elongation<180;float cos=(float)Math.cos(Math.toRadians(elongation)),half=Math.abs(cos)*r,edge=cx+((waxing==(cos>=0))?half:-half);Path path=new Path();if(waxing){path.moveTo(cx,cy-r);path.arcTo(new RectF(cx-r,cy-r,cx+r,cy+r),-90,180);path.cubicTo(edge,cy+r*.55f,edge,cy-r*.55f,cx,cy-r);}else{path.moveTo(cx,cy-r);path.arcTo(new RectF(cx-r,cy-r,cx+r,cy+r),-90,-180);path.cubicTo(edge,cy-r*.55f,edge,cy+r*.55f,cx,cy+r);}path.close();c.drawPath(path,p);}
        @Override public void setAlpha(int a){p.setAlpha(a);}@Override public void setColorFilter(android.graphics.ColorFilter f){p.setColorFilter(f);}@Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
    }
    static double nextTransition(Calendar c,double now){int y=c.get(Calendar.YEAR),m=c.get(Calendar.MONTH)+1,d=c.get(Calendar.DAY_OF_MONTH);double next=Math.min(Panchang.tithiEndJd(now),Math.min(Panchang.nakshatraEndJd(now),Panchang.moonRashiEndJd(now)));double rs=Panchang.rahuStartJd(y,m,d),re=Panchang.rahuEndJd(y,m,d);if(rs>now)next=Math.min(next,rs);if(re>now)next=Math.min(next,re);Calendar midnight=(Calendar)c.clone();midnight.add(Calendar.DAY_OF_YEAR,1);midnight.set(Calendar.HOUR_OF_DAY,0);midnight.set(Calendar.MINUTE,0);midnight.set(Calendar.SECOND,0);midnight.set(Calendar.MILLISECOND,0);return Math.min(next,midnight.getTimeInMillis()/86400000.0+2440587.5);}
    static long transitionDelayMs(Calendar c,double now){
        double next=nextTransition(c,now);
        if(!Double.isFinite(next)){
            Calendar midnight=(Calendar)c.clone();midnight.add(Calendar.DAY_OF_YEAR,1);midnight.set(Calendar.HOUR_OF_DAY,0);midnight.set(Calendar.MINUTE,0);midnight.set(Calendar.SECOND,0);midnight.set(Calendar.MILLISECOND,0);
            next=midnight.getTimeInMillis()/86400000.0+2440587.5;
        }
        double millis=(next-now)*86400000.0;
        if(Double.isNaN(millis)||Double.isInfinite(millis))return 60_000L;
        if(millis<=0)return 60_000L;
        return Math.max(1_000L,(long)millis+1_000L);
    }
}
