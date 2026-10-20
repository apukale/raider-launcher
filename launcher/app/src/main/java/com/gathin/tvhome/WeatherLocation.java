package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Weather (and Panchang) location chosen on the TV. Stored only in the TV's prefs, never in the repo. */
final class WeatherLocation {
    static final String MODE="weather_loc_mode", LAT="weather_city_lat", LON="weather_city_lon", NAME="weather_city_name", UNITS="weather_units";
    static final String CITY="city", BUILD="build";
    static final class Place { final String name,label; final double lat,lon; Place(String n,String l,double la,double lo){name=n;label=l;lat=la;lon=lo;} }

    private static double num(String s){try{return Double.parseDouble(s.trim());}catch(Exception e){return Double.NaN;}}
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences(Config.SETTINGS_NAME,Context.MODE_PRIVATE);}
    static boolean valid(double lat,double lon){return !Double.isNaN(lat)&&!Double.isNaN(lon)&&lat>=-90&&lat<=90&&lon>=-180&&lon<=180;}
    static double buildLat(){return num(BuildConfig.WEATHER_LATITUDE);}
    static double buildLon(){return num(BuildConfig.WEATHER_LONGITUDE);}
    static boolean hasBuildDefault(){return valid(buildLat(),buildLon());}
    static String buildPlace(){String p=BuildConfig.WEATHER_PLACE==null?"":BuildConfig.WEATHER_PLACE.trim();return p.isEmpty()&&hasBuildDefault()?"Build default":p;}

    /** First run of 2.11.10: copy the build's local.properties location onto the TV so weather keeps working in later builds without it. */
    static void migrate(Context c){SharedPreferences p=prefs(c);if(p.contains(MODE))return;SharedPreferences.Editor e=p.edit();
        if(hasBuildDefault())e.putString(MODE,CITY).putString(LAT,Double.toString(buildLat())).putString(LON,Double.toString(buildLon())).putString(NAME,buildPlace());else e.putString(MODE,BUILD);e.apply();}
    static boolean usingCity(Context c){migrate(c);return CITY.equals(prefs(c).getString(MODE,BUILD));}
    /** The saved city, or null when the build/theme default is in use. */
    static double[] override(Context c){if(!usingCity(c))return null;SharedPreferences p=prefs(c);double la=num(p.getString(LAT,"")),lo=num(p.getString(LON,""));return valid(la,lo)?new double[]{la,lo}:null;}
    static String cityName(Context c){return prefs(c).getString(NAME,"");}
    static boolean fahrenheit(Context c){return "f".equals(prefs(c).getString(UNITS,"c"));}
    static double lat(Context c,Config cfg){double[] o=override(c);return o!=null?o[0]:cfg.weatherLatitude;}
    static double lon(Context c,Config cfg){double[] o=override(c);return o!=null?o[1]:cfg.weatherLongitude;}
    static String place(Context c,Config cfg){return override(c)!=null?cityName(c):cfg.weatherPlace;}

    static void useCity(Context c,Place p){prefs(c).edit().putString(MODE,CITY).putString(LAT,Double.toString(p.lat)).putString(LON,Double.toString(p.lon)).putString(NAME,p.name).apply();changed(c);}
    static void useBuild(Context c){prefs(c).edit().putString(MODE,BUILD).apply();changed(c);}
    static void setFahrenheit(Context c,boolean f){prefs(c).edit().putString(UNITS,f?"f":"c").apply();invalidate(c);}
    private static void changed(Context c){invalidate(c);Config.load(c);}
    /** Drop the cached reading so Home fetches again for the new place or units. */
    static void invalidate(Context c){prefs(c).edit().remove("weather_fetched_at").remove("weather_last_error").remove("weather_last_try_at").apply();}

    /** Open-Meteo geocoding (free, no key). Call off the main thread. */
    static List<Place> search(String query)throws Exception{List<Place> out=new ArrayList<>();String q=query==null?"":query.trim();if(q.length()<2)return out;
        URL u=new URL("https://geocoding-api.open-meteo.com/v1/search?count=5&language=en&format=json&name="+URLEncoder.encode(q,"UTF-8"));HttpURLConnection h=(HttpURLConnection)u.openConnection();h.setInstanceFollowRedirects(false);h.setConnectTimeout(10000);h.setReadTimeout(10000);String body;
        try{int code=h.getResponseCode();if(code!=200)throw new IOException("Geocoding HTTP "+code);try(InputStream in=h.getInputStream();ByteArrayOutputStream b=new ByteArrayOutputStream()){byte[] buf=new byte[4096];int n,total=0;while((n=in.read(buf))!=-1){total+=n;if(total>65536)throw new IOException("Response too large");b.write(buf,0,n);}body=b.toString("UTF-8");}}finally{h.disconnect();}
        JSONArray r=new JSONObject(body).optJSONArray("results");if(r==null)return out;
        for(int i=0;i<r.length()&&out.size()<5;i++){JSONObject o=r.getJSONObject(i);double la=o.optDouble("latitude",Double.NaN),lo=o.optDouble("longitude",Double.NaN);if(!valid(la,lo))continue;String name=o.optString("name","").trim();if(name.isEmpty())continue;StringBuilder l=new StringBuilder(name);for(String k:new String[]{"admin2","admin1","country"}){String v=o.optString(k,"").trim();if(!v.isEmpty()&&!v.equals(name)&&l.indexOf(v)<0)l.append(", ").append(v);}out.add(new Place(name,l.toString(),la,lo));}
        return out;}
}