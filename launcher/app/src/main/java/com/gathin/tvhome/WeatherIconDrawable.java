package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Tiny vector weather glyph, drawn locally without bitmap assets. */
final class WeatherIconDrawable extends Drawable {
    private final Paint p=new Paint(3);private final String group;private final boolean night;private final int color;
    WeatherIconDrawable(String group,boolean night,int color){this.group=group;this.night=night;this.color=color;p.setColor(color);p.setStrokeWidth(1.8f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);}
    @Override public void draw(Canvas c){float w=getBounds().width(),h=getBounds().height(),s=Math.min(w,h)/24f;c.save();c.translate(getBounds().left,getBounds().top);c.scale(s,s);p.setColor(color);
        if(group.equals("clear")&&night){p.setStyle(Paint.Style.FILL);c.drawCircle(11,10,6,p);p.setColor(0xFF151922);c.drawCircle(14,7,5,p);p.setColor(color);p.setStyle(Paint.Style.STROKE);}
        else if(group.equals("clear")){c.drawCircle(12,12,4,p);for(int i=0;i<8;i++){double a=i*Math.PI/4;c.drawLine((float)(12+7*Math.cos(a)),(float)(12+7*Math.sin(a)),(float)(12+10*Math.cos(a)),(float)(12+10*Math.sin(a)),p);}}
        else if(group.equals("cloud")||group.equals("fog")||group.equals("drizzle")||group.equals("rain")||group.equals("thunder")||group.equals("snow")){Path cloud=new Path();cloud.moveTo(5,16);cloud.cubicTo(1,16,2,10,6,10);cloud.cubicTo(7,4,16,4,17,10);cloud.cubicTo(23,10,23,16,19,16);cloud.close();p.setStyle(Paint.Style.STROKE);c.drawPath(cloud,p);
            if(group.equals("fog")){c.drawLine(5,19,19,19,p);c.drawLine(8,22,17,22,p);}else if(group.equals("rain")||group.equals("drizzle")){c.drawLine(8,18,7,21,p);c.drawLine(13,18,12,21,p);if(group.equals("rain"))c.drawLine(18,18,17,21,p);}else if(group.equals("snow")){c.drawCircle(8,20,1,p);c.drawCircle(14,20,1,p);c.drawCircle(18,18,1,p);}else if(group.equals("thunder")){Path bolt=new Path();bolt.moveTo(13,17);bolt.lineTo(10,21);bolt.lineTo(13,21);bolt.lineTo(11,24);c.drawPath(bolt,p);}}
        c.restore();}
    @Override public void setAlpha(int a){p.setAlpha(a);invalidateSelf();}@Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);invalidateSelf();}@Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
}
