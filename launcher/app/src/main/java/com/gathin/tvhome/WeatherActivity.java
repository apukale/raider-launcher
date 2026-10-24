package com.gathin.tvhome;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/** Weather detail screen: today, hourly strip and a 7-day toggle over a dimmed, cached map of India.
 *  Forecast from Open-Meteo, map tiles from OpenStreetMap (darkened on the TV), rain radar from RainViewer. All free, no key. */
public class WeatherActivity extends Activity {
    private static final String TAG="TvHomeWeather";
    private static final int TILE=256;
    private static final long CACHE_MS=30L*60*1000;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final Handler ui=new Handler(Looper.getMainLooper());
    private volatile boolean destroyed;
    private FrameLayout root; private ImageView mapView; private Bitmap map;
    private LinearLayout todayPane, weekPane; private TextView subtitle, radarNote; private Button toggle;
    private android.widget.ImageButton back; private View headerBar; private TextView attributionView; private boolean week; private int accent; private double lat, lon; private String place; private boolean fahrenheit;

    @Override protected void onCreate(Bundle b){super.onCreate(b);Config cfg=Config.load(this);lat=WeatherLocation.lat(this,cfg);lon=WeatherLocation.lon(this,cfg);place=WeatherLocation.place(this,cfg);if(place==null)place="";fahrenheit=WeatherLocation.fahrenheit(this);accent=RaiderTheme.accent(this);build();
        NightEffects.dim(this,root,NightState.isActive(getSharedPreferences(Config.SETTINGS_NAME,MODE_PRIVATE),System.currentTimeMillis()));toggle.requestFocus();
        if(!WeatherLocation.valid(lat,lon)){subtitle.setText("Set city in Launcher settings > Weather");}else{String cached=cachedJson();if(cached!=null)render(cached,false);io.execute(this::loadForecast);}
        root.post(()->{android.graphics.Rect fr=freeRect();int w=root.getWidth(),h=root.getHeight();io.execute(()->loadMap(fr,w,h));});}
    @Override protected void onDestroy(){destroyed=true;io.shutdownNow();if(mapView!=null)mapView.setImageDrawable(null);if(map!=null){map.recycle();map=null;}super.onDestroy();}

    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private TextView text(CharSequence s,float sp,int font,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTypeface(Fonts.of(this,Fonts.PANCHANG,font));t.setTextColor(color);t.setIncludeFontPadding(false);t.setSingleLine(false);return t;}
    private GradientDrawable panel(){GradientDrawable d=new GradientDrawable();d.setColor(0xB81C1E3F);d.setCornerRadius(dp(16));d.setStroke(dp(1),0x22FFFFFF);return d;}
    private String deg(int v){return v+"\u00B0";}
    private String unit(){return fahrenheit?"\u00B0F":"\u00B0C";}

    /** One screen (960x540 dp): 48 dp sides, 27 dp top/bottom. Cards on the left 560 dp; the map shows through on the right. */
    private void build(){root=new FrameLayout(this);root.setBackgroundColor(0xFF1A1A1A);setContentView(root);
        mapView=new ImageView(this);mapView.setScaleType(ImageView.ScaleType.FIT_XY);root.addView(mapView,new FrameLayout.LayoutParams(-1,-1));
        radarNote=text("",13,R.font.outfit_light,0xD9FFFFFF);radarNote.setShadowLayer(4,0,1,0xFF000000);FrameLayout.LayoutParams rn=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.END);rn.setMargins(0,dp(96),dp(48),0);root.addView(radarNote,rn);
        LinearLayout col=new LinearLayout(this);col.setOrientation(LinearLayout.VERTICAL);col.setPadding(dp(48),dp(27),dp(48),dp(27));root.addView(col,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout header=new LinearLayout(this);headerBar=header;header.setOrientation(LinearLayout.HORIZONTAL);header.setGravity(Gravity.CENTER_VERTICAL);
        back=new android.widget.ImageButton(this);back.setId(View.generateViewId());back.setImageResource(R.drawable.ic_back);back.setContentDescription("Back");back.setBackground(RaiderTheme.focusBackground(this,accent,false));RaiderTheme.applyFocusAnimation(back);back.setImageTintList(RaiderTheme.accentStateList(accent));back.setScaleType(ImageView.ScaleType.CENTER);back.setFocusable(true);back.setFocusableInTouchMode(false);back.setOnClickListener(v->finish());LinearLayout.LayoutParams bk=new LinearLayout.LayoutParams(dp(52),dp(44));bk.setMarginEnd(dp(16));header.addView(back,bk);
        LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);titles.addView(shadow(text("Weather",28,R.font.outfit_medium,0xFFF4F1EA)));subtitle=text(place.isEmpty()?"Loading\u2026":place+" \u00B7 loading\u2026",15,R.font.outfit_light,0xFFC9CBE0);shadow(subtitle);LinearLayout.LayoutParams sl=new LinearLayout.LayoutParams(-2,-2);sl.topMargin=dp(4);titles.addView(subtitle,sl);header.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        toggle=new Button(this);toggle.setText("7-day forecast");toggle.setAllCaps(false);toggle.setTextSize(16);toggle.setTypeface(Fonts.of(this,Fonts.PANCHANG,R.font.outfit_medium));toggle.setIncludeFontPadding(false);toggle.setTextColor(0xFFF4F1EA);toggle.setStateListAnimator(null);toggle.setMinHeight(0);toggle.setMinimumHeight(0);toggle.setPadding(dp(20),0,dp(20),0);toggle.setBackground(RaiderTheme.focusBackground(this,accent,false));toggle.setFocusable(true);RaiderTheme.applyFocusAnimation(toggle);toggle.setOnClickListener(v->{week=!week;showPane();});header.addView(toggle,new LinearLayout.LayoutParams(-2,dp(44)));toggle.setId(View.generateViewId());toggle.setNextFocusLeftId(back.getId());toggle.setNextFocusUpId(back.getId());back.setNextFocusRightId(toggle.getId());back.setNextFocusDownId(toggle.getId());back.setNextFocusUpId(back.getId());back.setNextFocusLeftId(back.getId());col.addView(header);
        FrameLayout body=new FrameLayout(this);LinearLayout.LayoutParams bl=new LinearLayout.LayoutParams(dp(560),0,1);bl.topMargin=dp(16);col.addView(body,bl);
        todayPane=new LinearLayout(this);todayPane.setOrientation(LinearLayout.VERTICAL);body.addView(todayPane,new FrameLayout.LayoutParams(-1,-1));
        weekPane=new LinearLayout(this);weekPane.setOrientation(LinearLayout.VERTICAL);weekPane.setVisibility(View.GONE);body.addView(weekPane,new FrameLayout.LayoutParams(-1,-1));
        TextView attribution=attributionView=shadow(text("Forecast: Open-Meteo.com \u00B7 Map: \u00A9 OpenStreetMap contributors \u00B7 Radar: RainViewer.com",13,R.font.outfit_light,0xCCFFFFFF));LinearLayout.LayoutParams al=new LinearLayout.LayoutParams(-2,-2);al.topMargin=dp(8);col.addView(attribution,al);}
    private void showPane(){todayPane.setVisibility(week?View.GONE:View.VISIBLE);weekPane.setVisibility(week?View.VISIBLE:View.GONE);toggle.setText(week?"Today":"7-day forecast");}

    // ---- forecast ----
    private String key(){return String.format(Locale.US,"%.4f,%.4f,%s",lat,lon,fahrenheit?"f":"c");}
    private SharedPreferences prefs(){return getSharedPreferences(Config.SETTINGS_NAME,MODE_PRIVATE);}
    private String cachedJson(){SharedPreferences p=prefs();if(!key().equals(p.getString("weather_detail_key","")))return null;return p.getString("weather_detail_json",null);}
    private void loadForecast(){SharedPreferences p=prefs();long at=p.getLong("weather_detail_at",0),age=System.currentTimeMillis()-at;if(cachedJson()!=null&&age>=0&&age<CACHE_MS)return;
        String url="https://api.open-meteo.com/v1/forecast?latitude="+lat+"&longitude="+lon+(fahrenheit?"&temperature_unit=fahrenheit&wind_speed_unit=mph":"")
            +"&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m,is_day"
            +"&hourly=temperature_2m,weather_code,precipitation_probability,is_day&forecast_hours=24"
            +"&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum,sunrise,sunset&forecast_days=7&timezone=auto";
        try{String json=new String(get(url,262144),"UTF-8");new JSONObject(json);p.edit().putString("weather_detail_json",json).putString("weather_detail_key",key()).putLong("weather_detail_at",System.currentTimeMillis()).apply();ui.post(()->{if(!destroyed)render(json,true);});Log.i(TAG,"detail forecast ok");}
        catch(Exception e){Log.w(TAG,"detail forecast failed "+e);ui.post(()->{if(destroyed)return;if(cachedJson()==null)subtitle.setText((place.isEmpty()?"":place+" \u00B7 ")+"forecast unavailable, try again later");});}}

    private void render(String json,boolean fresh){try{JSONObject o=new JSONObject(json),cur=o.getJSONObject("current"),d=o.getJSONObject("daily"),h=o.getJSONObject("hourly");
        java.text.SimpleDateFormat hm=new java.text.SimpleDateFormat("h:mm a",Locale.getDefault());String upd=hm.format(new java.util.Date(prefs().getLong("weather_detail_at",System.currentTimeMillis())));
        subtitle.setText((place.isEmpty()?"":place+" \u00B7 ")+"updated "+upd);
        int code=cur.optInt("weather_code",0);boolean night=cur.optInt("is_day",1)==0;
        todayPane.removeAllViews();
        LinearLayout now=new LinearLayout(this);now.setOrientation(LinearLayout.HORIZONTAL);now.setGravity(Gravity.CENTER_VERTICAL);now.setBackground(panel());now.setPadding(dp(20),dp(14),dp(20),dp(14));
        ImageView icon=new ImageView(this);icon.setImageDrawable(new WeatherIconDrawable(WeatherData.iconGroup(code),night,0xFFF4F1EA));now.addView(icon,new LinearLayout.LayoutParams(dp(64),dp(64)));
        TextView temp=text(Math.round(cur.optDouble("temperature_2m"))+unit(),52,R.font.outfit_light,0xFFFFFFFF);LinearLayout.LayoutParams tl=new LinearLayout.LayoutParams(-2,-2);tl.setMarginStart(dp(16));now.addView(temp,tl);
        LinearLayout nowText=new LinearLayout(this);nowText.setOrientation(LinearLayout.VERTICAL);nowText.addView(text(condition(code),20,R.font.outfit_medium,0xFFF4F1EA));TextView feels=text("Feels like "+deg((int)Math.round(cur.optDouble("apparent_temperature")))+" \u00B7 High "+deg((int)Math.round(d.getJSONArray("temperature_2m_max").optDouble(0)))+" \u00B7 Low "+deg((int)Math.round(d.getJSONArray("temperature_2m_min").optDouble(0))),15,R.font.outfit_light,0xFFA9ABC7);LinearLayout.LayoutParams fl=new LinearLayout.LayoutParams(-2,-2);fl.topMargin=dp(6);nowText.addView(feels,fl);
        LinearLayout.LayoutParams ntl=new LinearLayout.LayoutParams(0,-2,1);ntl.setMarginStart(dp(20));now.addView(nowText,ntl);todayPane.addView(now,new LinearLayout.LayoutParams(-1,-2));
        String windUnit=fahrenheit?"mph":"km/h";
        String[][] cells={{"Rain chance",d.getJSONArray("precipitation_probability_max").optInt(0)+"%"},{"Wind",Math.round(cur.optDouble("wind_speed_10m"))+" "+windUnit+" "+compass(cur.optDouble("wind_direction_10m"))},{"Humidity",cur.optInt("relative_humidity_2m")+"%"},
            {"Sunrise",clock(d.getJSONArray("sunrise").optString(0))},{"Sunset",clock(d.getJSONArray("sunset").optString(0))},{"Rain today",String.format(Locale.getDefault(),"%.1f %s",d.getJSONArray("precipitation_sum").optDouble(0),fahrenheit?"in":"mm")}};
        for(int r=0;r<2;r++){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);for(int c=0;c<3;c++){String[] cell=cells[r*3+c];LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setBackground(panel());card.setPadding(dp(16),dp(10),dp(16),dp(10));card.addView(text(cell[0],14,R.font.outfit_medium,accent));LinearLayout.LayoutParams vl=new LinearLayout.LayoutParams(-2,-2);vl.topMargin=dp(6);card.addView(text(cell[1],20,R.font.outfit_regular,0xFFF4F1EA),vl);LinearLayout.LayoutParams cl=new LinearLayout.LayoutParams(0,-2,1);if(c>0)cl.setMarginStart(dp(12));row.addView(card,cl);}
            LinearLayout.LayoutParams rl=new LinearLayout.LayoutParams(-1,-2);rl.topMargin=dp(12);todayPane.addView(row,rl);}
        LinearLayout strip=new LinearLayout(this);strip.setOrientation(LinearLayout.HORIZONTAL);strip.setBackground(panel());strip.setPadding(dp(8),dp(12),dp(8),dp(12));
        JSONArray ht=h.getJSONArray("time"),htemp=h.getJSONArray("temperature_2m"),hcode=h.getJSONArray("weather_code"),hrain=h.optJSONArray("precipitation_probability"),hday=h.optJSONArray("is_day");
        int n=Math.min(8,ht.length());for(int i=0;i<n;i++){int idx=Math.min(ht.length()-1,i*2);LinearLayout cell=new LinearLayout(this);cell.setOrientation(LinearLayout.VERTICAL);cell.setGravity(Gravity.CENTER_HORIZONTAL);
            TextView t=text(i==0?"Now":hour(ht.optString(idx)),14,R.font.outfit_regular,0xFFA9ABC7);t.setGravity(Gravity.CENTER);cell.addView(t);ImageView hi=new ImageView(this);hi.setImageDrawable(new WeatherIconDrawable(WeatherData.iconGroup(hcode.optInt(idx)),hday!=null&&hday.optInt(idx,1)==0,0xE6FFFFFF));LinearLayout.LayoutParams il=new LinearLayout.LayoutParams(dp(28),dp(28));il.topMargin=dp(6);cell.addView(hi,il);
            TextView tv=text(deg((int)Math.round(htemp.optDouble(idx))),18,R.font.outfit_medium,0xFFF4F1EA);LinearLayout.LayoutParams vl=new LinearLayout.LayoutParams(-2,-2);vl.topMargin=dp(6);cell.addView(tv,vl);TextView rv=text((hrain!=null?hrain.optInt(idx):0)+"%",13,R.font.outfit_light,0xFF8FB8E8);LinearLayout.LayoutParams rvl=new LinearLayout.LayoutParams(-2,-2);rvl.topMargin=dp(4);cell.addView(rv,rvl);
            strip.addView(cell,new LinearLayout.LayoutParams(0,-2,1));}
        LinearLayout.LayoutParams stl=new LinearLayout.LayoutParams(-1,-2);stl.topMargin=dp(12);todayPane.addView(strip,stl);
        TextView every=text("Next 16 hours, every 2 hours",13,R.font.outfit_light,0x99FFFFFF);LinearLayout.LayoutParams el=new LinearLayout.LayoutParams(-2,-2);el.topMargin=dp(6);el.setMarginStart(dp(4));todayPane.addView(every,el);
        weekPane.removeAllViews();LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setBackground(panel());list.setPadding(dp(20),dp(8),dp(20),dp(8));
        JSONArray dt=d.getJSONArray("time"),dcode=d.getJSONArray("weather_code"),dmax=d.getJSONArray("temperature_2m_max"),dmin=d.getJSONArray("temperature_2m_min"),drain=d.getJSONArray("precipitation_probability_max");
        for(int i=0;i<Math.min(7,dt.length());i++){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(text(i==0?"Today":dayName(dt.optString(i)),17,R.font.outfit_medium,i==0?accent:0xFFF4F1EA),new LinearLayout.LayoutParams(dp(110),-2));
            ImageView di=new ImageView(this);di.setImageDrawable(new WeatherIconDrawable(WeatherData.iconGroup(dcode.optInt(i)),false,0xE6FFFFFF));row.addView(di,new LinearLayout.LayoutParams(dp(30),dp(30)));
            LinearLayout.LayoutParams cl=new LinearLayout.LayoutParams(0,-2,1);cl.setMarginStart(dp(14));row.addView(text(condition(dcode.optInt(i)),16,R.font.outfit_light,0xFFA9ABC7),cl);
            TextView rain=text(drain.optInt(i)+"%",15,R.font.outfit_regular,0xFF8FB8E8);rain.setGravity(Gravity.END);row.addView(rain,new LinearLayout.LayoutParams(dp(60),-2));
            TextView hl=text(deg((int)Math.round(dmax.optDouble(i)))+" / "+deg((int)Math.round(dmin.optDouble(i))),17,R.font.outfit_medium,0xFFF4F1EA);hl.setGravity(Gravity.END);row.addView(hl,new LinearLayout.LayoutParams(dp(100),-2));
            list.addView(row,new LinearLayout.LayoutParams(-1,dp(50)));}
        weekPane.addView(list,new LinearLayout.LayoutParams(-1,-2));
    }catch(Exception e){Log.w(TAG,"detail render failed "+e);if(fresh)subtitle.setText((place.isEmpty()?"":place+" \u00B7 ")+"forecast unavailable");}}

    static String condition(int c){if(c==0)return "Clear";if(c==1)return "Mostly clear";if(c==2)return "Partly cloudy";if(c==3)return "Overcast";if(c==45||c==48)return "Fog";if(c>=51&&c<=57)return "Drizzle";if(c>=61&&c<=65)return c==65?"Heavy rain":c==63?"Rain":"Light rain";if(c==66||c==67)return "Freezing rain";if(c>=71&&c<=77)return "Snow";if(c>=80&&c<=82)return "Showers";if(c==85||c==86)return "Snow showers";if(c>=95)return "Thunderstorm";return "Cloudy";}
    static String compass(double deg){if(Double.isNaN(deg))return "";String[] d={"N","NE","E","SE","S","SW","W","NW"};return d[(int)Math.round(((deg%360)+360)%360/45.0)%8];}
    private String clock(String iso){try{int t=iso.indexOf('T');String[] p=iso.substring(t+1).split(":");Calendar c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,Integer.parseInt(p[0]));c.set(Calendar.MINUTE,Integer.parseInt(p[1]));return new java.text.SimpleDateFormat("h:mm a",Locale.getDefault()).format(c.getTime());}catch(Exception e){return "-";}}
    private String hour(String iso){try{int t=iso.indexOf('T');int hr=Integer.parseInt(iso.substring(t+1,t+3));Calendar c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,hr);c.set(Calendar.MINUTE,0);return new java.text.SimpleDateFormat("h a",Locale.getDefault()).format(c.getTime());}catch(Exception e){return "";}}
    private String dayName(String date){try{String[] p=date.split("-");Calendar c=Calendar.getInstance();c.clear();c.set(Integer.parseInt(p[0]),Integer.parseInt(p[1])-1,Integer.parseInt(p[2]));return new java.text.SimpleDateFormat("EEE d",Locale.getDefault()).format(c.getTime());}catch(Exception e){return date;}}

    // ---- map + radar (2.11.15: India fitted into the free area right of the cards, crisp tiles, light dark-mode filter, highlighted state borders) ----
    private static final double K=0.92, IN_N=35.5, IN_S=8.0, IN_W=68.1, IN_E=97.4;
    /** OSM standard tile to a clear dark map: invert + hue-rotate 180 (keeps water blue), 30% desaturated; purple admin borders turned light lavender. */
    static int darkPixel(int c){int r=(c>>16)&0xFF,g=(c>>8)&0xFF,b=c&0xFF;if(r-g>12&&b-g>8&&r<235)return 0xFFE8C4FF;
        double or=K*255-K*(-0.574*r+1.43*g+0.144*b)+4,og=K*255-K*(0.426*r+0.43*g+0.144*b)+6,ob=K*255-K*(0.426*r+1.43*g-0.856*b)+18,gr=(or+og+ob)/3;
        or=or*0.7+gr*0.3;og=og*0.7+gr*0.3;ob=ob*0.7+gr*0.3;return 0xFF000000|(clamp(or)<<16)|(clamp(og)<<8)|clamp(ob);}
    private static int clamp(double v){return v<0?0:v>255?255:(int)v;}
    static double mercY(double lat,double n){double lr=Math.toRadians(lat);return (1-Math.log(Math.tan(lr)+1/Math.cos(lr))/Math.PI)/2*n;}
    static double mercX(double lon,double n){return (lon+180)/360*n;}
    /** Highest zoom whose India box, scaled to fit the free rect, is not shrunk below 80% (crisp: never upscaled past 1.0). */
    static int pickZoom(int fw){for(int z=3;z<9;z++){double n=(double)(1<<z)*TILE;if(fw/(mercX(IN_E,n)-mercX(IN_W,n))<=1.0)return z;}return 8;}
    /** Free area (root coordinates) not covered by the header, the card column or the attribution line. */
    private android.graphics.Rect freeRect(){int[] o=new int[2];int[] b=new int[2];root.getLocationInWindow(o);int W=root.getWidth(),H=root.getHeight();
        int left=W/2,top=0,bottom=H;View body=(View)todayPane.getParent();body.getLocationInWindow(b);left=b[0]-o[0]+body.getWidth();headerBar.getLocationInWindow(b);top=b[1]-o[1]+headerBar.getHeight();attributionView.getLocationInWindow(b);bottom=b[1]-o[1];
        int m=dp(16);return new android.graphics.Rect(left,top+m,W,bottom-m);}
    private static TextView shadow(TextView t){t.setShadowLayer(6,0,1,0xE6000000);return t;}
    private void loadMap(android.graphics.Rect free,int W,int H){try{if(W<=0||H<=0||free.width()<=0||free.height()<=0)return;
        int zoom=pickZoom(free.width());double n=(double)(1<<zoom)*TILE;double x0=mercX(IN_W,n),x1=mercX(IN_E,n),y0=mercY(IN_N,n),y1=mercY(IN_S,n);
        // India's longitude span fills card edge -> screen right edge (east tip touches the edge); never upscaled
        double s=free.width()/(x1-x0);double cx=(x0+x1)/2,cy=(y0+y1)/2;float fcx=free.exactCenterX(),fcy=free.exactCenterY();
        if((y1-y0)*s>free.height()){cy=y1;fcy=free.bottom;} // too tall: keep the southern tip above the attribution, crop Kashmir under the header
        File dir=new File(getCacheDir(),"maptiles");if(!dir.isDirectory()&&!dir.mkdirs())Log.w(TAG,"map cache dir missing");
        SharedPreferences p=prefs();if(p.getInt("map_cache_version",0)!=BuildConfig.VERSION_CODE){File[] all=dir.listFiles();long old=System.currentTimeMillis()-14L*86400000L;if(all!=null)for(File f:all)if((!f.getName().startsWith("osm")||f.lastModified()<old)&&!f.delete())Log.w(TAG,"could not delete "+f.getName());p.edit().putInt("map_cache_version",BuildConfig.VERSION_CODE).apply();}
        Bitmap bmp=Bitmap.createBitmap(W,H,Bitmap.Config.RGB_565);Canvas c=new Canvas(bmp);c.drawColor(0xFF1A1A1A);
        // world-pixel window that is visible right of the scrim
        double wl=cx+(0-fcx)/s,wr=cx+(W-fcx)/s,wt=cy+(0-fcy)/s,wb=cy+(H-fcy)/s;int max=1<<zoom;
        c.save();c.translate((float)(fcx-cx*s),(float)(fcy-cy*s));c.scale((float)s,(float)s);Paint fp=new Paint(Paint.FILTER_BITMAP_FLAG);
        Bitmap out=Bitmap.createBitmap(TILE,TILE,Bitmap.Config.ARGB_8888);int[] px=new int[TILE*TILE];int ok=0,total=0;
        for(int ty=(int)Math.floor(wt/TILE);ty<=(int)Math.floor(wb/TILE)&&!destroyed;ty++)for(int tx=(int)Math.floor(wl/TILE);tx<=(int)Math.floor(wr/TILE)&&!destroyed;tx++){if(ty<0||ty>=max)continue;int wx=((tx%max)+max)%max;total++;
            try{File f=new File(dir,"osm"+zoom+"_"+wx+"_"+ty+".png");byte[] data;if(f.length()>0)data=readFile(f);else{data=get("https://tile.openstreetmap.org/"+zoom+"/"+wx+"/"+ty+".png",524288);try(FileOutputStream o=new FileOutputStream(f)){o.write(data);}}
                Bitmap t=BitmapFactory.decodeByteArray(data,0,data.length);if(t==null)continue;if(t.getWidth()!=TILE||t.getHeight()!=TILE){Bitmap sc=Bitmap.createScaledBitmap(t,TILE,TILE,true);t.recycle();t=sc;}t.getPixels(px,0,TILE,0,0,TILE,TILE);t.recycle();for(int i=0;i<px.length;i++)px[i]=darkPixel(px[i]);out.setPixels(px,0,TILE,0,0,TILE,TILE);c.drawBitmap(out,tx*TILE,ty*TILE,fp);ok++;}
            catch(Exception e){Log.w(TAG,"map tile "+wx+","+ty+" failed "+e);}}
        out.recycle();
        String radar=drawRadar(c,zoom,wl,wr,wt,wb);c.restore();
        
        c.drawColor(0x28000000);Paint sp=new Paint();sp.setShader(new android.graphics.LinearGradient(0,0,0,dp(110),0x6E000000,0x00000000,android.graphics.Shader.TileMode.CLAMP));c.drawRect(0,0,W,dp(110),sp); // map edge to edge: light overall darkening + soft top fade for the header only
        if(WeatherLocation.valid(lat,lon))drawMarker(c,(float)((mercX(lon,n)-cx)*s+fcx),(float)((mercY(lat,n)-cy)*s+fcy));
        if(destroyed){bmp.recycle();return;}final String note=radar;Log.i(TAG,"map tiles "+ok+"/"+total+" z"+zoom+" scale "+String.format(Locale.US,"%.2f",s)+" free "+free.toShortString()+", radar: "+note);
        ui.post(()->{if(destroyed){bmp.recycle();return;}map=bmp;mapView.setImageBitmap(bmp);radarNote.setText(note);});}
        catch(Throwable t){Log.w(TAG,"map failed "+t);}}
    private String drawRadar(Canvas c,int zoom,double wl,double wr,double wt,double wb){try{JSONObject m=new JSONObject(new String(get("https://api.rainviewer.com/public/weather-maps.json",131072),"UTF-8"));String host=m.getString("host");JSONArray past=m.getJSONObject("radar").getJSONArray("past");if(past.length()==0)return "Rain radar unavailable";JSONObject frame=past.getJSONObject(past.length()-1);String path=frame.getString("path");long time=frame.optLong("time")*1000L;
        double n=(double)(1<<zoom)*TILE;int ix0=(int)(mercX(IN_W,n)/TILE),ix1=(int)(mercX(IN_E,n)/TILE),iy0=(int)(mercY(IN_N,n)/TILE),iy1=(int)(mercY(IN_S,n)/TILE); // India only
        Paint p=new Paint(Paint.FILTER_BITMAP_FLAG);p.setAlpha(140);int ok=0;
        for(int y=Math.max(iy0,(int)Math.floor(wt/TILE));y<=Math.min(iy1,(int)Math.floor(wb/TILE))&&!destroyed;y++)for(int x=Math.max(ix0,(int)Math.floor(wl/TILE));x<=Math.min(ix1,(int)Math.floor(wr/TILE))&&!destroyed;x++){try{byte[] data=get(host+path+"/256/"+zoom+"/"+x+"/"+y+"/2/1_1.png",524288);Bitmap t=BitmapFactory.decodeByteArray(data,0,data.length);if(t!=null){c.drawBitmap(t,x*TILE,y*TILE,p);t.recycle();ok++;}}catch(Exception e){Log.w(TAG,"radar tile failed "+e);}}
        if(ok==0)return "Rain radar unavailable";return "Rain radar "+new java.text.SimpleDateFormat("h:mm a",Locale.getDefault()).format(new java.util.Date(time));}catch(Exception e){Log.w(TAG,"radar failed "+e);return "Rain radar unavailable";}}
    private void drawMarker(Canvas c,float x,float y){Paint halo=new Paint(Paint.ANTI_ALIAS_FLAG);halo.setShader(new android.graphics.RadialGradient(x,y,52,new int[]{SettingsStyle.withAlpha(accent,0x90),SettingsStyle.withAlpha(accent,0x40),SettingsStyle.withAlpha(accent,0)},new float[]{0f,0.45f,1f},android.graphics.Shader.TileMode.CLAMP));c.drawCircle(x,y,52,halo);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(accent);c.drawCircle(x,y,13,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(0xFFFFFFFF);c.drawCircle(x,y,13,p);
        if(!place.isEmpty()){Paint t=new Paint(Paint.ANTI_ALIAS_FLAG);t.setColor(0xFFFFFFFF);t.setTextSize(30);t.setTypeface(Fonts.of(this,Fonts.PANCHANG,R.font.outfit_medium));float tw=t.measureText(place);android.graphics.RectF r=new android.graphics.RectF(x+24,y-60,x+24+tw+28,y-14);Paint bg=new Paint(Paint.ANTI_ALIAS_FLAG);bg.setColor(0xC812132A);c.drawRoundRect(r,22,22,bg);Paint.FontMetrics fm=t.getFontMetrics();c.drawText(place,x+38,r.centerY()-(fm.ascent+fm.descent)/2,t);}}
    private static byte[] readFile(File f)throws IOException{try(InputStream in=new java.io.FileInputStream(f)){return readAll(in,1048576);}}
    private static byte[] readAll(InputStream in,int max)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n,total=0;while((n=in.read(b))!=-1){total+=n;if(total>max)throw new IOException("Response too large");out.write(b,0,n);}return out.toByteArray();}
    private static boolean allowed(URL u){String h=u.getHost();return "https".equalsIgnoreCase(u.getProtocol())&&(h.equals("api.open-meteo.com")||h.equals("tile.openstreetmap.org")||h.equals("api.rainviewer.com")||h.equals("tilecache.rainviewer.com"));}
    private static byte[] get(String value,int max)throws IOException{URL u=new URL(value);if(!allowed(u))throw new IOException("Host not allowed");HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(10000);c.setReadTimeout(15000);c.setRequestProperty("User-Agent","RaiderLauncher/"+BuildConfig.VERSION_NAME+" (Android TV; github.com/apukale/raider-launcher)");
        try{int code=c.getResponseCode();if(code!=200)throw new IOException("HTTP "+code);try(InputStream in=c.getInputStream()){return readAll(in,max);}}finally{c.disconnect();}}
}
