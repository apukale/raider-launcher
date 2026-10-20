package com.gathin.tvhome;

import android.content.Context;
import android.os.StatFs;
import android.text.format.Formatter;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/** Local-only screensaver photo collection, populated from Picsum and Bing. */
final class PhotoStore {
    static final int JOB_ID=78231, PICSUM_JOB_ID=78232, BING_JOB_ID=78233;
    static final String MANIFEST="photo_manifest.json";
    static final String SOURCE_BING="bing", SOURCE_PICSUM="picsum", SOURCE_BOTH="both";
    private static final long MB=1024L*1024L, MIN_FREE=2L*1024L*MB, DEFAULT_CAP=200L*MB, FILE_CAP=1536*1024L;
    static final class Photo {
        final String file,id,author,source,title,copyright,startdate;
        Photo(String f,String i,String a,String s,String t,String c,String d){file=f;id=i;author=a;source=s;title=t;copyright=c;startdate=d;}
    }
    private PhotoStore(){}
    static File dir(Context c){File f=new File(c.getFilesDir(),"photos");if(!f.exists())f.mkdirs();return f;}
    static long cap(Context c){int mb=c.getSharedPreferences(Config.SETTINGS_NAME,Context.MODE_PRIVATE).getInt("photo_storage_mb",200);return (mb==100||mb==200||mb==500?mb*MB:DEFAULT_CAP);}
    static String chosenSource(Context c){String s=c.getSharedPreferences(Config.SETTINGS_NAME,Context.MODE_PRIVATE).getString("photo_source",SOURCE_BOTH);return SOURCE_BING.equals(s)||SOURCE_PICSUM.equals(s)?s:SOURCE_BOTH;}
    static List<Photo> list(Context c){ArrayList<Photo> out=new ArrayList<>();try{JSONArray a=new JSONObject(readText(new File(c.getFilesDir(),MANIFEST))).optJSONArray("photos");if(a!=null)for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);String name=o.optString("file"),src=o.optString("source",SOURCE_PICSUM);File f=new File(dir(c),name);if(name.matches("[A-Za-z0-9_-]+\\.jpg")&&f.isFile()&&f.length()>0)out.add(new Photo(name,o.optString("id"),o.optString("author"),src,o.optString("title"),o.optString("copyright"),o.optString("startdate")));}}catch(Exception ignored){}return out;}
    static List<Photo> list(Context c,String selected){ArrayList<Photo> out=new ArrayList<>();for(Photo p:list(c))if(SOURCE_BOTH.equals(selected)||selected.equals(p.source))out.add(p);out.sort((a,b)->{if(!a.source.equals(b.source))return SOURCE_BING.equals(a.source)?-1:1;if(SOURCE_BING.equals(a.source))return b.startdate.compareTo(a.startdate);return 0;});return out;}
    static long size(Context c,String selected){long n=0;for(Photo p:list(c,selected))n+=new File(dir(c),p.file).length();return n;}
    static String summary(Context c){List<Photo> photos=list(c);long n=0;for(Photo p:photos)n+=new File(dir(c),p.file).length();return c.getString(R.string.photo_count_size,photos.size(),Formatter.formatFileSize(c,n),Formatter.formatFileSize(c,cap(c)));}
    static boolean hasPhotos(Context c){return !list(c,chosenSource(c)).isEmpty();}
    static void schedule(Context c,boolean now){android.app.job.JobScheduler js=(android.app.job.JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);if(js==null)return;android.content.ComponentName cn=new android.content.ComponentName(c,PhotoDownloadJob.class);schedulePeriodic(js,cn,PICSUM_JOB_ID,7L*24*60*60*1000);schedulePeriodic(js,cn,BING_JOB_ID,24L*60*60*1000);if(now){String src=chosenSource(c);if(!SOURCE_BING.equals(src))scheduleNow(js,cn,JOB_ID);if(!SOURCE_PICSUM.equals(src))scheduleNow(js,cn,BING_JOB_ID+100);}}
    private static void schedulePeriodic(android.app.job.JobScheduler js,android.content.ComponentName cn,int id,long period){if(js.getPendingJob(id)==null)js.schedule(new android.app.job.JobInfo.Builder(id,cn).setRequiredNetworkType(android.app.job.JobInfo.NETWORK_TYPE_UNMETERED).setPeriodic(period).setPersisted(false).build());}
    private static void scheduleNow(android.app.job.JobScheduler js,android.content.ComponentName cn,int id){js.schedule(new android.app.job.JobInfo.Builder(id,cn).setRequiredNetworkType(android.app.job.JobInfo.NETWORK_TYPE_UNMETERED).setMinimumLatency(0).setOverrideDeadline(1000).setPersisted(false).build());}
    static void refresh(Context c,int jobId)throws Exception{if(jobId==BING_JOB_ID||jobId==BING_JOB_ID+100)refreshBing(c);else refreshPicsum(c);}
    static void enforceCap(Context c){try{write(c,list(c));}catch(Exception ignored){}}
    private static void refreshPicsum(Context c)throws Exception{
        ensureSpace(c);File dir=dir(c);List<Photo> old=list(c);write(c,old);old=list(c);HashSet<String> existing=new HashSet<>();for(Photo p:old)if(SOURCE_PICSUM.equals(p.source))existing.add(p.id);ArrayList<Photo> incoming=new ArrayList<>();
        JSONArray items=new JSONArray(readUrl("https://picsum.photos/v2/list?page=1&limit=100",512*1024));
        for(int i=0;i<items.length()&&incoming.size()<10;i++){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("photo job stopped");if(!hasSpace(c))break;JSONObject item=items.getJSONObject(i);String id=item.optString("id"),author=item.optString("author");if(item.optInt("width")<=item.optInt("height")||!id.matches("[0-9]+")||existing.contains(id))continue;String name="picsum_"+id+".jpg";File dst=new File(dir,name),tmp=new File(dir,name+".tmp");try{long n=download(c,"https://picsum.photos/id/"+id+"/1920/1080.jpg",tmp,FILE_CAP);if(n<=0){tmp.delete();continue;}if(dst.exists())dst.delete();if(!tmp.renameTo(dst)){tmp.delete();continue;}incoming.add(new Photo(name,id,author,SOURCE_PICSUM,"","",""));}catch(Exception e){tmp.delete();if(Thread.currentThread().isInterrupted())throw e;}}
        if(incoming.isEmpty())return;ArrayList<Photo> all=new ArrayList<>(old);all.addAll(incoming);write(c,all);
    }
    private static void refreshBing(Context c)throws Exception{
        ensureSpace(c);File dir=dir(c);List<Photo> old=list(c);write(c,old);old=list(c);ArrayList<Photo> incoming=new ArrayList<>();HashSet<String> seen=new HashSet<>();
        JSONArray images=new JSONObject(readUrl("https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=8&mkt=en-IN",512*1024)).optJSONArray("images");if(images==null)throw new IOException("No Bing images");
        for(int i=0;i<images.length()&&incoming.size()<8;i++){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("photo job stopped");if(!hasSpace(c))break;JSONObject item=images.getJSONObject(i);String base=item.optString("urlbase"),date=item.optString("startdate");if(base.isEmpty()||!base.matches("/[A-Za-z0-9_./?=&%-]+")||base.contains("..")||!date.matches("[0-9]{8}"))continue;if(!seen.add(base))continue;boolean exists=false;for(Photo p:old)if(SOURCE_BING.equals(p.source)&&p.id.equals(base)){exists=true;break;}if(exists)continue;String key=base.replaceAll("[^A-Za-z0-9_-]","_");String name="bing_"+key+".jpg";File dst=new File(dir,name),tmp=new File(dir,name+".tmp");try{long n=download(c,"https://www.bing.com"+base+"_1920x1080.jpg",tmp,FILE_CAP);if(n<=0){tmp.delete();continue;}if(dst.exists())dst.delete();if(!tmp.renameTo(dst)){tmp.delete();continue;}incoming.add(new Photo(name,base,"",SOURCE_BING,item.optString("title"),item.optString("copyright"),date));}catch(Exception e){tmp.delete();if(Thread.currentThread().isInterrupted())throw e;}}
        if(incoming.isEmpty())return;ArrayList<Photo> all=new ArrayList<>(old);all.addAll(incoming);all.sort((a,b)->{if(!a.source.equals(b.source))return SOURCE_BING.equals(a.source)?-1:1;return b.startdate.compareTo(a.startdate);});write(c,all);
    }
    private static ArrayList<Photo> without(List<Photo> items,String source){ArrayList<Photo> out=new ArrayList<>();for(Photo p:items)if(!source.equals(p.source))out.add(p);return out;}
    private static void write(Context c,List<Photo> photos)throws Exception{
        ArrayList<Photo> keep=new ArrayList<>(photos);long total=0;for(Photo p:keep)total+=new File(dir(c),p.file).length();
        while(total>cap(c)){Photo p=removeOldestPicsum(keep);if(p==null)p=removeOldestBingBeyondNewestEight(keep);if(p==null)break;total-=new File(dir(c),p.file).length();}
        JSONArray a=new JSONArray();HashSet<String> live=new HashSet<>();for(Photo p:keep){JSONObject o=new JSONObject();o.put("file",p.file);o.put("id",p.id);o.put("author",p.author);o.put("source",p.source);o.put("title",p.title);o.put("copyright",p.copyright);o.put("startdate",p.startdate);a.put(o);live.add(p.file);}JSONObject root=new JSONObject();root.put("photos",a);File manifest=new File(c.getFilesDir(),MANIFEST),temp=new File(c.getFilesDir(),MANIFEST+".tmp");try(FileOutputStream out=new FileOutputStream(temp)){out.write(root.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));out.getFD().sync();}if(manifest.exists())manifest.delete();if(!temp.renameTo(manifest))throw new IOException("Could not replace photo manifest");File[] files=dir(c).listFiles();if(files!=null)for(File f:files)if(f.isFile()&&!live.contains(f.getName()))f.delete();
    }
    private static Photo removeOldestPicsum(List<Photo> all){for(int i=0;i<all.size();i++)if(SOURCE_PICSUM.equals(all.get(i).source))return all.remove(i);return null;}
    private static Photo removeOldestBingBeyondNewestEight(List<Photo> all){ArrayList<Photo> bing=new ArrayList<>();for(Photo p:all)if(SOURCE_BING.equals(p.source))bing.add(p);bing.sort((a,b)->b.startdate.compareTo(a.startdate));if(bing.size()<=8)return null;Photo p=bing.get(bing.size()-1);all.remove(p);return p;}
    private static boolean hasSpace(Context c){return new StatFs(c.getFilesDir().getAbsolutePath()).getAvailableBytes()>=MIN_FREE;}
    private static void ensureSpace(Context c)throws IOException{if(!hasSpace(c))throw new IOException("Insufficient data partition space for photo download");}
    private static long download(Context c,String url,File out,long cap)throws Exception{ensureSpace(c);try(InputStream in=open(url,0);FileOutputStream dst=new FileOutputStream(out)){byte[] b=new byte[16384];long n=0;int r;while((r=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("photo job stopped");ensureSpace(c);n+=r;if(n>cap)throw new IOException("photo over 1.5 MB");dst.write(b,0,r);ensureSpace(c);}return n;}}
    private static String readUrl(String url,int cap)throws Exception{try(InputStream in=open(url,0);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int r,n=0;while((r=in.read(b))!=-1){n+=r;if(n>cap)throw new IOException("response too large");out.write(b,0,r);}return out.toString("UTF-8");}}
    private static InputStream open(String value,int redirects)throws Exception{URL u=new URL(value);int port=u.getPort();String host=u.getHost().toLowerCase(Locale.ROOT);boolean allowed="picsum.photos".equals(host)||"fastly.picsum.photos".equals(host)||"www.bing.com".equals(host);if(!"https".equalsIgnoreCase(u.getProtocol())||(port!=-1&&port!=443)||!allowed||redirects>5)throw new IOException("URL outside photo hosts");HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(10000);c.setReadTimeout(20000);c.setRequestProperty("User-Agent","CleanYourTV/2.8");int code=c.getResponseCode();if(code>=300&&code<400){String location=c.getHeaderField("Location");c.disconnect();if(location==null)throw new IOException("missing redirect");return open(new URL(u,location).toString(),redirects+1);}if(code<200||code>=300){c.disconnect();throw new IOException("Photo HTTP "+code);}return new DisconnectingInputStream(c);}
    private static String readText(File f)throws Exception{return new String(java.nio.file.Files.readAllBytes(f.toPath()),java.nio.charset.StandardCharsets.UTF_8);}
    private static final class DisconnectingInputStream extends FilterInputStream{final HttpURLConnection c;DisconnectingInputStream(HttpURLConnection c)throws Exception{super(c.getInputStream());this.c=c;}public void close()throws IOException{try{super.close();}finally{c.disconnect();}}}
}
