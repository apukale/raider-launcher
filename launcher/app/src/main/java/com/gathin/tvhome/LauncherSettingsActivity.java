package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.usage.StorageStatsManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.Build;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.provider.Settings.Global;
import android.provider.Settings;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.format.Formatter;
import android.view.WindowManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ColorDrawable;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Remote-friendly settings owned by the launcher; system settings stay one row away. */
public final class LauncherSettingsActivity extends Activity {
    private static final int NIGHT_HOURS_PER_HALF_DAY = 12;
    private static final int NIGHT_MINUTES_PER_HOUR = 60;
    private static final int NIGHT_MINUTE_STEP = 5;
    private static final int NIGHT_PICKER_PADDING_DP = 24;
    private static final int NIGHT_PICKER_CELL_HEIGHT_DP = 96;
    private static final int NIGHT_PICKER_CELL_WIDTH_DP = 92;
    private static final String PRIVATE_DNS_MODE = "private_dns_mode";
    private static final String PRIVATE_DNS_SPECIFIER = "private_dns_specifier";
    private static final String[] DNS_HOSTS = {
            "", "", "dns.adguard-dns.com", "family.adguard-dns.com", "dns.google",
            "one.one.one.one", "family-blocking.cloudflare-dns.com", "dns.quad9.net"
    };
    private static final String[] DNS_LABELS = {
            "Off", "Automatic", "AdGuard", "AdGuard Family", "Google", "Cloudflare",
            "Cloudflare Family", "Quad9"
    };
    private SharedPreferences choices;
    private final TextView[] columnChoices = new TextView[4];
    private TextView animationsChoice;
    private TextView advancedChoice;
    private TextView followThemeChoice;
    private TextView dnsChoice;
    private TextView defaultHomeChoice;
    private TextView nightModeChoice, nightStartsChoice, nightEndsChoice;
    private TextView clearCacheChoice;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean clearingCache;
    private LinearLayout categoryRail, categoryContent; private TextView clearCacheHeader, appsPerRowValue; private android.widget.ScrollView categoryScroll;
    private View[] railItems;
    private View[] categoryCards;
    private int selectedCategory;
    private int[] focusMemory = new int[9];
    private Bitmap settingsBackground;
    private int columns;
    private boolean animations;
    private boolean advancedEffects;
    private TextView screensaverStyleChoice, photoSourceChoice, photoStorageChoice, photoIntervalChoice, calendarChoice, homeCalendarChoice, calendarTextSizeChoice, refreshPhotosChoice, startAfterChoice;
    private final Runnable photoStatusPoll = new Runnable() { public void run() { updatePhotoSummary(); mainHandler.postDelayed(this, 2500); } };
    private final Runnable weatherStatusPoll = new Runnable() { public void run() { updateScreensaverRows(); mainHandler.postDelayed(this, 2500); } };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        // prefs first: building the categories reads them (2.11.5 crashed here)
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE); columns = Config.load(this).columns;
        {Config bg=Config.load(this);SettingsStyle.photoAccent=0;if(bg.wallpaper!=null)loadSettingsBackground(bg.wallpaper);}
        setContentView(R.layout.activity_launcher_settings);
        buildSettingsCategories();
        Fonts.apply(findViewById(R.id.settingsRoot), Fonts.SETTINGS);
        {TextView title=findViewById(R.id.settingsHeader);title.setText("Raider Launcher Settings");TextView ver=findViewById(R.id.versionChip);ver.setBackground(null);ver.setPadding(0,0,0,0);ver.setTextColor(0x99CAC4D0);ver.setText("v"+BuildConfig.VERSION_NAME);}
        clearCacheHeader.setFocusable(true);clearCacheHeader.setFocusableInTouchMode(false);clearCacheHeader.setClickable(true);
        clearCacheHeader.setBackground(SettingsStyle.glow(this,SettingsStyle.accent(this),SettingsStyle.SURFACE,false,true));clearCacheHeader.setOnFocusChangeListener((v,f)->{v.setBackground(SettingsStyle.glow(this,SettingsStyle.accent(this),SettingsStyle.SURFACE,f,true));SettingsStyle.scale(v,f);});
        TextView versionChip=findViewById(R.id.versionChip);versionChip.setText(BuildConfig.VERSION_NAME);versionChip.setBackground(SettingsStyle.pill(this,SettingsStyle.accent(this)));
        Config config = Config.load(this);
        columns = config.columns;
        animations = config.animations;
        advancedEffects = config.animations && config.advancedEffects;

        int[] ids = {R.id.columns4, R.id.columns5, R.id.columns6, R.id.columns7};
        for (int i = 0; i < ids.length; i++) {
            final int value = i + 4;
            columnChoices[i] = findViewById(ids[i]);
            columnChoices[i].setOnClickListener(v -> {
                if (columns == value) return;
                columns = value;
                choices.edit().putInt("columns", value).apply();
                setResult(RESULT_OK);
                refresh();
            });
        }
        animationsChoice = findViewById(R.id.animationsChoice);
        animationsChoice.setOnClickListener(v -> {
            animations = !animations;
            if (!animations) advancedEffects = false;
            choices.edit().putBoolean("animations", animations)
                    .putBoolean("advanced_effects", advancedEffects).apply();
            setResult(RESULT_OK);
            refresh();
        });
        advancedChoice = findViewById(R.id.advancedChoice);
        advancedChoice.setOnClickListener(v -> {
            if (advancedEffects) {
                setAdvanced(false);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.advanced_effects)
                        .setMessage(R.string.advanced_effects_warning)
                        .setPositiveButton(R.string.enable, (dialog, which) -> setAdvanced(true))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
        followThemeChoice = findViewById(R.id.followThemeChoice);
        followThemeChoice.setOnClickListener(v -> {
            if (!hasOnTvChoices()) return;
            choices.edit().remove("columns").remove("animations").remove("advanced_effects").commit();
            Config current = Config.load(this);
            columns = current.columns;
            animations = current.animations;
            advancedEffects = current.animations && current.advancedEffects;
            setResult(RESULT_OK);
            refresh();
        });
        defaultHomeChoice = findViewById(R.id.defaultHomeChoice);
        defaultHomeChoice.setOnClickListener(v -> {
            Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
            open(new Intent(Settings.ACTION_HOME_SETTINGS));
        });
        dnsChoice = findViewById(R.id.dnsChoice);
        dnsChoice.setOnClickListener(v -> showDnsChoices());
        clearCacheChoice = findViewById(R.id.clearCacheChoice);
        clearCacheChoice.setOnClickListener(v -> clearAppCache());
        clearCacheChoice.setText(R.string.storage_summary_unavailable);
        findViewById(R.id.tvSettingsChoice).setOnClickListener(v ->
                open(new Intent(Settings.ACTION_SETTINGS)));
        findViewById(R.id.resetOrderChoice).setOnClickListener(v ->
                new AlertDialog.Builder(this).setTitle(R.string.reset_order)
                        .setMessage(R.string.reset_order_confirm)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            getSharedPreferences("app_layout", MODE_PRIVATE).edit()
                                    .remove("custom_order").remove("ordered_components").apply();
                            Toast.makeText(this, R.string.reset_order, Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                        }).setNegativeButton(android.R.string.cancel, null).show());
        findViewById(R.id.unhideAppsChoice).setOnClickListener(v -> showHiddenApps());
        TextView moduleCards = findViewById(R.id.moduleCardsChoice);
        if (!config.moduleCards) moduleCards.setVisibility(View.GONE);
        moduleCards.setOnClickListener(v -> { setResult(RESULT_OK, new Intent().putExtra("open_module_cards", true)); finish(); });
        TextView weatherChoice=findViewById(R.id.weatherChoice);
        weatherChoice.setOnClickListener(v->{choices.edit().putBoolean("weather_on_home",!choices.getBoolean("weather_on_home",true)).apply();refresh();});
        findViewById(R.id.pillLabelsChoice).setOnClickListener(v->{choices.edit().putBoolean("pill_labels",!choices.getBoolean("pill_labels",false)).apply();refresh();});
        View accentChoice = findViewById(R.id.accentChoice);
        accentChoice.setOnClickListener(v -> chooseAccent());
        nightModeChoice=findViewById(R.id.nightModeChoice);
        nightModeChoice.setOnClickListener(v->{choices.edit().putBoolean("night_mode",!choices.getBoolean("night_mode",true)).apply();refresh();});
        nightStartsChoice=findViewById(R.id.nightStartsChoice);
        nightStartsChoice.setOnClickListener(v->chooseNightTime("night_start_min",22*60));
        nightEndsChoice=findViewById(R.id.nightEndsChoice);
        nightEndsChoice.setOnClickListener(v->chooseNightTime("night_end_min",6*60));
        screensaverStyleChoice=findViewById(R.id.screensaverStyleChoice);
        screensaverStyleChoice.setOnClickListener(v -> chooseScreensaverStyle());
        photoSourceChoice=findViewById(R.id.photoSourceChoice);
        photoSourceChoice.setOnClickListener(v -> choosePhotoSource());
        photoStorageChoice=findViewById(R.id.photoStorageChoice);
        photoStorageChoice.setOnClickListener(v -> choosePhotoStorage());
        photoIntervalChoice=findViewById(R.id.photoIntervalChoice);
        photoIntervalChoice.setOnClickListener(v -> choosePhotoInterval());
        calendarChoice=findViewById(R.id.calendarChoice);
        calendarChoice.setOnClickListener(v -> { boolean on=choices.getBoolean("dream_calendar",true);choices.edit().putBoolean("dream_calendar",!on).apply();refresh(); });
        homeCalendarChoice=findViewById(R.id.homeCalendarChoice);
        homeCalendarChoice.setOnClickListener(v -> { boolean on=choices.getBoolean("home_hindu_calendar",true);choices.edit().putBoolean("home_hindu_calendar",!on).apply();refresh(); });
        calendarTextSizeChoice=findViewById(R.id.calendarTextSizeChoice);
        calendarTextSizeChoice.setOnClickListener(v -> chooseCalendarTextSize());
        refreshPhotosChoice=findViewById(R.id.refreshPhotosChoice);
        refreshPhotosChoice.setOnClickListener(v -> { PhotoStore.schedule(this,true); Toast.makeText(this,R.string.photo_refresh_scheduled,Toast.LENGTH_SHORT).show();updatePhotoSummary();mainHandler.postDelayed(photoStatusPoll,2500); });
        startAfterChoice=findViewById(R.id.startAfterChoice);
        if (Build.VERSION.SDK_INT < 23 || !Settings.System.canWrite(this)) startAfterChoice.setVisibility(View.GONE);
        else startAfterChoice.setOnClickListener(v -> chooseStartAfter());
        findViewById(R.id.startScreensaverChoice).setOnClickListener(v -> startActivity(new Intent(this,ScreensaverActivity.class)));
        refresh();
        if (railItems.length > 0) railItems[0].requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStorageSummary();
        updatePhotoSummary();
        updateScreensaverRows();
        mainHandler.removeCallbacks(weatherStatusPoll);
        mainHandler.postDelayed(weatherStatusPoll,2500);
    }

    @Override protected void onPause(){mainHandler.removeCallbacks(photoStatusPoll);mainHandler.removeCallbacks(weatherStatusPoll);super.onPause();}


    private void buildSettingsCategories() {
        categoryRail = findViewById(R.id.categoryRail);
        categoryContent = findViewById(R.id.categoryContent);
        categoryScroll = findViewById(R.id.categoryScroll);
        categoryRail.setBackground(null);categoryRail.setClipChildren(false);categoryRail.setClipToPadding(false); categoryRail.setPadding(dp(4),dp(4),dp(4),dp(4));
        clearCacheHeader = findViewById(R.id.clearCacheHeader); clearCacheHeader.setBackground(SettingsStyle.focus(this,SettingsStyle.accent(this),false)); SettingsStyle.focusAnim(clearCacheHeader); clearCacheHeader.setOnClickListener(v -> clearAppCache());
        categoryScroll.getViewTreeObserver().addOnGlobalFocusChangeListener((oldFocus,newFocus) -> keepFocusedRowVisible(newFocus));
        String[] names = {"Layout", "Look", "Screensaver", "Photos", "Calendar", "Weather", "Apps", "DNS", "Night", "Kids", "About"};
        int[] icons = categoryIcons = new int[]{R.drawable.ic_cat_home,R.drawable.ic_clean,R.drawable.ic_cat_screensaver,R.drawable.ic_wallpaper,R.drawable.ic_cat_calendar,R.drawable.ic_cat_weather,R.drawable.ic_cat_apps,R.drawable.ic_cat_network,R.drawable.ic_cat_night,R.drawable.ic_cat_kids,R.drawable.ic_cat_about};
        railItems=new View[names.length]; categoryCards=new View[names.length];
        View legacy=LayoutInflater.from(this).inflate(R.layout.legacy_launcher_settings,null,false); // root is a ScrollView; only findViewById is used
        int[][] groups={{R.id.pillLabelsChoice,R.id.moduleCardsChoice,R.id.unhideAppsChoice,R.id.defaultHomeChoice,R.id.resetOrderChoice},{R.id.animationsChoice,R.id.advancedChoice,R.id.accentChoice,R.id.followThemeChoice},{R.id.screensaverStyleChoice,R.id.startAfterChoice,R.id.startScreensaverChoice},{R.id.photoSourceChoice,R.id.photoIntervalChoice,R.id.photoStorageChoice,R.id.refreshPhotosChoice},{R.id.homeCalendarChoice,R.id.calendarTextSizeChoice,R.id.calendarChoice},{R.id.weatherChoice},{R.id.tvSettingsChoice,R.id.clearCacheChoice},{R.id.dnsChoice},{R.id.nightModeChoice,R.id.nightStartsChoice,R.id.nightEndsChoice},{},{}};
        for(int category=0;category<names.length;category++){
            final int index=category;
            TextView rail=new TextView(this);railItems[category]=rail;rail.setText(names[category]);android.graphics.drawable.Drawable railIcon=getDrawable(icons[category]).mutate();railIcon.setTint(0xFFE6E1E5);railIcon.setBounds(0,0,dp(20),dp(20));rail.setCompoundDrawablesRelative(railIcon,null,null,null);rail.setCompoundDrawablePadding(dp(8));rail.setGravity(android.view.Gravity.CENTER_VERTICAL);rail.setPadding(dp(14),0,dp(10),0);rail.setTextSize(15);rail.setIncludeFontPadding(false);rail.setSingleLine(true);rail.setTextColor(0xFFF4F1EA);rail.setTypeface(Fonts.of(this,Fonts.SETTINGS,R.font.outfit_medium));rail.setFocusable(true);rail.setClickable(true);rail.setBackground(SettingsStyle.focus(this,SettingsStyle.accent(this),false));LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,dp(36));if(category<names.length-1)rlp.bottomMargin=dp(2);categoryRail.addView(rail,rlp);rail.setId(View.generateViewId());if(index==0)rail.setNextFocusUpId(R.id.clearCacheHeader);rail.setOnClickListener(v->selectCategory(index,true));rail.setOnFocusChangeListener((v,f)->{v.setBackground(SettingsStyle.rail(this,SettingsStyle.accent(this),index==selectedCategory,f));((TextView)v).setTextColor(f||index==selectedCategory?0xFFFFFFFF:SettingsStyle.SECONDARY);SettingsStyle.scale(v,f);});
            LinearLayout card=new LinearLayout(this);categoryCards[category]=card;card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(12),dp(10),dp(12),dp(10));card.setClipChildren(false);card.setClipToPadding(false);card.setBackground(SettingsStyle.card(this));LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,-2);clp.bottomMargin=0;categoryContent.addView(card,clp);
            if(category==9){TextView t=categoryText("Coming soon - see docs/kids-lock-plan.md",18,0xFFA9ABC7);card.addView(t);continue;}
            if(category==10){String link="github.com/apukale/cleanyourtv";String head="Raider Launcher "+BuildConfig.VERSION_NAME+" ("+BuildConfig.VERSION_CODE+")\nMade by apukale\n";android.text.SpannableString about=new android.text.SpannableString(head+link+"\n"+getPackageName()+"\nWeather data by Open-Meteo.com; Photos from Bing, Unsplash, NASA, Wikimedia Commons, the Art Institute of Chicago and The Met\n\n"+getString(R.string.home_settings_help)+"\n\n"+getString(R.string.home_restore_note));about.setSpan(new ForegroundColorSpan(SettingsStyle.value(this)),head.length(),head.length()+link.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);TextView t=categoryText("",16,0xFFF4F1EA);t.setText(about);t.setIncludeFontPadding(false);LinearLayout aboutRow=new LinearLayout(this);aboutRow.setOrientation(LinearLayout.HORIZONTAL);aboutRow.addView(t,new LinearLayout.LayoutParams(0,android.view.ViewGroup.LayoutParams.WRAP_CONTENT,1f));LinearLayout qrCol=new LinearLayout(this);qrCol.setOrientation(LinearLayout.VERTICAL);qrCol.setGravity(android.view.Gravity.CENTER_HORIZONTAL);qrCol.setPadding(dp(12),dp(8),dp(4),0);android.widget.ImageView qr=new android.widget.ImageView(this);qr.setImageResource(R.drawable.about_qr);qr.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);qr.setContentDescription("QR code for https://github.com/apukale/cleanyourtv");android.graphics.drawable.Drawable qd=qr.getDrawable();if(qd instanceof android.graphics.drawable.BitmapDrawable)((android.graphics.drawable.BitmapDrawable)qd).setFilterBitmap(false);qrCol.addView(qr,new LinearLayout.LayoutParams(dp(112),dp(112)));TextView scan=categoryText("Scan for GitHub",13,SettingsStyle.SECONDARY);scan.setPadding(0,dp(6),0,0);scan.setIncludeFontPadding(false);qrCol.addView(scan);aboutRow.addView(qrCol,new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT,android.view.ViewGroup.LayoutParams.WRAP_CONTENT));card.addView(aboutRow);continue;}
            if(category==0){LinearLayout apr=new LinearLayout(this);apr.setOrientation(LinearLayout.HORIZONTAL);apr.setGravity(android.view.Gravity.CENTER_VERTICAL);apr.setPadding(dp(14),0,dp(14),0);TextView aprTitle=categoryText("Apps per row",17,0xFFF4F1EA);aprTitle.setPadding(0,0,0,0);aprTitle.setIncludeFontPadding(false);aprTitle.setTypeface(Fonts.of(this,Fonts.SETTINGS,R.font.outfit_medium));apr.addView(aprTitle,new LinearLayout.LayoutParams(0,android.view.ViewGroup.LayoutParams.WRAP_CONTENT,1f));appsPerRowValue=categoryText(columns+" apps",16,SettingsStyle.accent(this));appsPerRowValue.setPadding(0,0,0,0);appsPerRowValue.setIncludeFontPadding(false);apr.addView(appsPerRowValue);card.addView(apr,new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,dp(40)));LinearLayout choicesRow=new LinearLayout(this);choicesRow.setOrientation(LinearLayout.HORIZONTAL);int[] columnIds={R.id.columns4,R.id.columns5,R.id.columns6,R.id.columns7};for(int id:columnIds){View choice=legacy.findViewById(id);ViewGroupTools.detach(choice);if(choice instanceof TextView){((TextView)choice).setTextColor(0xFFF4F1EA);((TextView)choice).setTypeface(Fonts.of(this,Fonts.SETTINGS,R.font.outfit_medium));}LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(40),1f);lp.setMargins(dp(4),0,dp(4),dp(4));choice.setBackground(SettingsStyle.focus(this,SettingsStyle.accent(this),false));SettingsStyle.focusAnim(choice);choicesRow.addView(choice,lp);}card.addView(choicesRow);}
            boolean resetShown=false;
            for(int id:groups[category]){if(id==R.id.followThemeChoice)addFontsEntry(card);if(id==R.id.clearCacheChoice)addInputNamesRow(card);View row=legacy.findViewById(id);if(row==null)continue;ViewGroupTools.detach(row);if(isDestructive(id)&&!resetShown){resetShown=true;TextView cap=categoryText("Reset",14,0xFFE8907E);cap.setPadding(dp(14),dp(18),dp(14),dp(6));cap.setIncludeFontPadding(false);card.addView(cap);}LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,android.view.ViewGroup.LayoutParams.WRAP_CONTENT);lp.bottomMargin=dp(4);row.setBackground(SettingsStyle.focus(this,SettingsStyle.accent(this),false));if(row instanceof TextView){TextView t=(TextView)row;t.setTextColor(0xFFF4F1EA);t.setTextSize(17);t.setTypeface(Fonts.of(this,Fonts.SETTINGS,R.font.outfit_medium));t.setPadding(dp(16),dp(6),dp(16),dp(6));t.setMinHeight(dp(rowHeight));t.setMinimumHeight(dp(rowHeight));t.setSingleLine(false);t.setMaxLines(8);t.setEllipsize(null);t.setIncludeFontPadding(false);t.setGravity(android.view.Gravity.CENTER_VERTICAL|android.view.Gravity.START);t.setFocusable(true);t.setClickable(true);SettingsStyle.focusAnim(t);}card.addView(row,lp);if(id==R.id.advancedChoice)card.addView(hint(getString(R.string.advanced_effects_warning)));if(id==R.id.followThemeChoice)card.addView(hint(getString(R.string.follow_theme_hint)));}
            if(category==8)addNightNowRow(card);
        }
        lookCard=(LinearLayout)categoryCards[1];buildFontsCard();
        selectCategory(0,false);if(getIntent().getBooleanExtra("open_fonts",false)){getIntent().removeExtra("open_fonts");selectCategory(1,false);showFonts();}
        {View sr=findViewById(R.id.settingsRoot);if(settingsBackground!=null){BitmapDrawable image=new BitmapDrawable(getResources(),settingsBackground);image.setGravity(android.view.Gravity.FILL);image.setFilterBitmap(true);sr.setBackground(new LayerDrawable(new android.graphics.drawable.Drawable[]{image,SettingsStyle.scrim()}));}else sr.setBackground(SettingsStyle.fallbackBackground());}
    }
    private TextView categoryText(String text,int size,int color){TextView view=new TextView(this);view.setText(text);view.setTextColor(color);view.setTextSize(size);view.setTypeface(Fonts.of(this,Fonts.SETTINGS,R.font.outfit_regular));view.setPadding(dp(8),dp(8),dp(8),dp(14));return view;}
    private static final class ViewGroupTools{static void detach(View view){if(view.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)view.getParent()).removeView(view);}}
    private void selectCategory(int index,boolean requestFocus){if(fontsCard!=null&&categoryCards[1]==fontsCard){categoryCards[1]=lookCard;fontsCard.setVisibility(View.GONE);}selectedCategory=index;if(categoryScroll!=null)categoryScroll.scrollTo(0,0);for(int i=0;i<categoryCards.length;i++){categoryCards[i].setVisibility(i==index?View.VISIBLE:View.GONE);railItems[i].setBackground(SettingsStyle.rail(this,SettingsStyle.accent(this),i==index,railItems[i].hasFocus()));((TextView)railItems[i]).setTextColor(i==index?0xFFFFFFFF:SettingsStyle.SECONDARY);}if(requestFocus)railItems[index].requestFocus();if(categoryScroll!=null)categoryScroll.post(this::fitCurrentCard);}
    private void loadSettingsBackground(java.io.File file){try{BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getAbsolutePath(),bounds);if(bounds.outWidth<=0)return;int sample=1;while(sample<64&&bounds.outWidth/(sample*2)>=240)sample*=2;BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=sample;Bitmap raw=BitmapFactory.decodeFile(file.getAbsolutePath(),options);if(raw==null)return;int w=raw.getWidth(),h=raw.getHeight();float target=(float)getResources().getDisplayMetrics().widthPixels/(float)getResources().getDisplayMetrics().heightPixels;int cropW=w,cropH=h;if((float)w/h>target)cropW=Math.max(1,(int)(h*target));else cropH=Math.max(1,(int)(w/target));Bitmap cropped=Bitmap.createBitmap(raw,(w-cropW)/2,(h-cropH)/2,cropW,cropH);Bitmap small=Bitmap.createScaledBitmap(cropped,240,Math.max(1,Math.round(240/target)),true);if(cropped!=raw)cropped.recycle();if(small!=raw)raw.recycle();
        // ~1/8 of 1080p; pre-blurred once here, never live
        try{androidx.palette.graphics.Palette p=androidx.palette.graphics.Palette.from(small).maximumColorCount(16).generate();androidx.palette.graphics.Palette.Swatch s=p.getVibrantSwatch();if(s==null)s=p.getLightVibrantSwatch();if(s==null)s=p.getDominantSwatch();SettingsStyle.photoAccent=s==null?0:SettingsStyle.fromSwatch(s.getRgb());}catch(RuntimeException e){SettingsStyle.photoAccent=0;}
        w=small.getWidth();h=small.getHeight();int[] pixels=new int[w*h];small.getPixels(pixels,0,w,0,0,w,h);small.recycle();BoxBlur.blur(pixels,w,h,8,2);Bitmap blurred=Bitmap.createBitmap(w,h,Bitmap.Config.RGB_565);blurred.setPixels(pixels,0,w,0,0,w,h);settingsBackground=blurred;}catch(Throwable t){SettingsStyle.photoAccent=0;}}
    private void applyAccentToRows(View view){if(view.isFocusable())view.setBackground(SettingsStyle.focus(this,SettingsStyle.accent(this),view.hasFocus()));if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++)applyAccentToRows(group.getChildAt(i));}}
    private View findFocusable(View view){if(view.isFocusable()&&view.getVisibility()==View.VISIBLE)return view;if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View found=findFocusable(group.getChildAt(i));if(found!=null)return found;}}return null;}
    private void chooseAccent(){String[] names={"Marigold","Teal","Rose","Silver"};new AlertDialog.Builder(this).setTitle("Accent colour").setSingleChoiceItems(names,Math.max(0,java.util.Arrays.asList(names).indexOf(choices.getString("accent_color","Marigold"))),(d,w)->{choices.edit().putString("accent_color",names[w]).apply();d.dismiss();for(int i=0;i<railItems.length;i++)railItems[i].setBackground(SettingsStyle.focus(this,SettingsStyle.accent(this),i==selectedCategory));applyAccentToRows(categoryContent);TextView chip=findViewById(R.id.versionChip);chip.setBackground(SettingsStyle.pill(this,SettingsStyle.accent(this)));}).setNegativeButton(android.R.string.cancel,null).show();}
    @Override public boolean dispatchKeyEvent(KeyEvent event){if(event.getAction()==KeyEvent.ACTION_DOWN){int key=event.getKeyCode();View cur=getCurrentFocus();boolean onHeader=clearCacheHeader!=null&&clearCacheHeader.isFocused();boolean inContent=cur!=null&&inside(categoryContent,cur);View first=findFocusable(categoryCards[selectedCategory]);
        if(key==KeyEvent.KEYCODE_BACK){if(fontsCard!=null&&categoryCards[1]==fontsCard&&inContent){hideFonts();return true;}if(inContent||onHeader){railItems[selectedCategory].requestFocus();return true;}finish();return true;}
        if(onHeader){if(key==KeyEvent.KEYCODE_DPAD_DOWN){View back=headerReturn;headerReturn=null;if(back!=null&&back.isShown()&&back.isFocusable())back.requestFocus();else if(first!=null)first.requestFocus();return true;}if(key==KeyEvent.KEYCODE_DPAD_LEFT){railItems[selectedCategory].requestFocus();return true;}if(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_RIGHT)return true;}
        if(inContent&&(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)){int dir=key==KeyEvent.KEYCODE_DPAD_UP?View.FOCUS_UP:key==KeyEvent.KEYCODE_DPAD_DOWN?View.FOCUS_DOWN:key==KeyEvent.KEYCODE_DPAD_LEFT?View.FOCUS_LEFT:View.FOCUS_RIGHT;View next=cur.focusSearch(dir);if(next==null||!inside(categoryContent,next)){if(key==KeyEvent.KEYCODE_DPAD_UP&&clearCacheHeader!=null)focusHeaderFrom(cur);else if(key==KeyEvent.KEYCODE_DPAD_LEFT)railItems[selectedCategory].requestFocus();return true;}}
        if(key==KeyEvent.KEYCODE_DPAD_RIGHT&&categoryRail.findFocus()!=null){if(first!=null)first.requestFocus();return true;}
        if(categoryRail.findFocus()!=null&&key==KeyEvent.KEYCODE_DPAD_UP&&selectedCategory==0&&clearCacheHeader!=null){focusHeaderFrom(cur);return true;}if(categoryRail.findFocus()!=null&&(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN)){selectCategory(Math.floorMod(selectedCategory+(key==KeyEvent.KEYCODE_DPAD_DOWN?1:-1),railItems.length),true);return true;}
        if(categoryRail.findFocus()!=null&&key==KeyEvent.KEYCODE_DPAD_LEFT)return true;}return super.dispatchKeyEvent(event);}
    @Override protected void onDestroy(){if(settingsBackground!=null&&!settingsBackground.isRecycled()){View root=findViewById(R.id.settingsRoot);if(root!=null)root.setBackground(null);settingsBackground.recycle();}super.onDestroy();}
    private void refreshScreensaverLabels(){updateScreensaverRows();}
    private void updateScreensaverRows(){if(screensaverStyleChoice==null)return;setToggle(findViewById(R.id.animationsChoice),animations);setToggle(findViewById(R.id.advancedChoice),advancedEffects);setToggle(findViewById(R.id.pillLabelsChoice),choices.getBoolean("pill_labels",false));setToggle(findViewById(R.id.weatherChoice),choices.getBoolean("weather_on_home",true));setToggle(findViewById(R.id.nightModeChoice),choices.getBoolean("night_mode",true));setToggle(findViewById(R.id.calendarChoice),choices.getBoolean("dream_calendar",true));setToggle(findViewById(R.id.homeCalendarChoice),choices.getBoolean("home_hindu_calendar",true));View accentView=findViewById(R.id.accentChoice);if(accentView instanceof TextView)((TextView)accentView).setText("Accent colour: "+choices.getString("accent_color","Marigold"));boolean photos=choices.getBoolean("dream_photos",true);screensaverStyleChoice.setText(getString(R.string.screensaver_style,getString(photos?R.string.screensaver_photos:R.string.screensaver_clock_only)));java.util.Set<String> srcs=PhotoStore.selectedSources(this);int onCount=0;StringBuilder srcLabel=new StringBuilder();for(String s:PhotoSources.KNOWN)if(srcs.contains(s)&&ExtraPhotoSources.available(s)){onCount++;if(onCount<=2){if(srcLabel.length()>0)srcLabel.append(", ");srcLabel.append(ExtraPhotoSources.label(s));}}if(onCount>2)srcLabel.append(" +"+(onCount-2));photoSourceChoice.setText(getString(R.string.photo_source,srcLabel.toString()));photoStorageChoice.setText(getString(R.string.photo_storage,(int)(PhotoStore.cap(this)/(1024L*1024L))));long ms=choices.getLong("photo_interval_ms",60_000L);int mins=ms<=30_000?30:ms<=60_000?1:ms<=120_000?2:5;photoIntervalChoice.setText(getString(R.string.photo_interval,getString(R.string.screensaver_minutes,mins)));calendarChoice.setText(getString(R.string.hindu_calendar,getString(choices.getBoolean("dream_calendar",true)?R.string.on:R.string.off)));homeCalendarChoice.setText(getString(R.string.home_hindu_calendar,getString(choices.getBoolean("home_hindu_calendar",true)?R.string.on:R.string.off)));int size=choices.getInt("home_calendar_text_size",2);int sizeLabel=size==0?R.string.calendar_size_small:size==1?R.string.calendar_size_medium:R.string.calendar_size_large;calendarTextSizeChoice.setText(getString(R.string.calendar_text_size,getString(sizeLabel)));TextView weather=findViewById(R.id.weatherChoice);if(weather!=null)weather.setText(getString(R.string.weather_on_home,getString(choices.getBoolean("weather_on_home",true)?R.string.on:R.string.off))+"\n"+weatherStatus());TextView pills=findViewById(R.id.pillLabelsChoice);if(pills!=null)pills.setText(getString(R.string.pill_labels,getString(choices.getBoolean("pill_labels",false)?R.string.pill_labels_text:R.string.pill_labels_icons)));if(nightModeChoice!=null){nightModeChoice.setText(getString(R.string.night_mode,getString(choices.getBoolean("night_mode",true)?R.string.on:R.string.off)));nightStartsChoice.setText(getString(R.string.night_starts,formatNightTime(choices.getInt("night_start_min",22*60))));nightEndsChoice.setText(getString(R.string.night_ends,formatNightTime(choices.getInt("night_end_min",6*60))));}if(startAfterChoice.getVisibility()==View.VISIBLE){int timeout=Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_OFF_TIMEOUT,600000);int current=Math.max(1,Math.round(timeout/60000f));startAfterChoice.setText(getString(R.string.start_after,getString(R.string.screensaver_minutes,current)));}formatRows();updateNightNow();}
    private static final int[] ACTION_ROWS={R.id.followThemeChoice,R.id.moduleCardsChoice,R.id.resetOrderChoice,R.id.unhideAppsChoice,R.id.defaultHomeChoice,R.id.accentChoice,R.id.screensaverStyleChoice,R.id.photoIntervalChoice,R.id.photoSourceChoice,R.id.photoStorageChoice,R.id.refreshPhotosChoice,R.id.startAfterChoice,R.id.startScreensaverChoice,R.id.calendarTextSizeChoice,R.id.clearCacheChoice,R.id.tvSettingsChoice,R.id.dnsChoice,R.id.nightStartsChoice,R.id.nightEndsChoice};
    private static boolean isActionRow(int id){for(int a:ACTION_ROWS)if(a==id)return true;return false;}
    private static boolean isDestructive(int id){return id==R.id.followThemeChoice||id==R.id.resetOrderChoice||id==R.id.clearCacheChoice;}
    private TextView nightNowChoice;
    /** Start now / Stop now: a manual night mode override that lasts until the next scheduled end (or start). */
    private void addNightNowRow(LinearLayout card){TextView t=new TextView(this);t.setId(R.id.nightNowChoice);nightNowChoice=t;t.setBackground(SettingsStyle.focus(this,SettingsStyle.accent(this),false));t.setTextColor(0xFFF4F1EA);t.setTextSize(17);t.setTypeface(Fonts.of(this,Fonts.SETTINGS,R.font.outfit_medium));t.setPadding(dp(16),dp(6),dp(16),dp(6));t.setMinHeight(dp(rowHeight));t.setMinimumHeight(dp(rowHeight));t.setSingleLine(false);t.setMaxLines(8);t.setEllipsize(null);t.setIncludeFontPadding(false);t.setGravity(android.view.Gravity.CENTER_VERTICAL|android.view.Gravity.START);t.setFocusable(true);t.setClickable(true);SettingsStyle.focusAnim(t);t.setOnClickListener(v->{NightState.toggleNow(choices,System.currentTimeMillis());setResult(RESULT_OK);updateNightNow();});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,android.view.ViewGroup.LayoutParams.WRAP_CONTENT);lp.bottomMargin=dp(4);card.addView(t,lp);updateNightNow();}
    private void updateNightNow(){if(nightNowChoice==null)return;if(choices==null)choices=getSharedPreferences(Config.SETTINGS_NAME,MODE_PRIVATE);long now=System.currentTimeMillis();boolean active=NightState.isActive(choices,now);java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("h:mm a",java.util.Locale.getDefault());String value;if(active){long until=NightState.nextChangeMillis(choices,now);value=until==Long.MAX_VALUE?"On":"On until "+f.format(new java.util.Date(until));}else{value=NightState.overrideActive(choices,now)?"Off until "+f.format(new java.util.Date(choices.getLong(NightState.OVERRIDE_UNTIL,0L))):"Off";}NightEffects.apply(this,findViewById(R.id.settingsRoot),active);nightNowChoice.setText((active?"Stop now":"Start now")+": "+value);formatRow(R.id.nightNowChoice);}
    private void formatRows(){int[] ids={R.id.animationsChoice,R.id.advancedChoice,R.id.followThemeChoice,R.id.pillLabelsChoice,R.id.moduleCardsChoice,R.id.resetOrderChoice,R.id.unhideAppsChoice,R.id.defaultHomeChoice,R.id.accentChoice,R.id.screensaverStyleChoice,R.id.photoIntervalChoice,R.id.photoSourceChoice,R.id.photoStorageChoice,R.id.refreshPhotosChoice,R.id.startAfterChoice,R.id.startScreensaverChoice,R.id.homeCalendarChoice,R.id.calendarTextSizeChoice,R.id.calendarChoice,R.id.weatherChoice,R.id.clearCacheChoice,R.id.tvSettingsChoice,R.id.dnsChoice,R.id.nightModeChoice,R.id.nightStartsChoice,R.id.nightEndsChoice};for(int id:ids)formatRow(id);}
    /** Title (and optional detail line) on the left, current value + toggle/chevron on the right. Re-parses only when the row text was set afresh. */
    private void formatRow(int id){View value=findViewById(id);if(!(value instanceof TextView))return;TextView row=(TextView)value;String raw=row.getText().toString();Object tag=row.getTag();String title,val,sub;
        if(tag instanceof String[]&&raw.equals(((String[])tag)[0])){String[] st=(String[])tag;title=st[1];val=st[2];sub=st[3];}
        else{int nl=raw.indexOf('\n');String first=nl<0?raw:raw.substring(0,nl);String rest=nl<0?"":raw.substring(nl+1).trim();int c=first.indexOf(": ");if(c>=0){title=first.substring(0,c).trim();val=first.substring(c+2).trim();sub=rest;}else{title=first.trim();int nl2=rest.indexOf('\n');val=nl2<0?rest:rest.substring(0,nl2).trim();sub=nl2<0?"":rest.substring(nl2+1).trim();}}
        SpannableString styled=new SpannableString(sub.isEmpty()?title:title+"\n"+sub);styled.setSpan(new AbsoluteSizeSpan(17,true),0,title.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);styled.setSpan(new ForegroundColorSpan(0xFFF4F1EA),0,title.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);if(!sub.isEmpty()){styled.setSpan(new AbsoluteSizeSpan(14,true),title.length()+1,styled.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);styled.setSpan(new ForegroundColorSpan(0xFFA9ABC7),title.length()+1,styled.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}
        row.setText(styled);row.setTag(new String[]{styled.toString(),title,val,sub});
        android.graphics.drawable.Drawable end=row.getCompoundDrawablesRelative()[2];android.graphics.drawable.Drawable trailing=end instanceof RaiderTheme.ValueDrawable?((RaiderTheme.ValueDrawable)end).trailing:end;if(trailing==null&&isActionRow(id))trailing=RaiderTheme.chevronDrawable(this);
        row.setCompoundDrawablePadding(dp(12));row.setCompoundDrawablesRelativeWithIntrinsicBounds(rowIcon(row),null,new RaiderTheme.ValueDrawable(this,val,SettingsStyle.value(this),trailing,dp(320)),null);}
    private boolean inside(View parent,View v){while(v!=null){if(v==parent)return true;android.view.ViewParent p=v.getParent();v=p instanceof View?(View)p:null;}return false;}
    private void keepFocusedRowVisible(View f){if(f==null||categoryScroll==null||!inside(categoryContent,f))return;android.graphics.Rect r=new android.graphics.Rect();f.getDrawingRect(r);categoryScroll.offsetDescendantRectToMyCoords(f,r);int m=dp(24),sy=categoryScroll.getScrollY(),h=categoryScroll.getHeight();if(r.top-m<sy)categoryScroll.smoothScrollTo(0,Math.max(0,r.top-m));else if(r.bottom+m>sy+h)categoryScroll.smoothScrollTo(0,r.bottom+m-h);}
    private TextView hint(String text){TextView t=categoryText(text,14,0xFFA9ABC7);t.setPadding(dp(14),dp(2),dp(14),dp(8));t.setIncludeFontPadding(false);return t;}
    private void setToggle(View view,boolean on){if(view instanceof TextView){TextView text=(TextView)view;text.setCompoundDrawablePadding(dp(12));text.setCompoundDrawablesRelativeWithIntrinsicBounds(null,null,SettingsStyle.toggle(this,on,SettingsStyle.accent(this)),null);}}
    private String weatherStatus(){long tried=choices.getLong("weather_last_try_at",0);String error=choices.getString("weather_last_error","");if(!error.isEmpty())return WeatherStatus.format(true,error,"",0);WeatherData d=WeatherClient.cached(this);if(tried==0||d==null)return WeatherStatus.format(false,"","",0);java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("h:mm a",java.util.Locale.getDefault());return WeatherStatus.format(true,"",f.format(new java.util.Date(tried)),d.temperature);}
    private String formatNightTime(int total){int hour24=total/NIGHT_MINUTES_PER_HOUR;int hour=hour24%NIGHT_HOURS_PER_HALF_DAY;if(hour==0)hour=NIGHT_HOURS_PER_HALF_DAY;return getString(R.string.night_time_value,Integer.toString(hour),total%NIGHT_MINUTES_PER_HOUR,getString(hour24< NIGHT_HOURS_PER_HALF_DAY?R.string.night_am:R.string.night_pm));}
    private void chooseNightTime(String key,int fallback){int[] parts={0,0,0};int total=choices.getInt(key,fallback);int h=total/NIGHT_MINUTES_PER_HOUR;parts[0]=h%NIGHT_HOURS_PER_HALF_DAY==0?NIGHT_HOURS_PER_HALF_DAY:h%NIGHT_HOURS_PER_HALF_DAY;parts[1]=total%NIGHT_MINUTES_PER_HOUR;parts[2]=h>=NIGHT_HOURS_PER_HALF_DAY?1:0;LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.HORIZONTAL);int pad=dp(NIGHT_PICKER_PADDING_DP);panel.setPadding(pad,pad,pad,pad);TextView[] cells=new TextView[3];final AlertDialog[] dialogRef=new AlertDialog[1];for(int i=0;i<cells.length;i++){final int column=i;TextView cell=new TextView(this);cells[i]=cell;cell.setTextSize(30f);cell.setGravity(android.view.Gravity.CENTER);cell.setFocusable(true);cell.setFocusableInTouchMode(true);cell.setMinWidth(dp(NIGHT_PICKER_CELL_WIDTH_DP));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(NIGHT_PICKER_CELL_HEIGHT_DP),1f);panel.addView(cell,lp);cell.setOnFocusChangeListener((v,focus)->v.setSelected(focus));cell.setOnKeyListener((v,keyCode,event)->{if(event.getAction()!=android.view.KeyEvent.ACTION_DOWN)return false;if(keyCode==android.view.KeyEvent.KEYCODE_DPAD_LEFT||keyCode==android.view.KeyEvent.KEYCODE_DPAD_RIGHT){cells[Math.max(0,Math.min(2,column+(keyCode==android.view.KeyEvent.KEYCODE_DPAD_LEFT?-1:1)))].requestFocus();return true;}if(keyCode==android.view.KeyEvent.KEYCODE_DPAD_UP||keyCode==android.view.KeyEvent.KEYCODE_DPAD_DOWN){int step=keyCode==android.view.KeyEvent.KEYCODE_DPAD_UP?1:-1;if(column==0)parts[0]=Math.floorMod(parts[0]-1+step,NIGHT_HOURS_PER_HALF_DAY)+1;else if(column==1)parts[1]=Math.floorMod(parts[1]+step*NIGHT_MINUTE_STEP,NIGHT_MINUTES_PER_HOUR);else parts[2]=1-parts[2];updateNightPicker(cells,parts);return true;}if(keyCode==android.view.KeyEvent.KEYCODE_DPAD_CENTER||keyCode==android.view.KeyEvent.KEYCODE_ENTER){choices.edit().putInt(key,(parts[0]%NIGHT_HOURS_PER_HALF_DAY+parts[2]*NIGHT_HOURS_PER_HALF_DAY)*NIGHT_MINUTES_PER_HOUR+parts[1]).apply();refresh();if(dialogRef[0]!=null)dialogRef[0].dismiss();return true;}return false;});}updateNightPicker(cells,parts);cells[0].requestFocus();AlertDialog dialog=new AlertDialog.Builder(this).setTitle(R.string.night_time_picker).setView(panel).setPositiveButton(android.R.string.ok,(d,w)->{choices.edit().putInt(key,(parts[0]%NIGHT_HOURS_PER_HALF_DAY+parts[2]*NIGHT_HOURS_PER_HALF_DAY)*NIGHT_MINUTES_PER_HOUR+parts[1]).apply();refresh();}).setNegativeButton(android.R.string.cancel,null).create();dialogRef[0]=dialog;dialog.show();}
    private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+0.5f);}
    private void updateNightPicker(TextView[] cells,int[] parts){cells[0].setText(Integer.toString(parts[0]));cells[1].setText(String.format(java.util.Locale.ROOT,"%02d",parts[1]));cells[2].setText(getString(parts[2]==0?R.string.night_am:R.string.night_pm));}
    private void chooseCalendarTextSize(){String[] labels={getString(R.string.calendar_size_small),getString(R.string.calendar_size_medium),getString(R.string.calendar_size_large)};new AlertDialog.Builder(this).setTitle(R.string.calendar_text_size_title).setSingleChoiceItems(labels,choices.getInt("home_calendar_text_size",2),(d,w)->{choices.edit().putInt("home_calendar_text_size",w).apply();d.dismiss();updateScreensaverRows();}).setNegativeButton(android.R.string.cancel,null).show();}
    private void updatePhotoSummary(){updatePhotoSummaryRaw();formatRow(R.id.refreshPhotosChoice);}
    private void updatePhotoSummaryRaw(){if(refreshPhotosChoice!=null){android.content.SharedPreferences p=getSharedPreferences(Config.SETTINGS_NAME,MODE_PRIVATE);long at=p.getLong("photo_last_refresh_at",0);String when=at==0?"never":new java.text.SimpleDateFormat("MMM d, h:mm a",java.util.Locale.getDefault()).format(new java.util.Date(at));String error=p.getString("photo_last_error","");refreshPhotosChoice.setText(getString(R.string.refresh_photos,PhotoStore.summary(this))+"\nLast refresh "+when+(error.isEmpty()?"":"\nLast error: "+error));formatRows();}}
    private void chooseScreensaverStyle(){String[] labels={getString(R.string.screensaver_clock_only),getString(R.string.screensaver_photos)};new AlertDialog.Builder(this).setTitle(R.string.screensaver_settings).setSingleChoiceItems(labels,choices.getBoolean("dream_photos",true)?1:0,(d,w)->{choices.edit().putBoolean("dream_photos",w==1).apply();d.dismiss();refreshScreensaverLabels();}).setNegativeButton(android.R.string.cancel,null).show();}
    /** One toggle per photo source; sources whose API key is missing are hidden (and skipped by PhotoStore). */
    private void choosePhotoSource(){java.util.ArrayList<String> keys=new java.util.ArrayList<>();for(String s:PhotoSources.KNOWN)if(ExtraPhotoSources.available(s))keys.add(s);String[] labels=new String[keys.size()];boolean[] checked=new boolean[keys.size()];java.util.Set<String> sel=PhotoStore.selectedSources(this);for(int i=0;i<keys.size();i++){labels[i]=ExtraPhotoSources.label(keys.get(i));checked[i]=sel.contains(keys.get(i));}
        new AlertDialog.Builder(this).setTitle(R.string.photo_source_title).setMultiChoiceItems(labels,checked,(d,w,on)->{checked[w]=on;java.util.LinkedHashSet<String> v=new java.util.LinkedHashSet<>();for(String s:sel)if(!keys.contains(s))v.add(s);for(int i=0;i<keys.size();i++)if(checked[i])v.add(keys.get(i));boolean any=false;for(String s:v)if(ExtraPhotoSources.available(s))any=true;if(!any){checked[w]=true;((AlertDialog)d).getListView().setItemChecked(w,true);return;}choices.edit().putString("photo_sources",PhotoSources.serialize(v)).remove("photo_source").apply();updateScreensaverRows();updatePhotoSummary();PhotoStore.schedule(this,false);}).setPositiveButton(android.R.string.ok,(d,w)->PhotoStore.schedule(this,true)).show();}
    private void choosePhotoStorage(){int[] values={100,200,500};String[] labels={"100 MB","200 MB","500 MB"};int current=(int)(PhotoStore.cap(this)/(1024L*1024L)),selected=current==100?0:current==500?2:1;new AlertDialog.Builder(this).setTitle(R.string.photo_storage_title).setSingleChoiceItems(labels,selected,(d,w)->{choices.edit().putInt("photo_storage_mb",values[w]).apply();d.dismiss();PhotoStore.enforceCap(this);updateScreensaverRows();updatePhotoSummary();}).setNegativeButton(android.R.string.cancel,null).show();}
    private void choosePhotoInterval(){int[] values={30_000,60_000,120_000,300_000};String[] labels={"30 s","1 min","2 min","5 min"};long current=choices.getLong("photo_interval_ms",60_000L);int selected=1;for(int i=0;i<values.length;i++)if(values[i]==current)selected=i;new AlertDialog.Builder(this).setTitle(R.string.photo_interval).setSingleChoiceItems(labels,selected,(d,w)->{choices.edit().putLong("photo_interval_ms",values[w]).apply();d.dismiss();refreshScreensaverLabels();}).setNegativeButton(android.R.string.cancel,null).show();}
    private void chooseStartAfter(){int[] values={5,10,15,30};String[] labels={"5 min","10 min","15 min","30 min"};int selected=0;int now=Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_OFF_TIMEOUT,600000);for(int i=0;i<values.length;i++)if(Math.abs(now-values[i]*60000)<60000)selected=i;new AlertDialog.Builder(this).setTitle(R.string.start_after_title).setSingleChoiceItems(labels,selected,(d,w)->{if(Settings.System.canWrite(this))Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_OFF_TIMEOUT,values[w]*60000);d.dismiss();updateScreensaverRows();}).setNegativeButton(android.R.string.cancel,null).show();}

    private void setAdvanced(boolean enabled) {
        advancedEffects = enabled;
        choices.edit().putBoolean("advanced_effects", enabled).apply();
        setResult(RESULT_OK);
        refresh();
    }

    private void refresh() {
        for (int i = 0; i < columnChoices.length; i++) {
            columnChoices[i].setSelected(columns == i + 4);
            if (columnChoices[i] instanceof TextView) ((TextView) columnChoices[i]).setTextColor(columns == i + 4 ? SettingsStyle.accent(this) : 0xFFF4F1EA);
        }
        animationsChoice.setText(getString(R.string.animations_state,
                getString(animations ? R.string.on : R.string.off)));
        advancedChoice.setText(getString(R.string.advanced_effects_state,
                getString(advancedEffects ? R.string.on : R.string.off)));
        advancedChoice.setEnabled(animations);
        advancedChoice.setAlpha(animations ? 1f : 0.5f);
        followThemeChoice.setSelected(!hasOnTvChoices());
        followThemeChoice.setText(getString(R.string.follow_theme) + ": " + (hasOnTvChoices() ? "Custom" : "Theme defaults"));
        View unhideRow = findViewById(R.id.unhideAppsChoice);
        if (unhideRow instanceof TextView) { int hiddenCount = getSharedPreferences("app_layout", MODE_PRIVATE).getStringSet("hidden", java.util.Collections.emptySet()).size(); ((TextView) unhideRow).setText(getString(R.string.unhide_apps) + ": " + (hiddenCount == 0 ? "None hidden" : hiddenCount + " hidden")); }
        if (appsPerRowValue != null) appsPerRowValue.setText(columns + " apps");
        dnsChoice.setText(getString(R.string.network_dns_current, currentDnsValue()));
        defaultHomeChoice.setText(getString(R.string.default_home_current, currentHomeLabel()));
        updateScreensaverRows();
        updatePhotoSummary();
    }

    private boolean hasOnTvChoices() {
        return choices.contains("columns") || choices.contains("animations")
                || choices.contains("advanced_effects");
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }

    private static final class StorageSnapshot {
        final long freeBytes;
        final long totalBytes;
        final Long cacheBytes;

        StorageSnapshot(long freeBytes, long totalBytes, Long cacheBytes) {
            this.freeBytes = freeBytes;
            this.totalBytes = totalBytes;
            this.cacheBytes = cacheBytes;
        }
    }

    private void updateStorageSummary() {
        new Thread(() -> {
            StorageSnapshot snapshot = readStorageSnapshot();
            mainHandler.post(() -> {
                if (isFinishing() || clearCacheChoice == null || clearingCache) return;
                clearCacheChoice.setText(storageSummary(snapshot));formatRow(R.id.clearCacheChoice);
            });
        }, "launcher-storage-stats").start();
    }

    private StorageSnapshot readStorageSnapshot() {
        long freeBytes = -1L;
        long totalBytes = -1L;
        try {
            StorageStatsManager storageStatsManager = getSystemService(StorageStatsManager.class);
            if (storageStatsManager != null) {
                freeBytes = storageStatsManager.getFreeBytes(StorageManager.UUID_DEFAULT);
                totalBytes = storageStatsManager.getTotalBytes(StorageManager.UUID_DEFAULT);
            }
        } catch (Throwable ignored) {
            // Some TV builds do not expose storage stats consistently; use the data partition below.
        }
        if (freeBytes < 0 || totalBytes < 0) {
            try {
                StatFs stat = new StatFs(Environment.getDataDirectory().getAbsolutePath());
                if (freeBytes < 0) freeBytes = stat.getAvailableBytes();
                if (totalBytes < 0) totalBytes = stat.getTotalBytes();
            } catch (Throwable ignored) {
                // Keep whichever values were available.
            }
        }
        return new StorageSnapshot(freeBytes, totalBytes, readCacheBytes());
    }

    private Long readCacheBytes() {
        try {
            AppOpsManager appOps = getSystemService(AppOpsManager.class);
            if (appOps == null || appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(), getPackageName()) != AppOpsManager.MODE_ALLOWED) {
                return null;
            }
            StorageStatsManager stats = getSystemService(StorageStatsManager.class);
            if (stats == null) return null;
            return stats.queryStatsForUser(StorageManager.UUID_DEFAULT, Process.myUserHandle())
                    .getCacheBytes();
        } catch (SecurityException ignored) {
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String storageSummary(StorageSnapshot snapshot) {
        String cache = snapshot.cacheBytes == null
                ? getString(R.string.cache_size_unavailable)
                : Formatter.formatShortFileSize(this, snapshot.cacheBytes);
        if (snapshot.freeBytes < 0 || snapshot.totalBytes < 0) {
            return getString(R.string.storage_free_unavailable, cache);
        }
        return getString(R.string.storage_summary,
                Formatter.formatShortFileSize(this, snapshot.freeBytes),
                Formatter.formatShortFileSize(this, snapshot.totalBytes), cache);
    }

    private void clearAppCache() {
        if (clearingCache) return;
        clearingCache = true;
        clearCacheChoice.setText(R.string.clearing_cache);
        formatRow(R.id.clearCacheChoice);
        if (clearCacheHeader != null) clearCacheHeader.setText("Clearing\u2026");
        new Thread(() -> {
            StorageSnapshot before = readStorageSnapshot();
            boolean cleared = tryFreeStorageAndNotify();
            if (!cleared) cleared = tryAllocateStorage();
            // Allow package manager cache deletion to settle before measuring the result.
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            StorageSnapshot after = readStorageSnapshot();
            final boolean attempted = cleared;
            mainHandler.post(() -> {
                clearingCache = false;
                if (isFinishing()) return;
                clearCacheChoice.setText(storageSummary(after));
                formatRow(R.id.clearCacheChoice);
                if (clearCacheHeader != null) clearCacheHeader.setText("Clear app cache");
                showCacheClearResult(before, after, attempted);
            });
        }, "launcher-clear-cache").start();
    }

    private boolean tryFreeStorageAndNotify() {
        try {
            if (checkSelfPermission("android.permission.CLEAR_APP_CACHE")
                    != PackageManager.PERMISSION_GRANTED) return false;
            PackageManager pm = getPackageManager();
            for (Method method : pm.getClass().getMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if ("freeStorageAndNotify".equals(method.getName()) && params.length == 2
                        && (params[0] == long.class || params[0] == Long.class)) {
                    method.invoke(pm, Long.MAX_VALUE, null);
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // Hidden API enforcement varies by TV firmware. Try the public allocation API next.
        }
        return false;
    }

    private boolean tryAllocateStorage() {
        try {
            StorageManager storageManager = getSystemService(StorageManager.class);
            if (storageManager == null) return false;
            UUID volume = StorageManager.UUID_DEFAULT;
            long bytes = storageManager.getAllocatableBytes(volume);
            storageManager.allocateBytes(volume, bytes);
            return true;
        } catch (IOException | SecurityException ignored) {
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void showCacheClearResult(StorageSnapshot before, StorageSnapshot after, boolean attempted) {
        long beforeFree = before.freeBytes;
        long afterFree = after.freeBytes;
        long delta = beforeFree >= 0 && afterFree >= 0 ? afterFree - beforeFree : 0L;
        boolean negligible = !attempted || beforeFree < 0 || afterFree < 0 || delta < 1024L * 1024L;
        String message;
        if (negligible) {
            message = (beforeFree >= 0 && afterFree >= 0)
                    ? getString(R.string.cache_clear_no_change,
                    Formatter.formatShortFileSize(this, beforeFree),
                    Formatter.formatShortFileSize(this, afterFree))
                    : getString(R.string.cache_clearing_unavailable);
        } else {
            message = getString(R.string.cache_clear_result,
                    Formatter.formatShortFileSize(this, delta),
                    Formatter.formatShortFileSize(this, beforeFree),
                    Formatter.formatShortFileSize(this, afterFree));
        }
        new AlertDialog.Builder(this).setTitle(R.string.clear_app_cache).setMessage(message)
                .setNeutralButton(R.string.open_storage_settings,
                        (dialog, which) -> openStorageSettings())
                .setPositiveButton(android.R.string.ok, null).show();
    }

    private void openStorageSettings() {
        String[] actions = {Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
                Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS, Settings.ACTION_SETTINGS};
        for (String action : actions) {
            try {
                startActivity(new Intent(action));
                return;
            } catch (ActivityNotFoundException | SecurityException ignored) {
                // Try the next supported settings screen on this TV.
            }
        }
        Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
    }

    private String currentHomeLabel() {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolved = getPackageManager().resolveActivity(home,
                android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
        if (resolved == null || resolved.activityInfo == null) return getString(R.string.not_available);
        if (getPackageName().equals(resolved.activityInfo.packageName)) return getString(R.string.this_launcher_home);
        CharSequence label = resolved.loadLabel(getPackageManager());
        return label == null ? resolved.activityInfo.packageName : label.toString();
    }

    private String currentDnsValue() {
        String mode = Global.getString(getContentResolver(), PRIVATE_DNS_MODE);
        if ("off".equals(mode)) return getString(R.string.dns_off);
        if (!"hostname".equals(mode)) return getString(R.string.dns_automatic);
        String host = Global.getString(getContentResolver(), PRIVATE_DNS_SPECIFIER);
        if (host == null || host.trim().isEmpty()) return getString(R.string.dns_automatic);
        for (int i = 2; i < DNS_HOSTS.length; i++) {
            if (DNS_HOSTS[i].equalsIgnoreCase(host)) {
                return getString(R.string.dns_named_value, DNS_LABELS[i], host);
            }
        }
        return host;
    }

    private void showDnsChoices() {
        String mode = Global.getString(getContentResolver(), PRIVATE_DNS_MODE);
        String host = Global.getString(getContentResolver(), PRIVATE_DNS_SPECIFIER);
        int checked = 1;
        if ("off".equals(mode)) {
            checked = 0;
        } else if ("hostname".equals(mode) && host != null && !host.trim().isEmpty()) {
            checked = DNS_HOSTS.length;
            for (int i = 2; i < DNS_HOSTS.length; i++) {
                if (DNS_HOSTS[i].equalsIgnoreCase(host)) {
                    checked = i;
                    break;
                }
            }
        }
        final int checkedIndex = checked;
        CharSequence[] options = new CharSequence[DNS_HOSTS.length + 1];
        options[0] = getString(R.string.dns_off);
        options[1] = getString(R.string.dns_automatic);
        options[2] = getString(R.string.dns_adguard);
        options[3] = getString(R.string.dns_adguard_family);
        options[4] = getString(R.string.dns_google);
        options[5] = getString(R.string.dns_cloudflare);
        options[6] = getString(R.string.dns_cloudflare_family);
        options[7] = getString(R.string.dns_quad9);
        options[8] = checked == DNS_HOSTS.length
                ? getString(R.string.dns_manual_current, host) : getString(R.string.dns_manual);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.network_dns)
                .setSingleChoiceItems(options, checked, (choiceDialog, which) -> {
                    choiceDialog.dismiss();
                    if (which == DNS_HOSTS.length) showCustomDns();
                    else if (which == 0) writeDns("off", null);
                    else if (which == 1) writeDns("opportunistic", null);
                    else writeDns("hostname", DNS_HOSTS[which]);
                }).setNegativeButton(android.R.string.cancel, null).create();
        dialog.setOnShowListener(d -> {
            dialog.getListView().setSelection(checkedIndex);
            dialog.getListView().requestFocus();
        });
        dialog.show();
    }

    private void showCustomDns() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setHint(R.string.dns_hostname_hint);
        input.setText(Global.getString(getContentResolver(), PRIVATE_DNS_SPECIFIER));
        input.setSelection(input.length());
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.dns_custom)
                .setMessage(R.string.dns_custom_hint).setView(input)
                .setPositiveButton(android.R.string.ok, (d, which) -> {
                    String host = input.getText().toString().trim();
                    if (!validHostname(host)) {
                        Toast.makeText(this, R.string.dns_invalid_hostname, Toast.LENGTH_LONG).show();
                    } else {
                        writeDns("hostname", host);
                    }
                }).setNegativeButton(android.R.string.cancel, null).create();
        dialog.setOnShowListener(d -> {
            input.requestFocus();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        });
        dialog.show();
    }

    private boolean validHostname(String host) {
        if (host.isEmpty() || host.length() > 253 || host.startsWith(".") || host.endsWith(".")) return false;
        String[] labels = host.split("\\.", -1);
        for (String label : labels) {
            if (label.isEmpty() || label.length() > 63 || !label.matches("[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?")) return false;
        }
        return true;
    }

    private void writeDns(String mode, String hostname) {
        if (checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(this).setTitle(R.string.dns_permission_title)
                    .setMessage(R.string.dns_permission_message).setPositiveButton(android.R.string.ok, null).show();
            return;
        }
        try {
            if ("hostname".equals(mode)) Global.putString(getContentResolver(), PRIVATE_DNS_SPECIFIER, hostname);
            Global.putString(getContentResolver(), PRIVATE_DNS_MODE, mode);
            refresh();
            Toast.makeText(this, R.string.dns_saved, Toast.LENGTH_SHORT).show();
        } catch (SecurityException ignored) {
            new AlertDialog.Builder(this).setTitle(R.string.dns_permission_title)
                    .setMessage(R.string.dns_permission_message).setPositiveButton(android.R.string.ok, null).show();
        }
    }

    private void showHiddenApps() {
        SharedPreferences layout = getSharedPreferences("app_layout", MODE_PRIVATE);
        Set<String> hidden = new HashSet<>(layout.getStringSet("hidden", Collections.emptySet()));
        List<String> packages = new ArrayList<>(hidden);
        Collections.sort(packages, String.CASE_INSENSITIVE_ORDER);
        if (packages.isEmpty()) {
            Toast.makeText(this, R.string.no_hidden_apps, Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence[] labels = new CharSequence[packages.size()];
        for (int i = 0; i < packages.size(); i++) {
            String pkg = packages.get(i);
            try {
                ApplicationInfo info = getPackageManager().getApplicationInfo(pkg, 0);
                labels[i] = getPackageManager().getApplicationLabel(info);
            } catch (Exception ignored) {
                labels[i] = pkg;
            }
        }
        new AlertDialog.Builder(this).setTitle(R.string.unhide_apps).setItems(labels, (dialog, which) -> {
            Set<String> updated = new HashSet<>(layout.getStringSet("hidden", Collections.emptySet()));
            updated.remove(packages.get(which));
            layout.edit().putStringSet("hidden", updated).apply();
            setResult(RESULT_OK);
            showHiddenApps();
        }).setNegativeButton(android.R.string.cancel, null).show();
    }

    // ---- 2.11.5: per-area fonts (small sub-page of Look) and input names (Apps) ----
    private LinearLayout lookCard,fontsCard;private TextView fontsEntry;private final int[] fontRowIds=new int[Fonts.AREAS.length];
    private TextView settingsRow(int id){TextView t=new TextView(this);t.setId(id);t.setBackground(SettingsStyle.focus(this,SettingsStyle.accent(this),false));t.setTextColor(0xFFF4F1EA);t.setTextSize(17);t.setTypeface(Fonts.of(this,Fonts.SETTINGS,R.font.outfit_medium));t.setPadding(dp(16),dp(6),dp(16),dp(6));t.setMinHeight(dp(rowHeight));t.setMinimumHeight(dp(rowHeight));t.setSingleLine(false);t.setMaxLines(8);t.setEllipsize(null);t.setIncludeFontPadding(false);t.setGravity(android.view.Gravity.CENTER_VERTICAL|android.view.Gravity.START);t.setFocusable(true);t.setClickable(true);SettingsStyle.focusAnim(t);t.setCompoundDrawablesRelativeWithIntrinsicBounds(null,null,RaiderTheme.chevronDrawable(this),null);return t;}
    private LinearLayout.LayoutParams rowParams(){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,android.view.ViewGroup.LayoutParams.WRAP_CONTENT);lp.bottomMargin=dp(4);return lp;}
    private void addFontsEntry(LinearLayout card){fontsEntry=settingsRow(R.id.fontChoice);fontsEntry.setOnClickListener(v->showFonts());card.addView(fontsEntry,rowParams());updateFontsEntry();}
    private void updateFontsEntry(){if(fontsEntry==null)return;String first=Fonts.name(this,Fonts.AREAS[0]);boolean same=true;for(String a:Fonts.AREAS)if(!Fonts.name(this,a).equals(first))same=false;fontsEntry.setText("Fonts: "+(same?first:"Mixed")+"\nHome, settings, screensaver and Panchang");formatRow(R.id.fontChoice);if(same)setValueFace(fontsEntry,Fonts.family(this,Fonts.AREAS[0]));}
    private void buildFontsCard(){fontsCard=new LinearLayout(this);fontsCard.setOrientation(LinearLayout.VERTICAL);fontsCard.setPadding(dp(12),dp(10),dp(12),dp(10));fontsCard.setClipChildren(false);fontsCard.setClipToPadding(false);fontsCard.setBackground(SettingsStyle.card(this));fontsCard.setVisibility(View.GONE);TextView cap=categoryText("Fonts",14,SettingsStyle.accent(this));cap.setPadding(dp(14),dp(8),dp(14),dp(8));cap.setIncludeFontPadding(false);fontsCard.addView(cap);
        for(int i=0;i<Fonts.AREAS.length;i++){final int area=i;fontRowIds[i]=View.generateViewId();TextView t=settingsRow(fontRowIds[i]);t.setOnClickListener(v->chooseFont(area));fontsCard.addView(t,rowParams());}
        fontsCard.addView(hint("Back returns to Look. Home and Panchang switch the next time they open."));categoryContent.addView(fontsCard,new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,-2));updateFontRows();}
    private void updateFontRows(){if(fontsCard==null)return;for(int i=0;i<Fonts.AREAS.length;i++){TextView t=fontsCard.findViewById(fontRowIds[i]);if(t==null)continue;t.setText(Fonts.AREA_LABELS[i]+": "+Fonts.name(this,Fonts.AREAS[i]));formatRow(fontRowIds[i]);setValueFace(t,Fonts.family(this,Fonts.AREAS[i]));}updateFontsEntry();}
    /** Redraw the row's value in the font it names. */
    private void setValueFace(TextView row,int family){Object tag=row.getTag();if(!(tag instanceof String[]))return;android.graphics.drawable.Drawable end=row.getCompoundDrawablesRelative()[2];android.graphics.drawable.Drawable trailing=end instanceof RaiderTheme.ValueDrawable?((RaiderTheme.ValueDrawable)end).trailing:end;row.setCompoundDrawablesRelativeWithIntrinsicBounds(row.getCompoundDrawablesRelative()[0],null,new RaiderTheme.ValueDrawable(this,((String[])tag)[2],SettingsStyle.value(this),trailing,dp(320),Fonts.regular(this,family)),null);}
    private void chooseFont(int area){String key=Fonts.AREAS[area];int cur=Fonts.family(this,key);new AlertDialog.Builder(this).setTitle(Fonts.AREA_LABELS[area]+" font").setSingleChoiceItems(Fonts.NAMES,cur,(d,w)->{d.dismiss();if(w==cur)return;choices.edit().putString(Fonts.key(key),Fonts.NAMES[w]).apply();setResult(RESULT_OK);if(Fonts.SETTINGS.equals(key)){getIntent().putExtra("open_fonts",true);recreate();return;}updateFontRows();}).setNegativeButton(android.R.string.cancel,null).show();}
    private void showFonts(){if(fontsCard==null||lookCard==null)return;categoryScroll.post(this::fitCurrentCard);lookCard.setVisibility(View.GONE);categoryCards[1]=fontsCard;fontsCard.setVisibility(View.VISIBLE);categoryScroll.scrollTo(0,0);View f=fontsCard.findViewById(fontRowIds[0]);if(f!=null)f.requestFocus();}
    private void hideFonts(){if(fontsCard==null||lookCard==null)return;fontsCard.setVisibility(View.GONE);categoryCards[1]=lookCard;lookCard.setVisibility(View.VISIBLE);if(fontsEntry!=null)fontsEntry.requestFocus();}
    private void addInputNamesRow(LinearLayout card){TextView t=settingsRow(R.id.inputNamesChoice);t.setOnClickListener(v->showInputNamesList());card.addView(t,rowParams());updateInputNamesRow();}
    private void updateInputNamesRow(){TextView t=findViewById(R.id.inputNamesChoice);if(t==null)return;int n=InputNames.count(this);t.setText("Input names: "+(n==0?"None":n+" named"));formatRow(R.id.inputNamesChoice);}
    private void showInputNamesList(){java.util.List<String[]> inputs=InputNames.list(this);String[] items=new String[inputs.size()+1];for(int i=0;i<inputs.size();i++)items[i]=InputNames.display(this,inputs.get(i)[0],inputs.get(i)[1]);items[inputs.size()]="Reset all input names";
        new AlertDialog.Builder(this).setTitle("Input names").setItems(items,(d,w)->{if(w==inputs.size()){new AlertDialog.Builder(this).setTitle("Reset all input names?").setMessage("Every input goes back to its TV name.").setPositiveButton("Reset",(d2,w2)->{InputNames.clearAll(this);setResult(RESULT_OK);updateInputNamesRow();}).setNegativeButton(android.R.string.cancel,null).show();return;}String[] in=inputs.get(w);InputNames.showRename(this,in[0],in[1],()->{setResult(RESULT_OK);updateInputNamesRow();showInputNamesList();});}).setNegativeButton(android.R.string.cancel,null).show();}

    // ---- 2.11.7 look: 56 dp rows with a left icon, 48 dp when a page would otherwise scroll ----
    private int rowHeight=56;private int[] categoryIcons;
    private android.graphics.drawable.Drawable rowIcon(View row){int id=row.getId();int res=0;if(id==R.id.clearCacheChoice)res=R.drawable.ic_clean;else if(id==R.id.photoSourceChoice||id==R.id.refreshPhotosChoice||id==R.id.photoStorageChoice||id==R.id.photoIntervalChoice)res=R.drawable.ic_wallpaper;else if(id==R.id.inputNamesChoice)res=R.drawable.ic_input;else if(categoryIcons!=null){if((fontsCard!=null&&inside(fontsCard,row))||(lookCard!=null&&inside(lookCard,row)))res=categoryIcons[1];else if(categoryCards!=null)for(int i=0;i<categoryCards.length&&i<categoryIcons.length;i++)if(categoryCards[i]!=null&&inside(categoryCards[i],row)){res=categoryIcons[i];break;}}if(res==0)return null;android.graphics.drawable.Drawable d=getDrawable(res);if(d==null)return null;d=d.mutate();d.setTint(SettingsStyle.SECONDARY);return d;}
    private void fitCurrentCard(){if(categoryScroll==null||categoryCards==null)return;View card=categoryCards[selectedCategory];if(card==null||card.getHeight()==0)return;if(card.getHeight()+dp(24)>categoryScroll.getHeight())compactRows(card);}
    private void compactRows(View v){if(v instanceof TextView){TextView t=(TextView)v;if(t.getMinHeight()==dp(56)){t.setMinHeight(dp(48));t.setMinimumHeight(dp(48));}}if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++)compactRows(g.getChildAt(i));}}
    /** Clear app cache pill: reached by UP from the first row or the first category; DOWN returns to where focus came from. */
    private View headerReturn;
    private void focusHeaderFrom(View from){headerReturn=from;if(from!=null&&from.getId()!=View.NO_ID)clearCacheHeader.setNextFocusDownId(from.getId());clearCacheHeader.requestFocus();}
}
