package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import java.io.File;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/** Shared renderer and timer for the Android dream and manual screensaver activity. */
final class ScreensaverController {
    private static final long MOVE=60_000L;
    private static final int[] PALETTE={0xFF2B5CFF,0xFF00A39A,0xFF8A3FFC,0xFFE0661B,0xFFD6336C};
    private final Context context; private final FrameLayout root; private final Handler handler=new Handler(Looper.getMainLooper());
    private final Random random=new Random(); private final View content,ambient; private final ImageView photo,nextImage; private final TextView calendar1,calendar2,festival,credit;
    private List<PhotoStore.Photo> photos; private Bitmap current,next; private int index,colorIndex,color; private ValueAnimator glowAnimator; private boolean running,calendarEnabled;
    private final Runnable tick=new Runnable(){public void run(){place();shiftGlow();handler.postDelayed(this,MOVE);}};
    private final Runnable transitionTick=()->updatePanchang(true);
    private boolean receiverRegistered;
    private final BroadcastReceiver timeChanges=new BroadcastReceiver(){public void onReceive(Context c,Intent i){updatePanchang(true);}};
    private final Runnable photoTick=new Runnable(){public void run(){showNext();}};
    ScreensaverController(Context c,FrameLayout root){this.context=c;this.root=root;content=root.findViewById(R.id.dreamContent);ambient=root.findViewById(R.id.dreamAmbient);photo=root.findViewById(R.id.dreamPhoto);nextImage=root.findViewById(R.id.dreamPhotoNext);calendar1=root.findViewById(R.id.calendarLine1);calendar2=root.findViewById(R.id.calendarLine2);festival=root.findViewById(R.id.calendarFestival);credit=root.findViewById(R.id.photoCredit);}
    void start(){if(running)return;running=true;SharedPreferences p=context.getSharedPreferences(Config.SETTINGS_NAME,Context.MODE_PRIVATE);boolean photoStyle=p.getBoolean("dream_photos",false),calendarOn=p.getBoolean("dream_calendar",true);calendarEnabled=calendarOn;photos=PhotoStore.list(context,PhotoStore.chosenSource(context));boolean usePhotos=photoStyle&&!photos.isEmpty();photo.setVisibility(usePhotos?View.VISIBLE:View.GONE);nextImage.setVisibility(View.GONE);credit.setVisibility(usePhotos?View.VISIBLE:View.GONE);ambient.setVisibility(usePhotos?View.GONE:View.VISIBLE);calendar1.setVisibility(calendarOn?View.VISIBLE:View.GONE);calendar2.setVisibility(calendarOn?View.VISIBLE:View.GONE);festival.setVisibility(calendarOn?View.VISIBLE:View.GONE);
        colorIndex=random.nextInt(PALETTE.length);android.graphics.drawable.GradientDrawable glow=new android.graphics.drawable.GradientDrawable();glow.setGradientType(android.graphics.drawable.GradientDrawable.RADIAL_GRADIENT);glow.setGradientRadius(Math.max(1,root.getWidth())*.55f);ambient.setBackground(glow);setGlow(PALETTE[colorIndex]);
        root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(content.getWidth()>0){setGlow(color);place();}});
        if(calendarOn){IntentFilter tf=new IntentFilter();tf.addAction(Intent.ACTION_TIME_CHANGED);tf.addAction(Intent.ACTION_TIMEZONE_CHANGED);tf.addAction(Intent.ACTION_DATE_CHANGED);if(android.os.Build.VERSION.SDK_INT>=33)context.registerReceiver(timeChanges,tf,Context.RECEIVER_NOT_EXPORTED);else context.registerReceiver(timeChanges,tf);receiverRegistered=true;updatePanchang(true);}place();if(usePhotos){index=0;current=decode(photos.get(index));photo.setImageBitmap(current);setCredit();long mins=p.getLong("photo_interval_ms",60_000L);handler.postDelayed(photoTick,Math.max(30_000,mins));}
        handler.postDelayed(tick,MOVE);
    }
    private void updatePanchang(boolean force){if(!calendarEnabled)return;long nowMs=System.currentTimeMillis();double now=PanchangDisplay.jdNow();Calendar c=Calendar.getInstance();Panchang.Result r=PanchangDisplay.day(c);String line1=r.month+" "+Panchang.currentPaksha(now)+" "+Panchang.currentTithiName(now)+" till "+PanchangDisplay.time(Panchang.tithiEndJd(now));String line2="Nakshatra: "+Panchang.currentNakshatra(now)+" till "+PanchangDisplay.time(Panchang.nakshatraEndJd(now));calendar1.setText(line1);SpannableString moon=new SpannableString(" "+PanchangDisplay.moon(now));PanchangDisplay.MoonDrawable icon=new PanchangDisplay.MoonDrawable(now);int size=(int)calendar2.getTextSize();icon.setBounds(0,0,size,size);moon.setSpan(new ImageSpan(icon,ImageSpan.ALIGN_BOTTOM),0,1,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);calendar2.setText(line2+moon);festival.setText(r.festival);festival.setVisibility(r.festival.isEmpty()?View.GONE:View.VISIBLE);double rs=Panchang.rahuStartJd(c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH)),re=Panchang.rahuEndJd(c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH));{TextView rahu=calendar2;String text="  RAHU KALAM "+PanchangDisplay.time(rs)+" to "+PanchangDisplay.time(re);rahu.append(text);rahu.setTextColor(now>=rs&&now<re?0xCCD9B273:0x7799AABB);}SharedPreferences p=context.getSharedPreferences(Config.SETTINGS_NAME,Context.MODE_PRIVATE);p.edit().putLong("panchang_updated",nowMs).putString("panchang_date",c.get(Calendar.YEAR)+"-"+(c.get(Calendar.MONTH)+1)+"-"+c.get(Calendar.DAY_OF_MONTH)).putString("panchang_line1",line1).putString("panchang_line2",line2).putString("panchang_festival",r.festival).apply();handler.removeCallbacks(transitionTick);long delay=PanchangDisplay.transitionDelayMs(c,now);handler.postDelayed(transitionTick,delay);}
    private static String hindiTithi(int n,boolean krishna){String[] a={"प्रतिपदा","द्वितीया","तृतीया","चतुर्थी","पंचमी","षष्ठी","सप्तमी","अष्टमी","नवमी","दशमी","एकादशी","द्वादशी","त्रयोदशी","चतुर्दशी","पूर्णिमा"};return krishna&&n==15?"अमावस्या":a[Math.max(1,Math.min(15,n))-1];}
    private void place(){if(content==null||content.getParent()==null)return;int maxX=Math.max(0,root.getWidth()-content.getWidth()),maxY=Math.max(0,root.getHeight()-content.getHeight());float x=maxX*(0.1f+0.8f*random.nextFloat()),y=maxY*(0.1f+0.8f*random.nextFloat());content.animate().alpha(0.65f).setDuration(450).withEndAction(()->{content.setX(x);content.setY(y);if(root.getWidth()>0&&root.getHeight()>0&&ambient.getBackground() instanceof android.graphics.drawable.GradientDrawable)((android.graphics.drawable.GradientDrawable)ambient.getBackground()).setGradientCenter((x+content.getWidth()/2f)/root.getWidth(),(y+content.getHeight()/2f)/root.getHeight());content.animate().alpha(1).setDuration(700).start();}).start();}
    private void shiftGlow(){int from=color;colorIndex=(colorIndex+1)%PALETTE.length;if(glowAnimator!=null)glowAnimator.cancel();glowAnimator=ValueAnimator.ofObject(new ArgbEvaluator(),from,PALETTE[colorIndex]);glowAnimator.setDuration(8000);glowAnimator.addUpdateListener(a->setGlow((Integer)a.getAnimatedValue()));glowAnimator.start();}
    private void setGlow(int c){color=c;int rgb=c&0x00ffffff;android.graphics.drawable.Drawable d=ambient.getBackground();if(d instanceof android.graphics.drawable.GradientDrawable){android.graphics.drawable.GradientDrawable g=(android.graphics.drawable.GradientDrawable)d;g.setColors(new int[]{0x40000000|rgb,0x14000000|rgb,0x00000000|rgb});g.setGradientRadius(Math.max(1,root.getWidth())*.55f);}}
    private Bitmap decode(PhotoStore.Photo p){if(p==null)return null;File f=new File(PhotoStore.dir(context),p.file);try{BitmapFactory.Options b=new BitmapFactory.Options();b.inJustDecodeBounds=true;BitmapFactory.decodeFile(f.getAbsolutePath(),b);android.util.DisplayMetrics dm=context.getResources().getDisplayMetrics();int w=root.getWidth()>0?root.getWidth():dm.widthPixels,h=root.getHeight()>0?root.getHeight():dm.heightPixels;w=Math.max(1,w);h=Math.max(1,h);int sample=1;while(b.outWidth/sample>w*1.5||b.outHeight/sample>h*1.5)sample*=2;b.inJustDecodeBounds=false;b.inSampleSize=sample;b.inPreferredConfig=Bitmap.Config.RGB_565;return BitmapFactory.decodeFile(f.getAbsolutePath(),b);}catch(Exception ignored){return null;}}
    private void showNext(){if(photos==null||photos.isEmpty())return;int attempts=0;Bitmap decoded=null;while(attempts++<photos.size()&&decoded==null){index=(index+1)%photos.size();decoded=decode(photos.get(index));}if(decoded==null){handler.postDelayed(photoTick,60_000);return;}next=decoded;nextImage.setImageBitmap(next);nextImage.setAlpha(0);nextImage.setVisibility(View.VISIBLE);nextImage.animate().alpha(1).setDuration(1400).withEndAction(()->{photo.setImageBitmap(next);photo.setAlpha(1);nextImage.setImageDrawable(null);nextImage.setVisibility(View.GONE);if(current!=null&&!current.isRecycled())current.recycle();current=next;next=null;setCredit();}).start();long ms=context.getSharedPreferences(Config.SETTINGS_NAME,Context.MODE_PRIVATE).getLong("photo_interval_ms",60_000L);handler.postDelayed(photoTick,Math.max(30_000,ms));}
    private void setCredit(){if(photos!=null&&!photos.isEmpty()){PhotoStore.Photo p=photos.get(index);if(PhotoStore.SOURCE_BING.equals(p.source)){String caption=p.title;if(!p.copyright.isEmpty())caption+=(caption.isEmpty()?"":" · ")+p.copyright;credit.setText(caption);}else credit.setText(context.getString(R.string.photo_credit,p.author));}}
    void stop(){if(!running)return;running=false;handler.removeCallbacks(tick);handler.removeCallbacks(photoTick);handler.removeCallbacks(transitionTick);if(receiverRegistered){try{context.unregisterReceiver(timeChanges);}catch(IllegalArgumentException ignored){}receiverRegistered=false;}if(glowAnimator!=null){glowAnimator.cancel();glowAnimator=null;}if(current!=null&&!current.isRecycled())current.recycle();if(next!=null&&!next.isRecycled())next.recycle();current=next=null;photo.setImageDrawable(null);nextImage.setImageDrawable(null);}
}
