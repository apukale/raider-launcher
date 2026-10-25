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
    private final Random random=new Random(); private final View content,ambient,calendarBox; private final ImageView photo,nextImage; private final TextView calendar1,tithi,nakshatra,moon,rahu,festival,credit,weather;
    private List<PhotoStore.Photo> photos; private Bitmap current,next; private int index,colorIndex,color; private ValueAnimator glowAnimator; private boolean running,calendarEnabled; private android.widget.LinearLayout worldClockGrid;
    private final Runnable tick=new Runnable(){public void run(){place();shiftGlow();handler.postDelayed(this,MOVE);}};
    private final Runnable transitionTick=()->{updatePanchang(true);updateNight(false);};
    private final Runnable nightBoundary=()->updateNight(true);
    private boolean receiverRegistered;
    private final Runnable worldTick=new Runnable(){public void run(){updateWorldClocks();if(running)handler.postDelayed(this,30_000L);}};
    private final BroadcastReceiver timeChanges=new BroadcastReceiver(){public void onReceive(Context c,Intent i){updatePanchang(true);updateNight(true);updateWorldClocks();}};
    private final Runnable photoTick=new Runnable(){public void run(){showNext();}};
    private final Runnable weatherTick=new Runnable(){public void run(){updateWeather(true);if(running)handler.postDelayed(this,WeatherClient.REFRESH_MS);}};
    ScreensaverController(Context c,FrameLayout root){this.context=c;this.root=root;Fonts.apply(root,Fonts.DREAM);content=root.findViewById(R.id.dreamContent);ambient=root.findViewById(R.id.dreamAmbient);photo=root.findViewById(R.id.dreamPhoto);nextImage=root.findViewById(R.id.dreamPhotoNext);calendarBox=root.findViewById(R.id.calendarBox);calendar1=root.findViewById(R.id.calendarLine1);tithi=root.findViewById(R.id.calendarTithi);nakshatra=root.findViewById(R.id.calendarNakshatra);moon=root.findViewById(R.id.calendarMoon);rahu=root.findViewById(R.id.calendarRahu);festival=root.findViewById(R.id.calendarFestival);credit=root.findViewById(R.id.photoCredit);weather=root.findViewById(R.id.calendarWeather);}
    void start(){if(running)return;running=true;Config.load(context);PhotoStore.migratePhotoContent(context);SharedPreferences p=context.getSharedPreferences(Config.SETTINGS_NAME,Context.MODE_PRIVATE);boolean photoStyle=p.getBoolean("dream_photos",true),clockOnlyBox=photoStyle&&!p.getBoolean("dream_photo_calendar",true),calendarOn=LauncherPrefs.panchangOnDream(LauncherPrefs.hinduOn(p.contains(LauncherPrefs.HINDU),p.getBoolean(LauncherPrefs.HINDU,true)),CalendarPlacement.resolvedDream(p.getBoolean(CalendarPlacement.HOME,true),p.getBoolean(CalendarPlacement.DREAM,true)))&&!clockOnlyBox;calendarEnabled=calendarOn;boolean photosOn=PhotosMaster.on(context);photos=photosOn?PhotoStore.list(context,PhotoStore.chosenSource(context)):java.util.Collections.emptyList();if(photosOn&&photoStyle&&photos.isEmpty())PhotoStore.scheduleIfMissingThrottled(context);boolean usePhotos=photoStyle&&photosOn&&!photos.isEmpty();boolean useGradient=photoStyle&&!photosOn;photo.animate().cancel();nextImage.animate().cancel();photo.setAlpha(1f);nextImage.setAlpha(0f);front=photo;back=nextImage;photo.setVisibility(usePhotos||useGradient?View.VISIBLE:View.GONE);nextImage.setVisibility(View.GONE);credit.setVisibility(usePhotos?View.VISIBLE:View.GONE);ambient.setVisibility(usePhotos||useGradient?View.GONE:View.VISIBLE);if(useGradient){photo.setImageDrawable(null);photo.setBackground(PhotoGradients.drawable(random.nextInt()));}calendarBox.setVisibility(!clockOnlyBox&&(calendarOn||p.getBoolean("weather_on_home",true))?View.VISIBLE:View.GONE);if(!calendarOn){calendar1.setVisibility(View.GONE);tithi.setVisibility(View.GONE);nakshatra.setVisibility(View.GONE);moon.setVisibility(View.GONE);rahu.setVisibility(View.GONE);festival.setVisibility(View.GONE);}
        colorIndex=random.nextInt(PALETTE.length);android.graphics.drawable.GradientDrawable glow=new android.graphics.drawable.GradientDrawable();glow.setGradientType(android.graphics.drawable.GradientDrawable.RADIAL_GRADIENT);glow.setGradientRadius(Math.max(1,root.getWidth())*.55f);ambient.setBackground(glow);setGlow(PALETTE[colorIndex]);
        root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{alignBoxToClockDigits();if(content.getWidth()>0){setGlow(color);place();}});
        IntentFilter tf=new IntentFilter();tf.addAction(Intent.ACTION_TIME_CHANGED);tf.addAction(Intent.ACTION_TIMEZONE_CHANGED);tf.addAction(Intent.ACTION_DATE_CHANGED);if(android.os.Build.VERSION.SDK_INT>=33)context.registerReceiver(timeChanges,tf,Context.RECEIVER_NOT_EXPORTED);else context.registerReceiver(timeChanges,tf);receiverRegistered=true;if(calendarOn)updatePanchang(true);updateWeather(true);handler.postDelayed(weatherTick,WeatherClient.REFRESH_MS);updateNight(true);place();if(usePhotos){startPhotos(p);long mins=p.getLong("photo_interval_ms",60_000L);handler.postDelayed(photoTick,Math.max(30_000,mins));}
        handler.postDelayed(tick,MOVE);
        updateWorldClocks();handler.postDelayed(worldTick,30_000L);
    }
    /** Two cities per row. Each city is one line (small label, slightly larger time) so four clocks are two rows, not four. India is not included. */
    private void updateWorldClocks() {
        SharedPreferences p = context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        boolean show = p.getBoolean(WorldClocks.DREAM, false);
        if (worldClockGrid == null) {
            android.widget.LinearLayout left = root.findViewById(R.id.dreamLeft);
            if (left == null) return;
            worldClockGrid = dreamCityGrid();
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(4);
            left.addView(worldClockGrid, lp);
        }
        if (!show) { worldClockGrid.setVisibility(View.GONE); return; }
        java.util.List<WorldClocks.Clock> cities = WorldClocks.cities(p::getString);
        long now = System.currentTimeMillis();
        for (int row = 0; row < worldClockGrid.getChildCount(); row++) {
            android.widget.LinearLayout line = (android.widget.LinearLayout) worldClockGrid.getChildAt(row);
            boolean any = false;
            for (int col = 0; col < line.getChildCount(); col++) {
                android.widget.LinearLayout cell = (android.widget.LinearLayout) line.getChildAt(col);
                int index = row * 2 + col;
                if (index >= cities.size()) { cell.setVisibility(View.GONE); continue; }
                WorldClocks.Clock clock = cities.get(index);
                TextView label = (TextView) cell.getChildAt(0);
                TextView time = (TextView) cell.getChildAt(1);
                label.setText(clock.label);
                time.setText(WorldClocks.formatTime(clock.zone, now));
                cell.setVisibility(View.VISIBLE);
                any = true;
            }
            line.setVisibility(any ? View.VISIBLE : View.GONE);
        }
        worldClockGrid.setVisibility(cities.isEmpty() ? View.GONE : View.VISIBLE);
    }
    private android.widget.LinearLayout dreamCityGrid() {
        android.widget.LinearLayout grid = new android.widget.LinearLayout(context);
        grid.setOrientation(android.widget.LinearLayout.VERTICAL);
        grid.setGravity(Gravity.CENTER_HORIZONTAL);
        for (int row = 0; row < 2; row++) {
            android.widget.LinearLayout line = new android.widget.LinearLayout(context);
            line.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            line.setGravity(Gravity.CENTER);
            android.widget.LinearLayout.LayoutParams lineLp = new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            if (row > 0) lineLp.topMargin = dp(1);
            grid.addView(line, lineLp);
            for (int col = 0; col < 2; col++) {
                android.widget.LinearLayout cell = new android.widget.LinearLayout(context);
                cell.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                cell.setGravity(Gravity.CENTER_VERTICAL);
                TextView label = dreamCityText(12, 0x99FFFFFF, Fonts.of(context, Fonts.DREAM, R.font.outfit_regular));
                label.setMaxWidth(dp(108));
                label.setEllipsize(android.text.TextUtils.TruncateAt.END);
                TextView time = dreamCityText(14, 0xE6FFFFFF, Fonts.of(context, Fonts.DREAM, R.font.outfit_medium));
                cell.addView(label);
                android.widget.LinearLayout.LayoutParams timeLp = new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                timeLp.setMarginStart(dp(4));
                cell.addView(time, timeLp);
                android.widget.LinearLayout.LayoutParams cellLp = new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                if (col > 0) cellLp.setMarginStart(dp(14));
                line.addView(cell, cellLp);
            }
        }
        return grid;
    }
    private TextView dreamCityText(int sp, int color, android.graphics.Typeface face) {
        TextView text = new TextView(context);
        text.setTextSize(sp);
        text.setTextColor(color);
        text.setTypeface(face);
        text.setIncludeFontPadding(false);
        text.setSingleLine(true);
        text.setMinHeight(0);
        text.setMinimumHeight(0);
        text.setPadding(0, 0, 0, 0);
        text.setLineSpacing(0f, 1f);
        return text;
    }
    private void updateWeather(boolean refresh){SharedPreferences p=context.getSharedPreferences(Config.SETTINGS_NAME,Context.MODE_PRIVATE);boolean enabled=p.getBoolean("weather_on_home",true);WeatherData d=WeatherClient.cached(context);if(d!=null){String label=d.temperature+"\u00B0 \u00B7 "+d.high+"\u00B0/"+d.low+"\u00B0"+WeatherData.rainSuffix(d.precipitation);weather.setText(label);int size=(int)(weather.getTextSize()*1.25f);WeatherIconDrawable icon=new WeatherIconDrawable(WeatherData.iconGroup(d.code),false,0xE6FFFFFF);icon.setBounds(0,0,size,size);weather.setCompoundDrawables(icon,null,null,null);weather.setCompoundDrawablePadding(dp(6));weather.setVisibility(p.getBoolean("weather_on_home",true)?View.VISIBLE:View.GONE);}if(enabled&&refresh&&(WeatherClient.fetchedAt(context)==0||System.currentTimeMillis()-WeatherClient.fetchedAt(context)>=WeatherClient.REFRESH_MS))WeatherClient.fetch(context,Config.load(context),data->handler.post(()->{if(running&&data!=null)updateWeather(false);}));}
    private void updatePanchang(boolean force){if(!calendarEnabled)return;long nowMs=System.currentTimeMillis();double now=PanchangDisplay.jdNow();Calendar c=Calendar.getInstance();Panchang.Result r=PanchangDisplay.day(c);String line1=r.month+" "+Panchang.currentPaksha(now);String tithiText=Panchang.currentTithiName(now)+" till "+PanchangDisplay.time(Panchang.tithiEndJd(now));String nakshatraText=Panchang.currentNakshatra(now)+" till "+PanchangDisplay.time(Panchang.nakshatraEndJd(now));String moonText=PanchangDisplay.moon(now);calendar1.setText(line1);tithi.setText(tithiText);nakshatra.setText(nakshatraText);moon.setText(moonText);PanchangDisplay.MoonDrawable icon=new PanchangDisplay.MoonDrawable(now);int size=Math.max(1,(int)(moon.getTextSize()*1.3f));icon.setBounds(0,0,size,size);moon.setCompoundDrawables(icon,null,null,null);moon.setCompoundDrawablePadding((int)(8*context.getResources().getDisplayMetrics().density));moon.setVisibility(View.VISIBLE);festival.setText(r.festival);festival.setVisibility(r.festival.isEmpty()?View.GONE:View.VISIBLE);double rs=Panchang.rahuStartJd(c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH)),re=Panchang.rahuEndJd(c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH));rahu.setText("Rahu Kalam "+PanchangDisplay.time(rs)+" to "+PanchangDisplay.time(re));rahu.setTextColor(now>=rs&&now<re?0xCCD9B273:(now>=re?0x7799AABB:0xE6FFFFFF));SharedPreferences p=context.getSharedPreferences(Config.SETTINGS_NAME,Context.MODE_PRIVATE);p.edit().putLong("panchang_updated",nowMs).putString("panchang_date",c.get(Calendar.YEAR)+"-"+(c.get(Calendar.MONTH)+1)+"-"+c.get(Calendar.DAY_OF_MONTH)).putString("panchang_line1",line1).putString("panchang_line2",tithiText).putString("panchang_festival",r.festival).apply();handler.removeCallbacks(transitionTick);long delay=PanchangDisplay.transitionDelayMs(c,now);handler.postDelayed(transitionTick,delay);}
    private static String hindiTithi(int n,boolean krishna){String[] a={"प्रतिपदा","द्वितीया","तृतीया","चतुर्थी","पंचमी","षष्ठी","सप्तमी","अष्टमी","नवमी","दशमी","एकादशी","द्वादशी","त्रयोदशी","चतुर्दशी","पूर्णिमा"};return krishna&&n==15?"अमावस्या":a[Math.max(1,Math.min(15,n))-1];}
    private void place(){if(content==null||content.getParent()==null||content.getWidth()==0||root.getWidth()==0||root.getHeight()==0)return;int maxX=Math.max(0,root.getWidth()-content.getWidth());int maxY=Math.max(0,root.getHeight()-content.getHeight());if(credit.getVisibility()==View.VISIBLE&&credit.getHeight()>0){int captionTop=root.getHeight()-credit.getHeight()-dp(24);maxY=Math.min(maxY,Math.max(0,captionTop-dp(16)-content.getHeight()));}float x=maxX*(0.1f+0.8f*random.nextFloat()),y=maxY*(0.1f+0.8f*random.nextFloat());content.animate().alpha(0.65f).setDuration(450).withEndAction(()->{content.setX(x);content.setY(y);if(ambient.getBackground() instanceof android.graphics.drawable.GradientDrawable)((android.graphics.drawable.GradientDrawable)ambient.getBackground()).setGradientCenter((x+content.getWidth()/2f)/root.getWidth(),(y+content.getHeight()/2f)/root.getHeight());content.animate().alpha(1).setDuration(700).start();}).start();}
    private int dp(float value){return (int)(value*context.getResources().getDisplayMetrics().density+0.5f);}
    private void shiftGlow(){int from=color;colorIndex=(colorIndex+1)%PALETTE.length;if(glowAnimator!=null)glowAnimator.cancel();glowAnimator=ValueAnimator.ofObject(new ArgbEvaluator(),from,PALETTE[colorIndex]);glowAnimator.setDuration(8000);glowAnimator.addUpdateListener(a->setGlow((Integer)a.getAnimatedValue()));glowAnimator.start();}
    private void setGlow(int c){color=c;int rgb=c&0x00ffffff;android.graphics.drawable.Drawable d=ambient.getBackground();if(d instanceof android.graphics.drawable.GradientDrawable){android.graphics.drawable.GradientDrawable g=(android.graphics.drawable.GradientDrawable)d;g.setColors(new int[]{0x40000000|rgb,0x14000000|rgb,0x00000000|rgb});g.setGradientRadius(Math.max(1,root.getWidth())*.55f);}}
    private Bitmap decode(PhotoStore.Photo p){if(p==null)return null;File f=new File(PhotoStore.dir(context),p.file);try{BitmapFactory.Options b=new BitmapFactory.Options();b.inJustDecodeBounds=true;BitmapFactory.decodeFile(f.getAbsolutePath(),b);android.util.DisplayMetrics dm=context.getResources().getDisplayMetrics();int w=root.getWidth()>0?root.getWidth():dm.widthPixels,h=root.getHeight()>0?root.getHeight():dm.heightPixels;w=Math.max(1,w);h=Math.max(1,h);int sample=1;while(b.outWidth/sample>w*1.5||b.outHeight/sample>h*1.5)sample*=2;b.inJustDecodeBounds=false;b.inSampleSize=sample;b.inPreferredConfig=Bitmap.Config.RGB_565;return BitmapFactory.decodeFile(f.getAbsolutePath(),b);}catch(Exception ignored){return null;}}
    private static final String PHOTO_TAG="TvHomePhotos";
    private int[] order=new int[0]; private int pos; private ImageView front,back;
    /** Shuffled first photo; never the same first photo as the previous start when there is a choice. */
    private void startPhotos(SharedPreferences p){java.util.ArrayList<String> keys=new java.util.ArrayList<>();for(PhotoStore.Photo ph:photos)keys.add(ph.file);int avoid=PhotoShuffle.indexOf(keys,p.getString("dream_last_first",""));order=PhotoShuffle.order(photos.size(),avoid,random);pos=-1;front=photo;back=nextImage;Bitmap first=null;int tries=0,max=Math.min(photos.size(),8);while(first==null&&tries++<max&&pos+1<order.length){pos++;index=order[pos];first=decode(photos.get(index));if(first==null)android.util.Log.w(PHOTO_TAG,"decode failed: "+photos.get(index).file);}if(first==null){photo.setVisibility(View.GONE);ambient.setVisibility(View.VISIBLE);credit.setVisibility(View.GONE);return;}p.edit().putString("dream_last_first",photos.get(index).file).apply();current=first;front.setImageBitmap(first);front.setAlpha(1f);front.setVisibility(View.VISIBLE);setCredit();}
    /** Cross-fades the back view in and the front view out, then swaps roles. Bitmaps are never moved between views and never recycled while attached. */
    private void showNext(){if(!running||photos==null||photos.isEmpty())return;long ms=context.getSharedPreferences(Config.SETTINGS_NAME,Context.MODE_PRIVATE).getLong("photo_interval_ms",60_000L);handler.postDelayed(photoTick,Math.max(30_000,ms));if(photos.size()<2&&current!=null)return;Bitmap decoded=null;int cand=index,tries=0,max=Math.min(photos.size(),8);while(decoded==null&&tries++<max){pos++;if(pos>=order.length){order=PhotoShuffle.order(photos.size(),index,random);pos=0;}cand=order[pos];if(cand==index&&photos.size()>1)continue;decoded=decode(photos.get(cand));if(decoded==null)android.util.Log.w(PHOTO_TAG,"decode failed: "+photos.get(cand).file);}if(decoded==null)return;if(front==null||back==null){front=photo;back=nextImage;}final ImageView in=back,out=front;in.animate().cancel();out.animate().cancel();in.setImageBitmap(decoded);in.setAlpha(0f);in.setVisibility(View.VISIBLE);in.animate().alpha(1f).setDuration(1400).start();out.animate().alpha(0f).setDuration(1400).withEndAction(()->{if(out!=front){out.setVisibility(View.GONE);out.setImageDrawable(null);}}).start();front=in;back=out;current=decoded;next=null;index=cand;setCredit();}
    private void setCredit(){if(photos!=null&&!photos.isEmpty()){PhotoStore.Photo p=photos.get(index);if(PhotoStore.SOURCE_BING.equals(p.source)){String caption=p.title;if(!p.copyright.isEmpty())caption+=(caption.isEmpty()?"":" · ")+p.copyright;credit.setText(caption);}else if(PhotoStore.SOURCE_UNSPLASH.equals(p.source))credit.setText("Photo by "+p.author+" on Unsplash");else{String cap=p.title==null?"":p.title;if(p.copyright!=null&&!p.copyright.isEmpty())cap+=(cap.isEmpty()?"":"  \u00B7  ")+p.copyright;credit.setText(cap);}}}
    private void updateNight(boolean schedule){handler.removeCallbacks(nightBoundary);SharedPreferences p=context.getSharedPreferences(Config.SETTINGS_NAME,Context.MODE_PRIVATE);long nowMs=System.currentTimeMillis();boolean night=NightState.isActive(p,nowMs);NightTheme.applyScreensaver(root,photo,nextImage,night);NightEffects.apply(context,root,night);long boundary=NightState.nextChangeMillis(p,nowMs);if(schedule&&boundary!=Long.MAX_VALUE){handler.postDelayed(nightBoundary,Math.max(0L,boundary-nowMs+500L));}}
    void stop(){if(!running)return;running=false;handler.removeCallbacks(tick);handler.removeCallbacks(photoTick);handler.removeCallbacks(transitionTick);handler.removeCallbacks(nightBoundary);handler.removeCallbacks(weatherTick);handler.removeCallbacks(worldTick);NightTheme.applyScreensaver(root,photo,nextImage,false);NightEffects.dim(root,false);if(receiverRegistered){try{context.unregisterReceiver(timeChanges);}catch(IllegalArgumentException ignored){}receiverRegistered=false;}if(glowAnimator!=null){glowAnimator.cancel();glowAnimator=null;}photo.animate().cancel();nextImage.animate().cancel();current=next=null;photo.setImageDrawable(null);nextImage.setImageDrawable(null);photo.setAlpha(1f);nextImage.setAlpha(0f);nextImage.setVisibility(View.GONE);front=photo;back=nextImage;}

    /** Top-aligns the calendar box with the visual top of the clock digits (not the TextView bounds). */
    private int lastBoxMargin=-1;
    /** Trim the empty font space above/below the clock digits and the date's capitals so the left column hugs its glyphs.
     *  (setFirstBaselineToTopHeight can only add space, never remove it below the font ascent, so negative margins are used.) */
    private void tightenLeftColumn(){TextView clock=root.findViewById(R.id.dreamClock),date=root.findViewById(R.id.dreamDate);if(clock==null||date==null)return;float dens=context.getResources().getDisplayMetrics().density;android.graphics.Rect r=new android.graphics.Rect();clock.getPaint().getTextBounds("0123456789",0,10,r);android.graphics.Paint.FontMetricsInt fm=clock.getPaint().getFontMetricsInt();int clockTop=Math.max(0,-fm.ascent+r.top),clockBottom=Math.max(0,fm.descent-r.bottom);String probe="0123456789ABCDEFGHIKLMNOPRSTUVWXYZ";date.getPaint().getTextBounds(probe,0,probe.length(),r);fm=date.getPaint().getFontMetricsInt();int dateTop=Math.max(0,-fm.ascent+r.top),dateBottom=Math.max(0,fm.descent-r.bottom);setVerticalMargins(clock,-clockTop,-clockBottom);setVerticalMargins(date,Math.round(12*dens)-dateTop,-dateBottom);}
    private static void setVerticalMargins(View v,int top,int bottom){android.view.ViewGroup.MarginLayoutParams lp=(android.view.ViewGroup.MarginLayoutParams)v.getLayoutParams();if(lp.topMargin==top&&lp.bottomMargin==bottom)return;lp.topMargin=top;lp.bottomMargin=bottom;v.setLayoutParams(lp);}
    private void alignBoxToClockDigits(){tightenLeftColumn();View divider=root.findViewById(R.id.dreamDivider);if(divider!=null&&calendarBox!=null){int dv=calendarBox.getVisibility()==View.VISIBLE?View.VISIBLE:View.GONE;if(divider.getVisibility()!=dv)divider.setVisibility(dv);}if(calendarBox!=null){android.view.ViewGroup.MarginLayoutParams lp=(android.view.ViewGroup.MarginLayoutParams)calendarBox.getLayoutParams();if(lp.topMargin!=0){lp.topMargin=0;calendarBox.setLayoutParams(lp);}}}
}
