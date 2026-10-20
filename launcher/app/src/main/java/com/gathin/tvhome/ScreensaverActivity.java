package com.gathin.tvhome;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

/** User-started preview of the exact shared ClockDream renderer. */
public final class ScreensaverActivity extends Activity {
    private ScreensaverController renderer;
    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);FrameLayout root=(FrameLayout)getLayoutInflater().inflate(R.layout.dream_clock,null);setContentView(root);renderer=new ScreensaverController(this,root);renderer.start();}
    @Override public boolean dispatchKeyEvent(KeyEvent e){if(e.getAction()==KeyEvent.ACTION_DOWN){finish();return true;}return true;}
    @Override protected void onResume(){super.onResume();if(renderer!=null)renderer.start();}
    @Override protected void onPause(){if(renderer!=null)renderer.stop();super.onPause();}
    @Override protected void onStop(){if(renderer!=null)renderer.stop();super.onStop();}
}
