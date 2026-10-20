package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import android.util.Log;

final class WeatherClient {
    static final long REFRESH_MS=30L*60*1000, FRESH_MS=3L*60*60*1000;
    interface Callback { void done(WeatherData data); }
    static long fetchedAt(Context c){return prefs(c).getLong("weather_fetched_at",0);}
    static WeatherData cached(Context c){SharedPreferences p=prefs(c);if(p.getLong("weather_fetched_at",0)==0)return null;try{return new WeatherData(p.getInt("weather_temp",0),p.getInt("weather_high",0),p.getInt("weather_low",0),p.getInt("weather_code",0),p.getInt("weather_apparent",0),p.getInt("weather_humidity",0),p.getInt("weather_precip",0));}catch(Exception e){return null;}}
    static void fetch(Context context,Config config,Callback cb){final double lat=WeatherLocation.lat(context,config),lon=WeatherLocation.lon(context,config);final boolean fahrenheit=WeatherLocation.fahrenheit(context);if(!WeatherLocation.valid(lat,lon)){Log.i("TvHomeWeather","no weather location set; skipping");return;}Context app=context.getApplicationContext();Log.i("TvHomeWeather","fetch start");new Thread(()->{WeatherData d=null;long now=System.currentTimeMillis();try{String url="https://api.open-meteo.com/v1/forecast?latitude="+lat+"&longitude="+lon+(fahrenheit?"&temperature_unit=fahrenheit":"")+"&current=temperature_2m,weather_code,apparent_temperature,relative_humidity_2m&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=Asia%2FKolkata&forecast_days=1";d=WeatherData.parse(read(url,0));SharedPreferences.Editor e=prefs(app).edit().putInt("weather_temp",d.temperature).putInt("weather_high",d.high).putInt("weather_low",d.low).putInt("weather_code",d.code).putInt("weather_apparent",d.apparent).putInt("weather_humidity",d.humidity).putInt("weather_precip",d.precipitation).putLong("weather_fetched_at",now).putLong("weather_last_try_at",now).remove("weather_last_error");e.apply();Log.i("TvHomeWeather","fetch success temp="+d.temperature+" high="+d.high+" low="+d.low+" code="+d.code);}catch(Exception ex){String error=ex.getClass().getSimpleName()+": "+String.valueOf(ex.getMessage());prefs(app).edit().putString("weather_last_error",error).putLong("weather_last_try_at",System.currentTimeMillis()).apply();Log.e("TvHomeWeather","fetch failed "+error);}cb.done(d);},"weather-fetch").start();}
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences(Config.SETTINGS_NAME,Context.MODE_PRIVATE);}
    private static String read(String value,int redirects)throws Exception{if(!WeatherData.allowsUrl(value)||redirects>5)throw new IOException("URL outside weather host");URL u=new URL(value);HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(10000);c.setReadTimeout(10000);try{int code=c.getResponseCode();if(code>=300&&code<400){String location=c.getHeaderField("Location");if(location==null)throw new IOException("Missing redirect");return read(new URL(u,location).toString(),redirects+1);}if(code<200||code>=300)throw new IOException("Weather HTTP "+code);try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n,total=0;while((n=in.read(b))!=-1){total+=n;if(total>65536)throw new IOException("Response too large");out.write(b,0,n);}return out.toString("UTF-8");}}finally{c.disconnect();}}
}
