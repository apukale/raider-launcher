package com.gathin.tvhome;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PhotoDownloadJob extends JobService {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Map<Integer,Future<?>> running=new HashMap<>();
    @Override public boolean onStartJob(JobParameters params){synchronized(running){Future<?> task=worker.submit(()->{try{PhotoStore.refresh(this,params.getJobId());}catch(Exception ignored){}finally{synchronized(running){running.remove(params.getJobId());}if(!Thread.currentThread().isInterrupted()){jobFinished(params,false);sendBroadcast(new Intent("com.gathin.tvhome.PHOTOS_UPDATED").setPackage(getPackageName()));}}});running.put(params.getJobId(),task);}return true;}
    @Override public boolean onStopJob(JobParameters params){synchronized(running){Future<?> task=running.remove(params.getJobId());if(task!=null)task.cancel(true);}return true;}
    @Override public void onDestroy(){synchronized(running){for(Future<?> task:running.values())task.cancel(true);running.clear();}worker.shutdownNow();super.onDestroy();}
}
