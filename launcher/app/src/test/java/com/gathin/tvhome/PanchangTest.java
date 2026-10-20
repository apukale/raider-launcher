package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class PanchangTest {
    private static double jd(String date){String[] p=date.split("[- :]");return Panchang.julianDay(Integer.parseInt(p[0]),Integer.parseInt(p[1]),Integer.parseInt(p[2]),Integer.parseInt(p[3]),Integer.parseInt(p[4]));}
    @Test(timeout = 5000) public void principalPhasesWithinTwoHours(){assertPhase("2024-11-01 12:47",false);assertPhase("2024-10-17 11:26",true);assertPhase("2025-03-14 06:55",true);assertPhase("2025-10-21 12:25",false);assertPhase("2026-03-03 11:38",true);}
    private void assertPhase(String d,boolean full){String[] p=d.split("[- :]");double target=jd(d),got=Panchang.phaseNear(target,full);assertTrue(d+" phase error hours="+Math.abs(got-target)*24,Math.abs(got-target)*24<2);}
    @Test(timeout = 5000) public void festivalSunriseDates(){check(2024,10,12,"Ashvin","Shukla",10);check(2025,10,2,"Ashvin","Shukla",10);check(2026,10,20,"Ashvin","Shukla",10);check(2024,11,1,"Ashvin","Krishna",15);check(2025,10,21,"Ashvin","Krishna",15);check(2026,11,8,"Ashvin","Krishna",15);check(2024,9,7,"Bhadrapada","Shukla",4);check(2025,8,27,"Bhadrapada","Shukla",4);check(2026,9,14,"Bhadrapada","Shukla",4);}
    // In the Amanta system, Holi is Phalguna Krishna 1, the day after Phalguna Purnima.
    @Test(timeout = 5000) public void holiSunriseTithi(){checkHoli(2024,3,25);checkHoli(2025,3,14);checkHoli(2026,3,4);}
    private void checkHoli(int y,int m,int d){Panchang.Result r=Panchang.forDate(y,m,d);assertTrue(("Phalguna".equals(r.month)&&"Shukla".equals(r.paksha)&&r.tithi==15)||("Phalguna".equals(r.month)&&"Krishna".equals(r.paksha)&&r.tithi==1));}
    @Test(timeout = 5000) public void fullAndNewMoonTithis(){Panchang.Result full=Panchang.forDate(2025,3,14),newMoon=Panchang.forDate(2024,11,1);assertEquals("Shukla",full.paksha);assertTrue(full.tithi==14||full.tithi==15);assertEquals("Krishna",newMoon.paksha);assertTrue(newMoon.tithi==14||newMoon.tithi==15);}
    @Test(timeout = 5000) public void samvatForOctober2026(){Panchang.Result r=Panchang.forDate(2026,10,20);assertEquals(2083,r.vikramSamvat);assertEquals(1948,r.shakaSamvat);}
    @Test(timeout = 5000) public void referenceSunriseSunsetAndRahuKalam(){double rise=Panchang.sunriseJd(2026,10,20),set=Panchang.sunsetJd(2026,10,20);assertWithinMinutes("sunrise",rise,2026,10,20,6,10,3);assertWithinMinutes("sunset",set,2026,10,20,17,51,3);assertWithinMinutes("Rahu Kalam start",Panchang.rahuStartJd(2026,10,20),2026,10,20,14,56,3);assertWithinMinutes("Rahu Kalam end",Panchang.rahuEndJd(2026,10,20),2026,10,20,16,24,3);assertEquals(6.0/8.0,(Panchang.rahuStartJd(2026,10,20)-rise)/(set-rise),0.0001);}
    @Test(timeout = 5000) public void lunarIlluminationAtPublishedPhases(){assertTrue(Panchang.illuminatedFraction(jd("2025-03-14 06:55"))>0.99);assertTrue(Panchang.illuminatedFraction(jd("2024-11-01 12:47"))<0.01);}
    @Test(timeout = 5000) public void currentTransitionPublishedTimes(){checkTransition(2026,10,20,12,50,false);checkTransition(2024,11,1,18,16,false);checkTransition(2025,3,14,12,23,false);checkTransition(2025,8,27,15,44,false);checkTransition(2025,10,21,17,54,false);checkTransition(2026,3,3,17,7,false);checkTransition(2026,9,14,7,6,false);checkNak(2026,10,20,18,2);checkNak(2025,10,21,22,59);checkNak(2026,9,14,13,55);}
    private void checkTransition(int y,int m,int d,int h,int min,boolean ignored){Calendar c=new GregorianCalendar(TimeZone.getTimeZone("UTC"));c.clear();c.set(y,m-1,d,h,min);double expect=c.getTimeInMillis()/86400000.0+2440587.5-5.5/24.0;double start=Panchang.julianDay(y,m,d,0,0)-5.5/24.0;double error=Math.abs(Panchang.tithiEndJd(start)-expect)*1440;assertTrue("tithi end "+y+"-"+m+"-"+d+" error="+error+" minutes",error<=10);}
    private void checkNak(int y,int m,int d,int h,int min){Calendar c=new GregorianCalendar(TimeZone.getTimeZone("UTC"));c.clear();c.set(y,m-1,d,h,min);double expect=c.getTimeInMillis()/86400000.0+2440587.5-5.5/24.0;double localMidnight=Panchang.julianDay(y,m,d,0,0)-5.5/24.0;double error=Math.abs(Panchang.nakshatraEndJd(localMidnight)-expect)*1440;assertTrue("nakshatra end "+y+"-"+m+"-"+d+" error="+error+" minutes",error<=10);}
    private void assertWithinMinutes(String label,double jd,int y,int m,int d,int h,int min,int tolerance){double local=Panchang.julianDay(y,m,d,h,min)-5.5/24.0;assertTrue(label+" error="+Math.abs(jd-local)*1440,Math.abs(jd-local)*1440<=tolerance);}
    private void check(int y,int m,int d,String month,String paksha,int t){Panchang.Result r=Panchang.forDate(y,m,d);assertEquals(month,r.month);assertEquals(paksha,r.paksha);assertTrue("tithi="+r.tithi,Math.abs(r.tithi-t)<=1);}
}
