package com.gathin.tvhome;

import android.service.dreams.DreamService;
import android.widget.FrameLayout;

/** Stable Android screensaver component; its rendered content is shared with ScreensaverActivity. */
public class ClockDream extends DreamService {
    private ScreensaverController renderer;
    @Override public void onAttachedToWindow(){super.onAttachedToWindow();setInteractive(false);setFullscreen(true);setScreenBright(false);FrameLayout root=(FrameLayout)android.view.LayoutInflater.from(this).inflate(R.layout.dream_clock,null);setContentView(root);renderer=new ScreensaverController(this,root);}
    @Override public void onDreamingStarted(){super.onDreamingStarted();if(renderer!=null)renderer.start();}
    @Override public void onDreamingStopped(){if(renderer!=null)renderer.stop();super.onDreamingStopped();}
    @Override public void onDetachedFromWindow(){if(renderer!=null)renderer.stop();renderer=null;super.onDetachedFromWindow();}
}
