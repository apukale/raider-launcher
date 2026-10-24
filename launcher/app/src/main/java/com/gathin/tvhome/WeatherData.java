package com.gathin.tvhome;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure Java Open-Meteo response parsing and WMO condition mapping. */
final class WeatherData {
    /** Home shows today's rain chance only from this percent up. Screensaver uses the same cutoff. */
    static final int RAIN_CHANCE_MIN = 40;
    final int temperature, high, low, code, apparent, humidity, precipitation;
    WeatherData(int temperature,int high,int low,int code,int apparent,int humidity,int precipitation){this.temperature=temperature;this.high=high;this.low=low;this.code=code;this.apparent=apparent;this.humidity=humidity;this.precipitation=precipitation;}
    static WeatherData parse(String json) {
        String current=object(json,"current"), daily=object(json,"daily");
        int t=round(number(current,"temperature_2m")), high=round(arrayNumber(daily,"temperature_2m_max")), low=round(arrayNumber(daily,"temperature_2m_min"));
        int code=(int)number(current,"weather_code");
        return new WeatherData(t,high,low,code,round(number(current,"apparent_temperature")),(int)number(current,"relative_humidity_2m"),(int)arrayNumber(daily,"precipitation_probability_max"));
    }
    private static int round(double d){return (int)Math.floor(d+0.5);}
    private static String object(String s,String key){Matcher m=Pattern.compile("\\\""+key+"\\\"\\s*:\\s*\\{([^}]*)\\}").matcher(s);if(!m.find())throw new IllegalArgumentException("Missing "+key);return m.group(1);}
    private static double number(String s,String key){Matcher m=Pattern.compile("\\\""+key+"\\\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").matcher(s);if(!m.find())throw new IllegalArgumentException("Missing "+key);return Double.parseDouble(m.group(1));}
    private static double arrayNumber(String s,String key){Matcher m=Pattern.compile("\\\""+key+"\\\"\\s*:\\s*\\[\\s*(-?\\d+(?:\\.\\d+)?)").matcher(s);if(!m.find())throw new IllegalArgumentException("Missing "+key);return Double.parseDouble(m.group(1));}
    /** Same suffix Home appends: blank below {@link #RAIN_CHANCE_MIN}, otherwise " · 40%". */
    static String rainSuffix(int precipitationPercent) {
        return precipitationPercent >= RAIN_CHANCE_MIN ? " \u00B7 " + precipitationPercent + "%" : "";
    }
    static String iconGroup(int code){if(code==0)return "clear";if(code<=3&&code>=1)return "cloud";if(code==45||code==48)return "fog";if(code>=51&&code<=57)return "drizzle";if((code>=61&&code<=67)||(code>=80&&code<=82))return "rain";if((code>=71&&code<=77)||(code>=85&&code<=86))return "snow";if(code>=95&&code<=99)return "thunder";return "cloud";}
    static boolean allowsUrl(String value){try{java.net.URL u=new java.net.URL(value);return "https".equalsIgnoreCase(u.getProtocol())&&"api.open-meteo.com".equalsIgnoreCase(u.getHost())&&(u.getPort()==-1||u.getPort()==443);}catch(Exception e){return false;}}
}
