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
 *  Forecast from Open-Meteo, map tiles from CARTO (OpenStreetMap data), rain radar from RainViewer. All free, no key. */
public class WeatherActivity extends Activity {
    private static final String TAG="TvHomeWeather";
    private static final int Z=5, X0=18, Y0=12, NX=7, NY=4, TILE=256;
    private static final int RX0=22, RX1=24, RY0=12, RY1=15; // radar only over India
    private static final long CACHE_MS=30L*60*1000;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final Handler ui=new Handler(Looper.getMainLooper());
    private volatile boolean destroyed;
    private FrameLayout root; private ImageView mapView; private Bitmap map;
    private LinearLayout todayPane, weekPane; private TextView subtitle, radarNote; private Button toggle;
    private android.widget.ImageButton back; private boolean week; private int accent; private double lat, lon; private String place; private boolean fahrenheit;

    @Override protected void onCreate(Bundle b){super.onCreate(b);Config cfg=Config.load(this);lat=WeatherLocation.lat(this,cfg);lon=WeatherLocation.lon(this,cfg);place=WeatherLocation.place(this,cfg);if(place==null)place="";fahrenheit=WeatherLocation.fahrenheit(this);accent=RaiderTheme.accent(this);build();
        NightEffects.dim(root,NightState.isActive(getSharedPreferences(Config.SETTINGS_NAME,MODE_PRIVATE),System.currentTimeMillis()));toggle.requestFocus();
        if(!WeatherLocation.valid(lat,lon)){subtitle.setText("No location set. Pick a city in Launcher settings > Weather.");}else{String cached=cachedJson();if(cached!=null)render(cached,false);io.execute(this::loadForecast);}
        io.execute(this::loadMap);}
    @Override protected void onDestroy(){destroyed=true;io.shutdownNow();if(mapView!=null)mapView.setImageDrawable(null);if(map!=null){map.recycle();map=null;}super.onDestroy();}

    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private TextView text(CharSequence s,float sp,int font,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTypeface(Fonts.of(this,Fonts.PANCHANG,font));t.setTextColor(color);t.setIncludeFontPadding(false);t.setSingleLine(false);return t;}
    private GradientDrawable panel(){GradientDrawable d=new GradientDrawable();d.setColor(0xB81C1E3F);d.setCornerRadius(dp(16));d.setStroke(dp(1),0x22FFFFFF);return d;}
    private String deg(int v){return v+"\u00B0";}
    private String unit(){return fahrenheit?"\u00B0F":"\u00B0C";}

    /** One screen (960x540 dp): 48 dp sides, 27 dp top/bottom. Cards on the left 560 dp; the map shows through on the right. */
    private void build(){root=new FrameLayout(this);root.setBackgroundColor(0xFF12132A);setContentView(root);
        mapView=new ImageView(this);mapView.setScaleType(ImageView.ScaleType.CENTER_CROP);mapView.setAlpha(0.6f);root.addView(mapView,new FrameLayout.LayoutParams(-1,-1));
        View scrim=new View(this);scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0xF012132A,0xB012132A,0x3012132A}));root.addView(scrim,new FrameLayout.LayoutParams(-1,-1));
        radarNote=text("",13,R.font.outfit_light,0xB3FFFFFF);FrameLayout.LayoutParams rn=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.END);rn.setMargins(0,dp(96),dp(48),0);root.addView(radarNote,rn);
        LinearLayout col=new LinearLayout(this);col.setOrientation(LinearLayout.VERTICAL);col.setPadding(dp(48),dp(27),dp(48),dp(27));root.addView(col,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout header=new LinearLayout(this);header.setOrientation(LinearLayout.HORIZONTAL);header.setGravity(Gravity.CENTER_VERTICAL);
        back=new android.widget.ImageButton(this);back.setId(View.generateViewId());back.setImageResource(R.drawable.ic_back);back.setContentDescription("Back");back.setBackground(RaiderTheme.focusBackground(this,accent,false));RaiderTheme.applyFocusAnimation(back);back.setImageTintList(RaiderTheme.accentStateList(accent));back.setScaleType(ImageView.ScaleType.CENTER);back.setFocusable(true);back.setFocusableInTouchMode(false);back.setOnClickListener(v->finish());LinearLayout.LayoutParams bk=new LinearLayout.LayoutParams(dp(52),dp(44));bk.setMarginEnd(dp(16));header.addView(back,bk);
        LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);titles.addView(text("Weather",28,R.font.outfit_medium,0xFFF4F1EA));subtitle=text(place.isEmpty()?"Loading\u2026":place+" \u00B7 loading\u2026",15,R.font.outfit_light,0xFFA9ABC7);LinearLayout.LayoutParams sl=new LinearLayout.LayoutParams(-2,-2);sl.topMargin=dp(4);titles.addView(subtitle,sl);header.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        toggle=new Button(this);toggle.setText("7-day forecast");toggle.setAllCaps(false);toggle.setTextSize(16);toggle.setTypeface(Fonts.of(this,Fonts.PANCHANG,R.font.outfit_medium));toggle.setIncludeFontPadding(false);toggle.setTextColor(0xFFF4F1EA);toggle.setStateListAnimator(null);toggle.setMinHeight(0);toggle.setMinimumHeight(0);toggle.setPadding(dp(20),0,dp(20),0);toggle.setBackground(RaiderTheme.focusBackground(this,accent,false));toggle.setFocusable(true);RaiderTheme.applyFocusAnimation(toggle);toggle.setOnClickListener(v->{week=!week;showPane();});header.addView(toggle,new LinearLayout.LayoutParams(-2,dp(44)));toggle.setId(View.generateViewId());toggle.setNextFocusLeftId(back.getId());toggle.setNextFocusUpId(back.getId());back.setNextFocusRightId(toggle.getId());back.setNextFocusDownId(toggle.getId());back.setNextFocusUpId(back.getId());back.setNextFocusLeftId(back.getId());col.addView(header);
        FrameLayout body=new FrameLayout(this);LinearLayout.LayoutParams bl=new LinearLayout.LayoutParams(dp(560),0,1);bl.topMargin=dp(16);col.addView(body,bl);
        todayPane=new LinearLayout(this);todayPane.setOrientation(LinearLayout.VERTICAL);body.addView(todayPane,new FrameLayout.LayoutParams(-1,-1));
        weekPane=new LinearLayout(this);weekPane.setOrientation(LinearLayout.VERTICAL);weekPane.setVisibility(View.GONE);body.addView(weekPane,new FrameLayout.LayoutParams(-1,-1));
        TextView attribution=text("Forecast: Open-Meteo.com \u00B7 Map: \u00A9 OpenStreetMap contributors \u00A9 CARTO \u00B7 Radar: RainViewer.com",13,R.font.outfit_light,0x99FFFFFF);LinearLayout.LayoutParams al=new LinearLayout.LayoutParams(-2,-2);al.topMargin=dp(8);col.addView(attribution,al);}
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

    // ---- map + radar ----
    private void loadMap(){try{Bitmap bmp=Bitmap.createBitmap(NX*TILE,NY*TILE,Bitmap.Config.RGB_565);Canvas c=new Canvas(bmp);c.drawColor(0xFF0E0F1A);BitmapFactory.Options o=new BitmapFactory.Options();o.inPreferredConfig=Bitmap.Config.RGB_565;int ok=0;
        File dir=new File(getCacheDir(),"maptiles");if(!dir.isDirectory()&&!dir.mkdirs())Log.w(TAG,"map cache dir missing");
        for(int y=0;y<NY&&!destroyed;y++)for(int x=0;x<NX&&!destroyed;x++){try{File f=new File(dir,"dark_"+Z+"_"+(X0+x)+"_"+(Y0+y)+".png");byte[] data;if(f.length()>0)data=readFile(f);else{data=get("https://basemaps.cartocdn.com/dark_all/"+Z+"/"+(X0+x)+"/"+(Y0+y)+".png",524288);try(FileOutputStream out=new FileOutputStream(f)){out.write(data);}}Bitmap t=BitmapFactory.decodeByteArray(data,0,data.length,o);if(t!=null){c.drawBitmap(t,x*TILE,y*TILE,null);t.recycle();ok++;}}catch(Exception e){Log.w(TAG,"map tile "+(X0+x)+","+(Y0+y)+" failed "+e);}}
        String radar=drawRadar(c);
        if(WeatherLocation.valid(lat,lon))drawMarker(c);
        if(destroyed){bmp.recycle();return;}final int tiles=ok;final String note=radar;Log.i(TAG,"map tiles "+tiles+"/"+(NX*NY)+", radar: "+note);
        ui.post(()->{if(destroyed){bmp.recycle();return;}map=bmp;mapView.setImageBitmap(bmp);radarNote.setText(note);});}
        catch(Throwable t){Log.w(TAG,"map failed "+t);}}
    private String drawRadar(Canvas c){try{JSONObject m=new JSONObject(new String(get("https://api.rainviewer.com/public/weather-maps.json",131072),"UTF-8"));String host=m.getString("host");JSONArray past=m.getJSONObject("radar").getJSONArray("past");if(past.length()==0)return "Rain radar unavailable";JSONObject frame=past.getJSONObject(past.length()-1);String path=frame.getString("path");long time=frame.optLong("time")*1000L;
        Paint p=new Paint(Paint.FILTER_BITMAP_FLAG);p.setAlpha(210);int ok=0;for(int y=RY0;y<=RY1&&!destroyed;y++)for(int x=RX0;x<=RX1&&!destroyed;x++){try{byte[] data=get(host+path+"/256/"+Z+"/"+x+"/"+y+"/2/1_1.png",524288);Bitmap t=BitmapFactory.decodeByteArray(data,0,data.length);if(t!=null){c.drawBitmap(t,(x-X0)*TILE,(y-Y0)*TILE,p);t.recycle();ok++;}}catch(Exception e){Log.w(TAG,"radar tile failed "+e);}}
        if(ok==0)return "Rain radar unavailable";return "Rain radar "+new java.text.SimpleDateFormat("h:mm a",Locale.getDefault()).format(new java.util.Date(time));}catch(Exception e){Log.w(TAG,"radar failed "+e);return "Rain radar unavailable";}}
    private void drawMarker(Canvas c){double n=1<<Z;double px=(lon+180)/360*n*TILE-X0*TILE;double lr=Math.toRadians(lat);double py=(1-Math.log(Math.tan(lr)+1/Math.cos(lr))/Math.PI)/2*n*TILE-Y0*TILE;if(px<0||py<0||px>NX*TILE||py>NY*TILE)return;
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(SettingsStyle.withAlpha(accent,0x55));c.drawCircle((float)px,(float)py,18,p);p.setColor(accent);c.drawCircle((float)px,(float)py,8,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(0xFFFFFFFF);c.drawCircle((float)px,(float)py,8,p);
        if(!place.isEmpty()){Paint t=new Paint(Paint.ANTI_ALIAS_FLAG);t.setColor(0xFFFFFFFF);t.setTextSize(22);t.setTypeface(Fonts.of(this,Fonts.PANCHANG,R.font.outfit_medium));t.setShadowLayer(4,0,1,0xFF000000);c.drawText(place,(float)px+16,(float)py-14,t);}}
    private static byte[] readFile(File f)throws IOException{try(InputStream in=new java.io.FileInputStream(f)){return readAll(in,1048576);}}
    private static byte[] readAll(InputStream in,int max)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n,total=0;while((n=in.read(b))!=-1){total+=n;if(total>max)throw new IOException("Response too large");out.write(b,0,n);}return out.toByteArray();}
    private static boolean allowed(URL u){String h=u.getHost();return "https".equalsIgnoreCase(u.getProtocol())&&(h.equals("api.open-meteo.com")||h.equals("basemaps.cartocdn.com")||h.endsWith(".basemaps.cartocdn.com")||h.equals("api.rainviewer.com")||h.equals("tilecache.rainviewer.com"));}
    private static byte[] get(String value,int max)throws IOException{URL u=new URL(value);if(!allowed(u))throw new IOException("Host not allowed");HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(10000);c.setReadTimeout(15000);c.setRequestProperty("User-Agent","RaiderLauncher/"+BuildConfig.VERSION_NAME+" (Android TV)");
        try{int code=c.getResponseCode();if(code!=200)throw new IOException("HTTP "+code);try(InputStream in=c.getInputStream()){return readAll(in,max);}}finally{c.disconnect();}}
}
