package com.gathin.tvhome;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.util.Log;

public final class PhotoDownloadJob extends JobService {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Map<Integer,Future<?>> running=new HashMap<>();
    @Override public boolean onStartJob(JobParameters params){synchronized(running){Future<?> task=worker.submit(()->{getSharedPreferences(Config.SETTINGS_NAME,MODE_PRIVATE).edit().remove("photo_last_error").apply();long before=PhotoStore.size(this,PhotoStore.SOURCE_BOTH);int beforeCount=PhotoStore.list(this).size();Log.i("TvHomePhotos","refresh start job="+params.getJobId());try{PhotoStore.refresh(this,params.getJobId());}catch(Exception e){getSharedPreferences(Config.SETTINGS_NAME,MODE_PRIVATE).edit().putString("photo_last_error",e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage())).putLong("photo_last_error_at",System.currentTimeMillis()).apply();Log.e("TvHomePhotos","refresh failed job="+params.getJobId()+" "+e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage()));}finally{long after=PhotoStore.size(this,PhotoStore.SOURCE_BOTH);int count=PhotoStore.list(this).size();getSharedPreferences(Config.SETTINGS_NAME,MODE_PRIVATE).edit().putLong("photo_last_refresh_at",System.currentTimeMillis()).apply();Log.i("TvHomePhotos","refresh end job="+params.getJobId()+" addedPhotos="+Math.max(0,count-beforeCount)+" addedBytes="+Math.max(0,after-before)+" totalBytes="+after+" totalCount="+count);synchronized(running){running.remove(params.getJobId());}if(!Thread.currentThread().isInterrupted()){jobFinished(params,false);sendBroadcast(new Intent("com.gathin.tvhome.PHOTOS_UPDATED").setPackage(getPackageName()));}}});running.put(params.getJobId(),task);}return true;}
    @Override public boolean onStopJob(JobParameters params){synchronized(running){Future<?> task=running.remove(params.getJobId());if(task!=null)task.cancel(true);}return true;}
    @Override public void onDestroy(){synchronized(running){for(Future<?> task:running.values())task.cancel(true);running.clear();}worker.shutdownNow();super.onDestroy();}
}
