package com.gathin.tvhome;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/** Offline, intentionally low precision panchang calculations. No Android dependencies. */
public final class Panchang {
    /** Observer used for sunrise, sunset, moonrise and Rahu Kalam. Default is a neutral reference point on the IST meridian (82.5°E); the TV replaces it with the weather city set in Launcher settings. */
    static final double REF_LAT=23.0, REF_LON=82.5;
    static volatile double siteLat=REF_LAT, siteLon=REF_LON;
    static void setLocation(double lat,double lon){if(Double.isNaN(lat)||Double.isNaN(lon)){siteLat=REF_LAT;siteLon=REF_LON;}else{siteLat=lat;siteLon=lon;}}

    private Panchang() {}
    private static final java.util.LinkedHashMap<Integer, Double> CHAITRA_CACHE = new java.util.LinkedHashMap<Integer, Double>(8, 0.75f, true) {
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<Integer, Double> eldest) { return size() > 8; }
    };
    private static final double DEG = Math.PI / 180.0;
    private static final String[] TITHI = {"Pratipada","Dvitiya","Tritiya","Chaturthi","Panchami","Shashthi","Saptami","Ashtami","Navami","Dashami","Ekadashi","Dwadashi","Trayodashi","Chaturdashi","Purnima"};
    private static final String[] NAK = {"Ashwini","Bharani","Krittika","Rohini","Mrigashira","Ardra","Punarvasu","Pushya","Ashlesha","Magha","Purva Phalguni","Uttara Phalguni","Hasta","Chitra","Swati","Vishakha","Anuradha","Jyeshtha","Mula","Purva Ashadha","Uttara Ashadha","Shravana","Dhanishtha","Shatabhisha","Purva Bhadrapada","Uttara Bhadrapada","Revati"};
    private static final String[] NAK_HI = {"\u0905\u0936\u094D\u0935\u093F\u0928\u0940","\u092D\u0930\u0923\u0940","\u0915\u0943\u0924\u094D\u0924\u093F\u0915\u093E","\u0930\u094B\u0939\u093F\u0923\u0940","\u092E\u0943\u0917\u0936\u093F\u0930\u093E","\u0906\u0930\u094D\u0926\u094D\u0930\u093E","\u092A\u0941\u0928\u0930\u094D\u0935\u0938\u0941","\u092A\u0941\u0937\u094D\u092F","\u0906\u0936\u094D\u0932\u0947\u0937\u093E","\u092E\u0918\u093E","\u092A\u0942\u0930\u094D\u0935\u093E \u092B\u093E\u0932\u094D\u0917\u0941\u0928\u0940","\u0909\u0924\u094D\u0924\u0930\u093E \u092B\u093E\u0932\u094D\u0917\u0941\u0928\u0940","\u0939\u0938\u094D\u0924","\u091A\u093F\u0924\u094D\u0930\u093E","\u0938\u094D\u0935\u093E\u0924\u093F","\u0935\u093F\u0936\u093E\u0916\u093E","\u0905\u0928\u0941\u0930\u093E\u0927\u093E","\u091C\u094D\u092F\u0947\u0937\u094D\u0920\u093E","\u092E\u0942\u0932","\u092A\u0942\u0930\u094D\u0935\u093E\u0937\u093E\u0922\u093E","\u0909\u0924\u094D\u0924\u0930\u093E\u0937\u093E\u0922\u093E","\u0936\u094D\u0930\u0935\u0923","\u0927\u0928\u093F\u0937\u094D\u0920\u093E","\u0936\u0924\u092D\u093F\u0937\u093E","\u092A\u0942\u0930\u094D\u0935\u093E\u092D\u093E\u0926\u094D\u0930\u092A\u0926","\u0909\u0924\u094D\u0924\u0930\u093E\u092D\u093E\u0926\u094D\u0930\u092A\u0926","\u0930\u0947\u0935\u0924\u0940"};
    // Rashi order Aries..Pisces; amanta month assigned to the solar sign at new moon.
    private static final String[] RASHI_MONTH = {"Vaishakha","Jyeshtha","Ashadha","Shravana","Bhadrapada","Ashvin","Kartika","Margashirsha","Pausha","Magha","Phalguna","Chaitra"};
    private static final String[] RASHI = {"Mesha","Vrishabha","Mithuna","Karka","Simha","Kanya","Tula","Vrishchika","Dhanu","Makara","Kumbha","Meena"};

    public static double julianDay(int year,int month,int day,int hour,int minute) {
        int y=year, m=month; if(m<=2){y--;m+=12;} int a=y/100, b=2-a+a/4;
        return Math.floor(365.25*(y+4716))+Math.floor(30.6001*(m+1))+day+b-1524.5+(hour+minute/60.0)/24.0;
    }
    public static double sunLongitude(double jd) {
        double t=(jd-2451545.0)/36525.0;
        double l0=norm(280.46646+t*(36000.76983+0.0003032*t));
        double m=norm(357.52911+t*(35999.05029-0.0001537*t));
        double c=(1.914602-t*(0.004817+0.000014*t))*sin(m)+(0.019993-0.000101*t)*sin(2*m)+0.000289*sin(3*m);
        double omega=125.04-1934.136*t;
        return norm(l0+c-0.00569-0.00478*sin(omega));
    }
    /** Meeus ch.47 principal lunar longitude terms. */
    public static double moonLongitude(double jd) {
        double t=(jd-2451545.0)/36525.0;
        double lp=norm(218.3164477+481267.88123421*t-0.0015786*t*t+t*t*t/538841.0-t*t*t*t/65194000.0);
        double d=norm(297.8501921+445267.1114034*t-0.0018819*t*t+t*t*t/545868.0-t*t*t*t/113065000.0);
        double m=norm(357.5291092+35999.0502909*t-0.0001536*t*t+t*t*t/24490000.0);
        double mp=norm(134.9633964+477198.8675055*t+0.0087414*t*t+t*t*t/69699.0-t*t*t*t/14712000.0);
        double f=norm(93.2720950+483202.0175233*t-0.0036539*t*t-t*t*t/3526000.0+t*t*t*t/863310000.0);
        double e=1-0.002516*t-0.0000074*t*t;
        // D, M, M', F, longitude coefficient (millionths of a degree)
        int[][] a={{0,0,1,0,6288774},{2,0,-1,0,1274027},{2,0,0,0,658314},{0,0,2,0,213618},{0,1,0,0,-185116},{0,0,0,2,-114332},{2,0,-2,0,58793},{2,-1,-1,0,57066},{2,0,1,0,53322},{2,-1,0,0,45758},{0,1,-1,0,-40923},{1,0,0,0,-34720},{0,1,1,0,-30383},{2,0,0,-2,15327},{0,0,1,2,-12528},{0,0,1,-2,10980},{4,0,-1,0,10675},{0,0,3,0,10034},{4,0,-2,0,8548},{2,1,-1,0,-7888},{2,1,0,0,-6766},{1,0,-1,0,-5163},{1,1,0,0,4987},{2,-1,1,0,4036},{2,0,2,0,3994},{4,0,0,0,3861},{2,0,-3,0,3665},{0,1,-2,0,-2689},{2,0,-1,2,-2602},{2,-1,-2,0,2390},{1,0,1,0,-2348},{2,-2,0,0,2236},{0,1,2,0,-2120},{0,2,0,0,-2069},{2,-2,-1,0,2048},{2,0,1,-2,-1773},{2,0,0,2,-1595},{4,-1,-1,0,1215},{0,0,2,2,-1110},{3,0,-1,0,-892},{2,1,1,0,-810},{4,-1,-2,0,759},{0,2,-1,0,-713},{2,2,-1,0,-700},{2,1,-2,0,691},{2,-1,0, -2,596},{4,0,1,0,549},{0,0,4,0,537},{4,-1,0,0,520},{1,0,-2,0,-487},{2,1,0,-2,-399},{0,0,2,-2,-381},{1,1,1,0,351},{3,0,-2,0,-340},{4,0,-3,0,330},{2,-1,2,0,327},{0,2,1,0,-323},{1,1,-1,0,299},{2,0,3,0,294}};
        // 60th Table 47.A entry (zero longitude coefficient; retained so the table is complete).
        a=java.util.Arrays.copyOf(a,60); a[59]=new int[]{2,0,-1,-2,0};
        double sum=0;
        for(int[] q:a){ double arg=q[0]*d+q[1]*m+q[2]*mp+q[3]*f; double ef=(Math.abs(q[1])==1?e:Math.abs(q[1])==2?e*e:1); sum+=q[4]*ef*sin(arg)/1_000_000.0; }
        double aa1=119.75+131.849*t, aa2=53.09+479264.290*t, aa3=313.45+481266.484*t;
        sum+=0.003958*sin(aa1)+0.001962*sin(lp-f)+0.000318*sin(aa2);
        // Approximate nutation in longitude (Meeus ch.22), needed for apparent lunar longitude.
        double omega=norm(125.04452-1934.136261*t+0.0020708*t*t+t*t*t/450000.0);
        double l=norm(280.4665+36000.7698*t), lm=norm(218.3165+481267.8813*t);
        double dpsi=(-17.20*sin(omega)-1.32*sin(2*l)-0.23*sin(2*lm)+0.21*sin(2*omega))/3600.0;
        return norm(lp+sum+dpsi);
    }
    public static double ayanamsa(double jd) { double y=(jd-2451545.0)/365.2425; return 23.85306+50.2388475*y/3600.0; }
    public static double elongation(double jd){return norm(moonLongitude(jd)-sunLongitude(jd));}
    /** Meeus principal lunar latitude terms, degrees. */
    private static double moonLatitude(double jd){double t=(jd-2451545.0)/36525.0;double d=norm(297.8501921+445267.1114034*t-0.0018819*t*t+t*t*t/545868.0),m=norm(357.5291092+35999.0502909*t-0.0001536*t*t),mp=norm(134.9633964+477198.8675055*t+0.0087414*t*t),f=norm(93.2720950+483202.0175233*t-0.0036539*t*t);int[][] a={{0,0,0,1,5128122},{0,0,1,1,280602},{0,0,1,-1,277693},{2,0,0,-1,173237},{2,0,-1,1,55413},{2,0,-1,-1,46271},{2,0,0,1,32573},{0,0,2,1,17198},{2,0,1,-1,9266},{0,0,2,-1,8822},{2,-1,0,-1,8216},{2,0,-2,-1,4324},{2,0,1,1,4200},{2,1,0,-1,-3359},{2,-1,-1,1,2463},{2,-1,0,1,2211},{2,-1,-1,-1,2065},{0,1,-1,-1,-1870},{4,0,-1,-1,1828},{0,1,0,1,-1794},{0,0,0,3,-1749}};double sum=0;for(int[] q:a){double arg=q[0]*d+q[1]*m+q[2]*mp+q[3]*f;sum+=q[4]*sin(arg);}return sum/1_000_000.0;}
    public static double illuminatedFraction(double jd){return (1.0-Math.cos(elongation(jd)*DEG))/2.0;}
    public static boolean waxing(double jd){return elongation(jd)<180.0;}
    public static String moonRashi(double jd){return RASHI[(int)(norm(moonLongitude(jd)-ayanamsa(jd))/30.0)];}
    public static String currentPaksha(double jd){return elongation(jd)<180.0?"Shukla":"Krishna";}
    public static String currentTithiName(double jd){int n=(int)(elongation(jd)/12.0)+1,local=n<=15?n:n-15;boolean shukla=n<=15;if(local==15)return shukla?"Purnima":"Amavasya";return TITHI[local-1];}
    public static String currentNakshatra(double jd){return NAK[(int)(norm(moonLongitude(jd)-ayanamsa(jd))/(360.0/27))];}
    /** UTC JD when the sidereal Moon enters the next Lahiri rashi. */
    public static double moonRashiEndJd(double jd){return crossing(jd,30.0,13.2,true);}

    /** Solves a nearest new/full moon around the supplied Julian date, to sub-minute numerical precision. */
    public static double phaseNear(double jd, boolean fullMoon) {
        double target=fullMoon?180:0, x=jd;
        for(int i=0;i<12;i++){
            double delta=wrap180(elongation(x)-target);
            if(Math.abs(delta)<0.00001) break;
            double rate=norm(moonLongitude(x+0.01)-sunLongitude(x+0.01)-moonLongitude(x)+sunLongitude(x))/0.01;
            if(rate>180) rate-=360; if(rate<0) rate+=360;
            x-=delta/Math.max(10,rate);
        }
        return x;
    }
    private static double wrap180(double x){x=norm(x);return x>180?x-360:x;}
    private static double norm(double x){x%=360;return x<0?x+360:x;}
    private static double sin(double d){return Math.sin(d*DEG);}

    /** UTC JD of local sunrise (IST), NOAA apparent upper limb. */
    public static double sunriseJd(int year,int month,int day) {
        double localNoon=julianDay(year,month,day,12,0)-5.5/24.0;
        double n=Math.round(localNoon-2451545.0009+siteLon/360.0);
        double jstar=2451545.0009-siteLon/360.0+n;
        double m=norm(357.5291+0.98560028*(jstar-2451545));
        double c=1.9148*sin(m)+0.0200*sin(2*m)+0.0003*sin(3*m);
        double lambda=norm(m+c+180+102.9372);
        double jtransit=jstar+0.0053*sin(m)-0.0069*sin(2*lambda);
        double decl=Math.asin(Math.sin(lambda*DEG)*Math.sin(23.44*DEG));
        double lat=siteLat*DEG, h=-0.833*DEG;
        double cosH=(Math.sin(h)-Math.sin(lat)*Math.sin(decl))/(Math.cos(lat)*Math.cos(decl));
        double ha=Math.acos(Math.max(-1,Math.min(1,cosH)))/(2*Math.PI);
        return jtransit-ha;
    }
    public static Result forDate(int year,int month,int day) {
        double jd=sunriseJd(year,month,day), sun=sunLongitude(jd), moon=moonLongitude(jd);
        int ti=(int)(norm(moon-sun)/12)+1; ti=Math.min(30,Math.max(1,ti));
        boolean shukla=ti<=15; int local=shukla?ti:ti-15;
        double sidMoon=norm(moon-ayanamsa(jd)); int ni=(int)(sidMoon/(360.0/27));
        double start=phaseNear(jd,false); if(start>jd) start-=29.530588853;
        double sunStart=norm(sunLongitude(start)-ayanamsa(start));
        int sign=(int)(sunStart/30); String lunarMonth=RASHI_MONTH[sign];
        double nextNewMoon=phaseNear(start+20,false); if(nextNewMoon<=start) nextNewMoon+=29.530588853;
        int nextSign=(int)(norm(sunLongitude(nextNewMoon)-ayanamsa(nextNewMoon))/30);
        // The month starting at this new moon is Adhika when the next new moon has the same Sun sign.
        if(nextSign==sign) lunarMonth="Adhika "+lunarMonth;
        Calendar cal=new GregorianCalendar(TimeZone.getTimeZone("UTC")); cal.setTimeInMillis((long)((jd-2440587.5)*86400000));
        int gy=cal.get(Calendar.YEAR); boolean yearStarted=jd>=chaitraStartJd(gy);
        int saka=gy-(yearStarted?78:79);
        String tname=shukla?TITHI[local-1]:(local==15?"Amavasya":TITHI[local-1]);
        String paksha=shukla?"Shukla":"Krishna";
        String festival=festival(lunarMonth.replace("Adhika ",""),shukla,local);
        return new Result(jd,lunarMonth,lunarMonth.replace("Ashvin","Ashwina"),paksha,local,ti,tname,NAK[ni],NAK_HI[ni],saka,festival);
    }
    /** First sunrise after the Amanta new moon in sidereal Pisces: Chaitra Shukla Pratipada. */
    public static double chaitraStartJd(int year){
        synchronized (CHAITRA_CACHE) { Double cached=CHAITRA_CACHE.get(year); if(cached!=null)return cached; }
        double from=julianDay(year,1,1,0,0)-5.5/24.0, until=julianDay(year+1,1,1,0,0)-5.5/24.0;
        double x=from;
        // At most 20 lunations per year; advance past the nearest conjunction each time.
        for(int i=0;i<20 && x<until;i++){
            double nm=phaseNear(x,false);
            if(nm>=from-1 && nm<until && (int)(norm(sunLongitude(nm)-ayanamsa(nm))/30)==11){
                // Civil sunrise following the conjunction is the first Chaitra day.
                double local=(nm+5.5/24.0); Calendar c=new GregorianCalendar(TimeZone.getTimeZone("UTC"));c.setTimeInMillis((long)((local-2440587.5)*86400000));
                double rise=sunriseJd(c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH));
                if(rise<=nm) { c.add(Calendar.DAY_OF_MONTH,1); rise=sunriseJd(c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH)); }
                synchronized (CHAITRA_CACHE) { CHAITRA_CACHE.put(year,rise); }
                return rise;
            }
            double advanced=nm+29.530588853/2.0+1.0;
            x=Math.max(x+20.0,advanced);
        }
        // Sane deterministic fallback if the astronomical search fails.
        synchronized (CHAITRA_CACHE) { CHAITRA_CACHE.put(year,from); }
        return from;
    }

    /** Continuous transitions for the live (current-time) display. Returned Julian days are UTC. */
    public static double tithiEndJd(double jd){return crossing(jd,12.0,12.19075,false);}
    public static double nakshatraEndJd(double jd){return crossing(jd,360.0/27.0,13.2,true);}
    private static double crossing(double jd,double step,double rate,boolean sidereal){
        double value=sidereal?norm(moonLongitude(jd)-ayanamsa(jd)):elongation(jd);
        double target=norm((Math.floor(value/step)+1)*step), needed=norm(target-value);
        double lo=jd,hi=jd+Math.max(step/rate*1.5,0.25);
        boolean bracketed=false;
        for(int i=0;i<2000;i++){
            double moved=norm((sidereal?norm(moonLongitude(hi)-ayanamsa(hi)):elongation(hi))-value);
            if(Double.isNaN(moved)||Double.isInfinite(moved))return Double.NaN;
            if(moved>=needed){bracketed=true;break;}
            hi+=0.05;
        }
        if(!bracketed || Double.isNaN(hi) || Double.isInfinite(hi) || hi<=lo)return Double.NaN;
        for(int i=0;i<48;i++){double mid=(lo+hi)/2;double moved=norm((sidereal?norm(moonLongitude(mid)-ayanamsa(mid)):elongation(mid))-value);if(moved<needed)lo=mid;else hi=mid;}
        return (lo+hi)/2;
    }
    public static double sunsetJd(int year,int month,int day){
        double rise=sunriseJd(year,month,day), localNoon=julianDay(year,month,day,12,0)-5.5/24.0;
        double n=Math.round(localNoon-2451545.0009+siteLon/360.0), js=2451545.0009-siteLon/360.0+n;
        double m=norm(357.5291+0.98560028*(js-2451545)), c=1.9148*sin(m)+0.0200*sin(2*m)+0.0003*sin(3*m), lambda=norm(m+c+180+102.9372);
        double transit=js+0.0053*sin(m)-0.0069*sin(2*lambda), decl=Math.asin(Math.sin(lambda*DEG)*Math.sin(23.44*DEG));
        double lat=siteLat*DEG,h=-0.833*DEG,cosH=(Math.sin(h)-Math.sin(lat)*Math.sin(decl))/(Math.cos(lat)*Math.cos(decl));
        return 2*transit-rise;
    }
    public static double rahuStartJd(int year,int month,int day){Calendar c=new GregorianCalendar(TimeZone.getTimeZone("UTC"));c.clear();c.set(year,month-1,day);int dow=c.get(Calendar.DAY_OF_WEEK);int part=dow==Calendar.MONDAY?2:dow==Calendar.SATURDAY?3:dow==Calendar.FRIDAY?4:dow==Calendar.WEDNESDAY?5:dow==Calendar.THURSDAY?6:dow==Calendar.TUESDAY?7:8;double r=sunriseJd(year,month,day),s=sunsetJd(year,month,day);return r+(part-1)*(s-r)/8.0;}
    public static double rahuEndJd(int year,int month,int day){double r=sunriseJd(year,month,day),s=sunsetJd(year,month,day);return rahuStartJd(year,month,day)+(s-r)/8.0;}
    private static final String[] YOGA={"Vishkambha","Priti","Ayushman","Saubhagya","Shobhana","Atiganda","Sukarma","Dhriti","Shula","Ganda","Vriddhi","Dhruva","Vyaghata","Harshana","Vajra","Siddhi","Vyatipata","Variyana","Parigha","Shiva","Siddha","Sadhya","Shubha","Shukla","Brahma","Indra","Vaidhriti"};
    private static final String[] KARANA={"Bava","Balava","Kaulava","Taitila","Garaja","Vanija","Vishti"};
    public static String yoga(double jd){return YOGA[(int)(norm(norm(sunLongitude(jd)-ayanamsa(jd))+norm(moonLongitude(jd)-ayanamsa(jd)))/(360.0/27))%27];}
    public static double yogaEndJd(double jd){return angularCrossing(jd,360.0/27,true);}
    public static String karana(double jd){int k=Math.min(59,(int)(elongation(jd)/6));if(k==0)return "Kimstughna";if(k<=56)return KARANA[(k-1)%7];return k==57?"Shakuni":k==58?"Chatushpada":"Naga";}
    public static double karanaEndJd(double jd){return crossing(jd,6.0,24.3815,false);}
    private static double angularCrossing(double jd,double step,boolean yogaValue){double v=yogaValue?norm(sunLongitude(jd)+moonLongitude(jd)-2*ayanamsa(jd)):0;double target=norm((Math.floor(v/step)+1)*step);return findCross(jd,step,norm(target-v),yogaValue);}
    private static double findCross(double jd,double step,double needed,boolean yogaValue){double lo=jd,hi=jd+0.25;for(int i=0;i<80;i++){double v=yogaValue?norm(sunLongitude(hi)+moonLongitude(hi)-2*ayanamsa(hi)):elongation(hi);double v0=yogaValue?norm(sunLongitude(jd)+moonLongitude(jd)-2*ayanamsa(jd)):elongation(jd);if(norm(v-v0)>=needed)break;hi+=0.25;if(i==79)return Double.NaN;}for(int i=0;i<48;i++){double mid=(lo+hi)/2;double v=yogaValue?norm(sunLongitude(mid)+moonLongitude(mid)-2*ayanamsa(mid)):elongation(mid);double v0=yogaValue?norm(sunLongitude(jd)+moonLongitude(jd)-2*ayanamsa(jd)):elongation(jd);if(norm(v-v0)<needed)lo=mid;else hi=mid;}return (lo+hi)/2;}
    public static double solarRashiEndJd(double jd){double v=norm(sunLongitude(jd)-ayanamsa(jd)),needed=30-(v%30);double lo=jd,hi=jd+40;if(norm(sunLongitude(hi)-ayanamsa(hi)-v)<needed)return Double.NaN;for(int i=0;i<48;i++){double mid=(lo+hi)/2;if(norm(sunLongitude(mid)-ayanamsa(mid)-v)<needed)lo=mid;else hi=mid;}return (lo+hi)/2;}
    public static String sunRashi(double jd){return RASHI[(int)(norm(sunLongitude(jd)-ayanamsa(jd))/30)];}
    public static double intervalStart(double rise,double set,int part){return rise+(set-rise)*(part-1)/8.0;}
    public static double intervalEnd(double rise,double set,int part){return rise+(set-rise)*part/8.0;}
    public static int weekdayPart(int dow,boolean yamaganda){int[] y={5,4,3,2,1,7,6},g={7,6,5,4,3,2,1};return (yamaganda?y:g)[dow-1];}
    public static double abhijitStartJd(double rise,double set){return (rise+set)/2-(set-rise)/30.0;}
    public static double abhijitEndJd(double rise,double set){return (rise+set)/2+(set-rise)/30.0;}
    private static double siderealTime(double jd){double t=(jd-2451545.0)/36525.0;return norm(280.46061837+360.98564736629*(jd-2451545.0)+0.000387933*t*t-t*t*t/38710000.0);}
    private static double moonAltitude(double jd){double lon=moonLongitude(jd)*DEG,lat=moonLatitude(jd)*DEG;double eps=23.4393*DEG;double ra=Math.atan2(Math.sin(lon)*Math.cos(eps)-Math.tan(lat)*Math.sin(eps),Math.cos(lon));double dec=Math.asin(Math.sin(lat)*Math.cos(eps)+Math.cos(lat)*Math.sin(eps)*Math.sin(lon));double ha=(siderealTime(jd)+siteLon-Math.toDegrees(ra))*DEG;double parallax=0.9507;double h0=(0.7275*parallax-0.5667)*DEG;return Math.asin(Math.sin(siteLat*DEG)*Math.sin(dec)+Math.cos(siteLat*DEG)*Math.cos(dec)*Math.cos(ha))-h0;}
    public static double[] moonriseMoonset(int y,int m,int d){double start=julianDay(y,m,d,0,0)-5.5/24.0,end=start+1.25,prev=moonAltitude(start);double[] out={Double.NaN,Double.NaN};for(int i=1;i<=180;i++){double a=start+(end-start)*(i-1)/180,b=start+(end-start)*i/180,cur=moonAltitude(b);if((prev<=0&&cur>0)||(prev>=0&&cur<0)){double lo=a,hi=b;boolean up=cur>prev;for(int n=0;n<32;n++){double mid=(lo+hi)/2;if((moonAltitude(mid)>0)==up)hi=mid;else lo=mid;}double root=(lo+hi)/2;if(up&&Double.isNaN(out[0])&&root<=start+1){out[0]=root;if(!Double.isNaN(out[1])&&out[1]<root)out[1]=Double.NaN;}if(!up&&!Double.isNaN(out[0])&&root>out[0]&&Double.isNaN(out[1]))out[1]=root;}prev=cur;}return out;}
    public static final class DayEvents {public final Result base;public final double sunset,moonrise,moonset,tithiEnd,nakshatraEnd,yogaEnd,karanaEnd,moonRashiEnd,sunRashiEnd,rahuStart,rahuEnd,yamagandaStart,yamagandaEnd,gulikaStart,gulikaEnd,abhijitStart,abhijitEnd;public DayEvents(int y,int m,int d){base=forDate(y,m,d);double rise=base.sunriseJd;sunset=sunsetJd(y,m,d);double[] moon=moonriseMoonset(y,m,d);moonrise=moon[0];moonset=moon[1];tithiEnd=tithiEndJd(rise);nakshatraEnd=nakshatraEndJd(rise);yogaEnd=yogaEndJd(rise);karanaEnd=karanaEndJd(rise);moonRashiEnd=moonRashiEndJd(rise);sunRashiEnd=solarRashiEndJd(rise);Calendar c=new GregorianCalendar(TimeZone.getTimeZone("UTC"));c.setTimeInMillis((long)((rise-2440587.5)*86400000));int dow=c.get(Calendar.DAY_OF_WEEK);int[] rahu={8,2,7,5,6,4,3};int rp=rahu[dow-1],yp=weekdayPart(dow,true);rahuStart=intervalStart(rise,sunset,rp);rahuEnd=intervalEnd(rise,sunset,rp);yamagandaStart=intervalStart(rise,sunset,yp);yamagandaEnd=intervalEnd(rise,sunset,yp);int gp=dow;int part=new int[]{7,6,5,4,3,2,1}[gp-1];gulikaStart=intervalStart(rise,sunset,part);gulikaEnd=intervalEnd(rise,sunset,part);abhijitStart=abhijitStartJd(rise,sunset);abhijitEnd=abhijitEndJd(rise,sunset);}}
    public static int clampDay(int year,int month,int day){Calendar c=new GregorianCalendar(TimeZone.getTimeZone("UTC"));c.clear();c.set(year,month-1,1);return Math.min(Math.max(1,day),c.getActualMaximum(Calendar.DAY_OF_MONTH));}
    private static String festival(String month,boolean s,int t){
        // Match the sunrise tithi. Some observances use pradosh or nishita rules instead, so this
        // simple screensaver list can show those festivals a day off occasionally.
        if((s&&t==11)||(!s&&t==11))return "Ekadashi";
        String key=month+" "+(s?"S":"K")+t;
        switch(key){case "Chaitra S1":return "Ugadi / Gudi Padwa";case "Chaitra S9":return "Ram Navami";case "Chaitra S15":return "Hanuman Jayanti";case "Vaishakha S3":return "Akshaya Tritiya";case "Ashadha S15":return "Guru Purnima";case "Shravana S15":return "Raksha Bandhan";case "Shravana K8":return "Krishna Janmashtami";case "Bhadrapada S4":return "Ganesh Chaturthi";case "Ashvin S1":return "Navaratri begins";case "Ashvin S10":return "Dussehra / Vijayadashami";case "Ashvin S15":return "Sharad Purnima";case "Ashvin K4":return "Karva Chauth";case "Ashvin K13":return "Dhanteras";case "Ashvin K14":return "Naraka Chaturdashi";case "Ashvin K15":return "Diwali / Lakshmi Puja";case "Kartika S1":return "Govardhan Puja";case "Kartika S2":return "Bhai Dooj";case "Kartika S15":return "Kartika Purnima";case "Pausha S11":return "Vaikuntha Ekadashi";case "Magha S5":return "Vasant Panchami";case "Magha K14":return "Maha Shivaratri";case "Phalguna S15":return "Holika Dahan";case "Phalguna K1":return "Holi";default:return t==15?(s?"Purnima":"Amavasya"):"";}
    }
    public static final class Result {
        public final double sunriseJd; public final String month,monthDisplay,paksha,tithiName,nakshatra,nakshatraHindi,festival; public final int tithi,tithiNumber,shakaSamvat;
        Result(double j,String m,String md,String p,int t,int n,String tn,String nk,String nh,int ss,String f){sunriseJd=j;month=m;monthDisplay=md;paksha=p;tithi=t;tithiNumber=n;tithiName=tn;nakshatra=nk;nakshatraHindi=nh;shakaSamvat=ss;festival=f;}
    }
}
