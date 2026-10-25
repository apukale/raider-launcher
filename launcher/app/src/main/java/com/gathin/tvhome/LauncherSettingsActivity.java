package com.gathin.tvhome;

import android.app.Activity;
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
    private TextView nightModeChoice, nightDimChoice, nightStartsChoice, nightEndsChoice;
    private TextView clearCacheChoice;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean clearingCache;
    private LinearLayout categoryRail, categoryContent; private TextView clearCacheHeader, freeRamHeader, appsPerRowValue; private android.widget.ScrollView categoryScroll;
    private View[] railItems;
    private View[] categoryCards;
    private int selectedCategory;
    private int[] focusMemory = new int[9];
    private Bitmap settingsBackground;
    private int columns;
    private boolean animations;
    private boolean advancedEffects;
    private TextView screensaverStyleChoice, photoSourceChoice, photoStorageChoice, photoIntervalChoice, calendarTextSizeChoice, refreshPhotosChoice, startAfterChoice;
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
                SettingsPicker.buttons(this, getString(R.string.advanced_effects), getString(R.string.advanced_effects_warning), null,
                        getString(android.R.string.cancel), null, null, null, getString(R.string.enable), () -> setAdvanced(true));
            }
        });
        followThemeChoice = findViewById(R.id.followThemeChoice);
        followThemeChoice.setOnClickListener(v -> {
            if (!hasOnTvChoices()) {
                Toast.makeText(this, "Already using theme defaults", Toast.LENGTH_SHORT).show();
                return;
            }
            SettingsPicker.confirm(this, "Reset theme choices", "Apps per row, animations and advanced effects go back to the theme defaults.", () -> {
                choices.edit().remove("columns").remove("animations").remove("advanced_effects").commit();
                Config current = Config.load(this);
                columns = current.columns;
                animations = current.animations;
                advancedEffects = current.animations && current.advancedEffects;
                setResult(RESULT_OK);
                refresh();
            });
        });
        defaultHomeChoice = findViewById(R.id.defaultHomeChoice);
        defaultHomeChoice.setOnClickListener(v -> {
            Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
            open(new Intent(Settings.ACTION_HOME_SETTINGS));
        });
        dnsChoice = findViewById(R.id.dnsChoice);
        dnsChoice.setOnClickListener(v -> showDnsChoices());
        clearCacheChoice = findViewById(R.id.clearCacheChoice);
        if (clearCacheChoice != null) {
            clearCacheChoice.setOnClickListener(v -> SettingsPicker.confirm(this, "Clear cache", "This frees cached data for apps on this TV. It does not remove apps, photos, or your settings.", this::clearAppCache));
            clearCacheChoice.setText(R.string.storage_summary_unavailable);
        }
        wireHeaderActions();
        findViewById(R.id.tvSettingsChoice).setOnClickListener(v ->
                open(new Intent(Settings.ACTION_SETTINGS)));
        findViewById(R.id.resetOrderChoice).setOnClickListener(v ->
                SettingsPicker.confirm(this, "Reset app order", getString(R.string.reset_order_confirm), () -> {
                            getSharedPreferences("app_layout", MODE_PRIVATE).edit()
                                    .remove("custom_order").remove("ordered_components").apply();
                            Toast.makeText(this, R.string.reset_order, Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                        }));
        findViewById(R.id.unhideAppsChoice).setOnClickListener(v -> showHiddenApps());
        TextView weatherChoice=findViewById(R.id.weatherChoice);
        weatherChoice.setOnClickListener(v->{choices.edit().putBoolean("weather_on_home",!choices.getBoolean("weather_on_home",true)).apply();refresh();});
        findViewById(R.id.pillLabelsChoice).setOnClickListener(v->{choices.edit().putBoolean("pill_labels",!choices.getBoolean("pill_labels",false)).apply();refresh();});
        View accentChoice = findViewById(R.id.accentChoice);
        accentChoice.setOnClickListener(v -> chooseAccent());
        nightModeChoice=findViewById(R.id.nightModeChoice);
        nightModeChoice.setOnClickListener(v->{choices.edit().putBoolean("night_mode",!choices.getBoolean("night_mode",true)).apply();refresh();});
        nightDimChoice=findViewById(R.id.nightDimChoice);
        nightDimChoice.setOnClickListener(v->chooseNightDim());
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
        calendarTextSizeChoice=findViewById(R.id.calendarTextSizeChoice);
        calendarTextSizeChoice.setOnClickListener(v -> chooseCalendarTextSize());
        refreshPhotosChoice=findViewById(R.id.refreshPhotosChoice);
        refreshPhotosChoice.setOnClickListener(v -> { if(!PhotosMaster.on(this)){Toast.makeText(this,"Photos are off",Toast.LENGTH_SHORT).show();return;} PhotoStore.schedule(this,true); Toast.makeText(this,R.string.photo_refresh_scheduled,Toast.LENGTH_SHORT).show();updatePhotoSummary();mainHandler.postDelayed(photoStatusPoll,2500); });
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
        categoryRail.setBackground(SettingsStyle.card(this));categoryRail.setClipChildren(false);categoryRail.setClipToPadding(false); int railPad=dp(SettingsChrome.RAIL_PAD_DP);categoryRail.setPadding(railPad,railPad,railPad,railPad);categoryRail.setMinimumWidth(0);insetContentPanel();
        categoryScroll.getViewTreeObserver().addOnGlobalFocusChangeListener((oldFocus,newFocus) -> keepFocusedRowVisible(newFocus));
        String[] names = SettingsMenu.LABELS;
        int[] icons = categoryIcons = new int[]{R.drawable.ic_cat_home,R.drawable.ic_cat_appearance,R.drawable.ic_cat_screensaver,R.drawable.ic_wallpaper,R.drawable.ic_cat_calendar,R.drawable.ic_cat_weather,R.drawable.ic_cat_night,R.drawable.ic_cat_apps,R.drawable.ic_cat_about};
        railItems=new View[names.length]; categoryCards=new View[names.length];
        View legacy=LayoutInflater.from(this).inflate(R.layout.legacy_launcher_settings,null,false); // root is a ScrollView; only findViewById is used
        int[][] groups={{R.id.pillLabelsChoice,R.id.unhideAppsChoice},{R.id.accentChoice,R.id.animationsChoice,R.id.advancedChoice},{R.id.startScreensaverChoice,R.id.screensaverStyleChoice,R.id.startAfterChoice},{R.id.photoSourceChoice,R.id.photoIntervalChoice,R.id.photoStorageChoice,R.id.refreshPhotosChoice},{R.id.calendarTextSizeChoice},{R.id.weatherChoice},{R.id.nightModeChoice,R.id.nightDimChoice,R.id.nightStartsChoice,R.id.nightEndsChoice},{R.id.dnsChoice,R.id.tvSettingsChoice,R.id.defaultHomeChoice,R.id.resetOrderChoice,R.id.followThemeChoice},{}};
        for(int category=0;category<names.length;category++){
            final int index=category;
            TextView rail=new TextView(this);railItems[category]=rail;rail.setText(names[category]);android.graphics.drawable.Drawable railIcon=getDrawable(icons[category]).mutate();railIcon.setTint(RaiderTheme.theme(this).secondary);railIcon.setBounds(0,0,dp(20),dp(20));rail.setCompoundDrawablesRelative(railIcon,null,null,null);rail.setCompoundDrawablePadding(dp(8));rail.setGravity(android.view.Gravity.CENTER_VERTICAL);rail.setPadding(dp(12),0,dp(12),0);rail.setMinWidth(0);rail.setMinimumWidth(0);rail.setTextSize(14);rail.setIncludeFontPadding(false);rail.setSingleLine(true);rail.setTextColor(ink());rail.setTypeface(Fonts.of(this,Fonts.SETTINGS,R.font.outfit_medium));rail.setFocusable(true);rail.setClickable(true);rail.setBackground(SettingsStyle.focus(this,SettingsStyle.focusColor(this),false));LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT,dp(36));if(category<names.length-1)rlp.bottomMargin=dp(SettingsChrome.RAIL_MIN_GAP_DP);categoryRail.addView(rail,rlp);rail.setId(View.generateViewId());rail.setOnClickListener(v->selectCategory(index,true));rail.setOnFocusChangeListener((v,f)->{int focus=SettingsStyle.focusColor(this);v.setBackground(SettingsStyle.rail(this,focus,index==selectedCategory,f));((TextView)v).setTextColor(index==selectedCategory?RaiderColors.inkOn(focus):(f?ink():RaiderTheme.theme(this).secondary));tintRailIcon((TextView)v,index==selectedCategory,f);SettingsStyle.scale(v,f);});
            LinearLayout card=new LinearLayout(this);categoryCards[category]=card;card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(SettingsChrome.PANEL_PAD_H_DP),dp(10),dp(SettingsChrome.PANEL_PAD_H_DP),dp(10));card.setClipChildren(false);card.setClipToPadding(false);card.setBackground(SettingsStyle.card(this));LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,-2);clp.bottomMargin=0;categoryContent.addView(card,clp);
            if(category==8){buildAbout(card);continue;}
            if(category==4){addOpenPanchang(card);addHinduMaster(card);addCalendarPlace(card);}
            if(category==3)addPhotosMaster(card);
            if(category==0){addHomeLayoutModules(card);LinearLayout apr=new LinearLayout(this);apr.setOrientation(LinearLayout.HORIZONTAL);apr.setGravity(android.view.Gravity.CENTER_VERTICAL);apr.setPadding(dp(14),0,dp(14),0);TextView aprTitle=categoryText("Apps per row",17,ink());aprTitle.setPadding(0,0,0,0);aprTitle.setIncludeFontPadding(false);aprTitle.setTypeface(Fonts.of(this,Fonts.SETTINGS,R.font.outfit_medium));apr.addView(aprTitle,new LinearLayout.LayoutParams(0,android.view.ViewGroup.LayoutParams.WRAP_CONTENT,1f));appsPerRowValue=categoryText(columns+" apps",16,SettingsStyle.accent(this));appsPerRowValue.setPadding(0,0,0,0);appsPerRowValue.setIncludeFontPadding(false);apr.addView(appsPerRowValue);card.addView(apr,new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,dp(40)));LinearLayout choicesRow=new LinearLayout(this);choicesRow.setOrientation(LinearLayout.HORIZONTAL);int[] columnIds={R.id.columns4,R.id.columns5,R.id.columns6,R.id.columns7};for(int id:columnIds){View choice=legacy.findViewById(id);ViewGroupTools.detach(choice);if(choice instanceof TextView){((TextView)choice).setTextColor(ink());((TextView)choice).setTypeface(Fonts.of(this,Fonts.SETTINGS,R.font.outfit_medium));}LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(40),1f);lp.setMargins(dp(4),0,dp(4),dp(4));final int tint=COLUMN_TINTS[id==R.id.columns4?0:id==R.id.columns5?1:id==R.id.columns6?2:3];choice.setBackground(SettingsStyle.glow(this,SettingsStyle.focusColor(this),tint,false,false));choice.setOnFocusChangeListener((v,f)->{v.setBackground(SettingsStyle.glow(this,SettingsStyle.focusColor(this),tint,f,false));SettingsStyle.scale(v,f);});choicesRow.addView(choice,lp);}card.addView(choicesRow);}
            boolean resetShown=false;
            for(int id:groups[category]){View row=legacy.findViewById(id);if(row==null)continue;ViewGroupTools.detach(row);if(isDestructive(id)&&!resetShown){resetShown=true;TextView cap=categoryText("Reset",13,0xFFE8907E);cap.setPadding(dp(12),0,dp(12),0);cap.setIncludeFontPadding(false);cap.setSingleLine(true);card.addView(cap);}LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,android.view.ViewGroup.LayoutParams.WRAP_CONTENT);lp.bottomMargin=dp(4);row.setBackground(SettingsStyle.focus(this,SettingsStyle.focusColor(this),false));if(row instanceof TextView){TextView t=(TextView)row;t.setTextColor(ink());t.setTextSize(17);t.setTypeface(Fonts.of(this,Fonts.SETTINGS,R.font.outfit_medium));t.setPadding(dp(16),dp(6),dp(16),dp(6));t.setMinHeight(dp(rowHeight));t.setMinimumHeight(dp(rowHeight));t.setSingleLine(false);t.setMaxLines(8);t.setEllipsize(null);t.setIncludeFontPadding(false);t.setGravity(android.view.Gravity.CENTER_VERTICAL|android.view.Gravity.START);t.setFocusable(true);t.setClickable(true);SettingsStyle.focusAnim(t);}card.addView(row,lp);if(id==R.id.accentChoice)addFontsEntry(card);if(id==R.id.tvSettingsChoice)addInputNamesRow(card);if(id==R.id.advancedChoice)card.addView(hint(getString(R.string.advanced_effects_warning)));}
            if(category==0){addHomePhotoRows(card);linkHomeLayoutModules();}
            if(category==4)addWorldClocks(card);
            if(category==6)addNightNowRow(card);
            if(category==5)addWeatherRows(card);
            if(category==7)addKidsRow(card);
        }
        lookCard=(LinearLayout)categoryCards[1];buildFontsCard();
        selectCategory(0,false);if(getIntent().getBooleanExtra("open_fonts",false)){getIntent().removeExtra("open_fonts");selectCategory(1,false);showFonts();}
        int openCategory=getIntent().getIntExtra(SettingsMenu.OPEN_CATEGORY,-1);
        if(openCategory>=0&&openCategory<railItems.length){getIntent().removeExtra(SettingsMenu.OPEN_CATEGORY);selectCategory(openCategory,false);if(categoryScroll!=null){final int opened=openCategory;categoryScroll.post(()->{View target=opened==SettingsMenu.CALENDAR?findViewById(R.id.worldCity1Choice):railItems[opened];if(target!=null)target.requestFocus();});}}
        tightenRail();spaceRail();if(categoryScroll!=null){categoryScroll.post(this::fitHomeCard);categoryScroll.post(this::fitCalendarCard);categoryScroll.post(this::fitAdvancedCard);}
        {View sr=findViewById(R.id.settingsRoot);if(settingsBackground!=null){BitmapDrawable image=new BitmapDrawable(getResources(),settingsBackground);image.setGravity(android.view.Gravity.FILL);image.setFilterBitmap(true);sr.setBackground(new LayerDrawable(new android.graphics.drawable.Drawable[]{image,SettingsStyle.scrim()}));}else sr.setBackgroundColor(RaiderTheme.theme(this).background);}TextView settingsHeader=findViewById(R.id.settingsHeader);if(settingsHeader!=null)settingsHeader.setTextColor(ink());
    }
    private TextView categoryText(String text,int size,int color){TextView view=new TextView(this);view.setText(text);view.setTextColor(color);view.setTextSize(size);view.setTypeface(Fonts.of(this,Fonts.SETTINGS,R.font.outfit_regular));view.setPadding(dp(8),dp(8),dp(8),dp(14));return view;}
    private static final class ViewGroupTools{static void detach(View view){if(view.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)view.getParent()).removeView(view);}}
    private void selectCategory(int index,boolean requestFocus){if(fontsCard!=null&&categoryCards[1]==fontsCard){categoryCards[1]=lookCard;fontsCard.setVisibility(View.GONE);}selectedCategory=index;if(categoryScroll!=null)categoryScroll.scrollTo(0,0);for(int i=0;i<categoryCards.length;i++){categoryCards[i].setVisibility(i==index?View.VISIBLE:View.GONE);railItems[i].setBackground(SettingsStyle.rail(this,SettingsStyle.focusColor(this),i==index,railItems[i].hasFocus()));((TextView)railItems[i]).setTextColor(i==index?RaiderColors.inkOn(SettingsStyle.focusColor(this)):RaiderTheme.theme(this).secondary);tintRailIcon((TextView)railItems[i],i==index,railItems[i].hasFocus());}if(requestFocus)railItems[index].requestFocus();if(categoryScroll!=null)categoryScroll.post(this::fitCurrentCard);}
    private void loadSettingsBackground(java.io.File file){try{BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getAbsolutePath(),bounds);if(bounds.outWidth<=0)return;int sample=1;while(sample<64&&bounds.outWidth/(sample*2)>=240)sample*=2;BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=sample;Bitmap raw=BitmapFactory.decodeFile(file.getAbsolutePath(),options);if(raw==null)return;int w=raw.getWidth(),h=raw.getHeight();float target=(float)getResources().getDisplayMetrics().widthPixels/(float)getResources().getDisplayMetrics().heightPixels;int cropW=w,cropH=h;if((float)w/h>target)cropW=Math.max(1,(int)(h*target));else cropH=Math.max(1,(int)(w/target));Bitmap cropped=Bitmap.createBitmap(raw,(w-cropW)/2,(h-cropH)/2,cropW,cropH);Bitmap small=Bitmap.createScaledBitmap(cropped,240,Math.max(1,Math.round(240/target)),true);if(cropped!=raw)cropped.recycle();if(small!=raw)raw.recycle();
        // ~1/8 of 1080p; pre-blurred once here, never live
        try{androidx.palette.graphics.Palette p=androidx.palette.graphics.Palette.from(small).maximumColorCount(16).generate();androidx.palette.graphics.Palette.Swatch s=p.getVibrantSwatch();if(s==null)s=p.getLightVibrantSwatch();if(s==null)s=p.getDominantSwatch();SettingsStyle.photoAccent=s==null?0:SettingsStyle.fromSwatch(s.getRgb());}catch(RuntimeException e){SettingsStyle.photoAccent=0;}
        w=small.getWidth();h=small.getHeight();int[] pixels=new int[w*h];small.getPixels(pixels,0,w,0,0,w,h);small.recycle();BoxBlur.blur(pixels,w,h,8,2);Bitmap blurred=Bitmap.createBitmap(w,h,Bitmap.Config.RGB_565);blurred.setPixels(pixels,0,w,0,0,w,h);settingsBackground=blurred;}catch(Throwable t){SettingsStyle.photoAccent=0;}}
    private void applyAccentToRows(View view){if(view.isFocusable())view.setBackground(SettingsStyle.focus(this,SettingsStyle.focusColor(this),view.hasFocus()));if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++)applyAccentToRows(group.getChildAt(i));}}
    private View findFocusable(View view){if(view.isFocusable()&&view.getVisibility()==View.VISIBLE)return view;if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View found=findFocusable(group.getChildAt(i));if(found!=null)return found;}}return null;}
    private int ink(){return RaiderTheme.theme(this).onSurface;}
    private void chooseAccent(){String[] labels=RaiderColors.labels();String[] ids=RaiderColors.ids();String current=RaiderColors.theme(choices.getString("accent_color",RaiderTheme.DEFAULT)).id;int selected=0;for(int i=0;i<ids.length;i++)if(ids[i].equals(current))selected=i;SettingsPicker.single(this,"Colour theme",labels,selected,w->{choices.edit().putString("accent_color",ids[w]).apply();setResult(RESULT_OK);recreate();});}
    @Override public boolean dispatchKeyEvent(KeyEvent event){if(event.getAction()==KeyEvent.ACTION_DOWN){int key=event.getKeyCode();View cur=getCurrentFocus();boolean onCache=clearCacheHeader!=null&&clearCacheHeader.isFocused();boolean onRam=freeRamHeader!=null&&freeRamHeader.isFocused();boolean onHeader=onCache||onRam;boolean inContent=cur!=null&&inside(categoryContent,cur);View first=findFocusable(categoryCards[selectedCategory]);
        if(key==KeyEvent.KEYCODE_BACK){if(fontsCard!=null&&categoryCards[1]==fontsCard&&inContent){hideFonts();return true;}if(inContent||onHeader){railItems[selectedCategory].requestFocus();return true;}finish();return true;}
        if(onHeader){if(key==KeyEvent.KEYCODE_DPAD_DOWN){View back=headerReturn;headerReturn=null;if(back!=null&&back.isShown()&&back.isFocusable())back.requestFocus();else if(first!=null)first.requestFocus();return true;}if(key==KeyEvent.KEYCODE_DPAD_LEFT){if(onRam&&clearCacheHeader!=null){clearCacheHeader.requestFocus();return true;}railItems[selectedCategory].requestFocus();return true;}if(key==KeyEvent.KEYCODE_DPAD_RIGHT){if(onCache&&freeRamHeader!=null){freeRamHeader.requestFocus();return true;}return true;}if(key==KeyEvent.KEYCODE_DPAD_UP)return true;}
        if(inContent&&(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)){int dir=key==KeyEvent.KEYCODE_DPAD_UP?View.FOCUS_UP:key==KeyEvent.KEYCODE_DPAD_DOWN?View.FOCUS_DOWN:key==KeyEvent.KEYCODE_DPAD_LEFT?View.FOCUS_LEFT:View.FOCUS_RIGHT;View next=cur.focusSearch(dir);if(next==null||!inside(categoryContent,next)){if(key==KeyEvent.KEYCODE_DPAD_UP&&clearCacheHeader!=null)focusHeaderFrom(cur);else if(key==KeyEvent.KEYCODE_DPAD_LEFT)railItems[selectedCategory].requestFocus();return true;}}
        if(key==KeyEvent.KEYCODE_DPAD_RIGHT&&categoryRail.findFocus()!=null){if(first!=null)first.requestFocus();return true;}
        if(categoryRail.findFocus()!=null&&key==KeyEvent.KEYCODE_DPAD_UP&&selectedCategory==0&&clearCacheHeader!=null){focusHeaderFrom(cur);return true;}if(categoryRail.findFocus()!=null&&(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN)){selectCategory(Math.floorMod(selectedCategory+(key==KeyEvent.KEYCODE_DPAD_DOWN?1:-1),railItems.length),true);return true;}
        if(categoryRail.findFocus()!=null&&key==KeyEvent.KEYCODE_DPAD_LEFT)return true;}return super.dispatchKeyEvent(event);}
    @Override protected void onDestroy(){if(settingsBackground!=null&&!settingsBackground.isRecycled()){View root=findViewById(R.id.settingsRoot);if(root!=null)root.setBackground(null);settingsBackground.recycle();}super.onDestroy();}
    private void refreshScreensaverLabels(){updateScreensaverRows();}
    private void updateScreensaverRows(){if(screensaverStyleChoice==null)return;setToggle(findViewById(R.id.animationsChoice),animations);setToggle(findViewById(R.id.advancedChoice),advancedEffects);setToggle(findViewById(R.id.pillLabelsChoice),choices.getBoolean("pill_labels",false));setToggle(findViewById(R.id.weatherChoice),choices.getBoolean("weather_on_home",true));setToggle(findViewById(R.id.nightModeChoice),choices.getBoolean("night_mode",true));setToggle(findViewById(R.id.hinduMasterChoice),choices.getBoolean(LauncherPrefs.HINDU,true));setToggle(findViewById(R.id.worldDreamChoice),choices.getBoolean(WorldClocks.DREAM,false));View accentView=findViewById(R.id.accentChoice);if(accentView instanceof TextView)((TextView)accentView).setText("Colour theme: "+RaiderColors.theme(choices.getString("accent_color",RaiderTheme.DEFAULT)).label);screensaverStyleChoice.setText(getString(R.string.screensaver_style,screensaverStyleLabel()));java.util.Set<String> srcs=PhotoStore.selectedSources(this);int onCount=0;StringBuilder srcLabel=new StringBuilder();for(String s:PhotoSources.KNOWN)if(srcs.contains(s)&&ExtraPhotoSources.available(s)){onCount++;if(onCount<=2){if(srcLabel.length()>0)srcLabel.append(", ");srcLabel.append(ExtraPhotoSources.label(s));}}if(onCount>2)srcLabel.append(" +"+(onCount-2));photoSourceChoice.setText(getString(R.string.photo_source,srcLabel.toString()));photoStorageChoice.setText(getString(R.string.photo_storage,(int)(PhotoStore.cap(this)/(1024L*1024L))));long ms=choices.getLong("photo_interval_ms",60_000L);boolean photosOn=PhotosMaster.on(this);String intervalText=getString(R.string.photo_interval,HomeFocusPhoto.intervalLabel(ms));if(!photosOn)intervalText=intervalText+"\n"+getString(R.string.photo_interval_paused);photoIntervalChoice.setText(intervalText);photoIntervalChoice.setAlpha(HomePhotoSettings.intervalAlpha(photosOn));boolean hindu=choices.getBoolean(LauncherPrefs.HINDU,true);TextView hinduRow=findViewById(R.id.hinduMasterChoice);if(hinduRow!=null)hinduRow.setText("Hindu calendar: "+getString(hindu?R.string.on:R.string.off));updateCalendarPlace(hindu);if(calendarTextSizeChoice!=null)calendarTextSizeChoice.setAlpha(hindu?1f:0.45f);int size=choices.getInt("home_calendar_text_size",2);int sizeLabel=size==0?R.string.calendar_size_small:size==1?R.string.calendar_size_medium:R.string.calendar_size_large;calendarTextSizeChoice.setText(getString(R.string.calendar_text_size,getString(sizeLabel)));TextView weather=findViewById(R.id.weatherChoice);if(weather!=null)weather.setText(getString(R.string.weather_on_home,getString(choices.getBoolean("weather_on_home",true)?R.string.on:R.string.off))+"\n"+weatherStatus());TextView pills=findViewById(R.id.pillLabelsChoice);if(pills!=null)pills.setText(getString(R.string.pill_labels,getString(choices.getBoolean("pill_labels",false)?R.string.pill_labels_text:R.string.pill_labels_icons)));if(nightModeChoice!=null){nightModeChoice.setText(getString(R.string.night_mode,getString(choices.getBoolean("night_mode",true)?R.string.on:R.string.off)));if(nightDimChoice!=null)nightDimChoice.setText("Dim: "+NightEffects.percent(choices.getInt(NightEffects.PREF_DIM,NightEffects.DEFAULT_DIM))+"%");nightStartsChoice.setText(getString(R.string.night_starts,formatNightTime(choices.getInt("night_start_min",22*60))));nightEndsChoice.setText(getString(R.string.night_ends,formatNightTime(choices.getInt("night_end_min",6*60))));}updateWorldClockRows();if(startAfterChoice.getVisibility()==View.VISIBLE){int timeout=Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_OFF_TIMEOUT,600000);int current=Math.max(1,Math.round(timeout/60000f));startAfterChoice.setText(getString(R.string.start_after,getString(R.string.screensaver_minutes,current)));}formatRows();updateNightNow();updateHomePhotoRows();}
    private static final int[] ACTION_ROWS={R.id.followThemeChoice,R.id.resetOrderChoice,R.id.unhideAppsChoice,R.id.defaultHomeChoice,R.id.accentChoice,R.id.screensaverStyleChoice,R.id.photoIntervalChoice,R.id.photoSourceChoice,R.id.photoStorageChoice,R.id.refreshPhotosChoice,R.id.startAfterChoice,R.id.startScreensaverChoice,R.id.calendarTextSizeChoice,R.id.clearCacheChoice,R.id.tvSettingsChoice,R.id.dnsChoice,R.id.nightDimChoice,R.id.nightStartsChoice,R.id.nightEndsChoice};
    private static boolean isActionRow(int id){for(int a:ACTION_ROWS)if(a==id)return true;return false;}
    private static boolean isDestructive(int id){return id==R.id.followThemeChoice||id==R.id.resetOrderChoice;}
    private TextView nightNowChoice;
    /** Start now / Stop now: a manual night mode override that lasts until the next scheduled end (or start). */
    private void addNightNowRow(LinearLayout card){TextView t=new TextView(this);t.setId(R.id.nightNowChoice);nightNowChoice=t;t.setBackground(SettingsStyle.focus(this,SettingsStyle.focusColor(this),false));t.setTextColor(ink());t.setTextSize(17);t.setTypeface(Fonts.of(this,Fonts.SETTINGS,R.font.outfit_medium));t.setPadding(dp(16),dp(6),dp(16),dp(6));t.setMinHeight(dp(rowHeight));t.setMinimumHeight(dp(rowHeight));t.setSingleLine(false);t.setMaxLines(8);t.setEllipsize(null);t.setIncludeFontPadding(false);t.setGravity(android.view.Gravity.CENTER_VERTICAL|android.view.Gravity.START);t.setFocusable(true);t.setClickable(true);SettingsStyle.focusAnim(t);t.setOnClickListener(v->{NightState.toggleNow(choices,System.currentTimeMillis());setResult(RESULT_OK);updateNightNow();});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,android.view.ViewGroup.LayoutParams.WRAP_CONTENT);lp.bottomMargin=dp(4);card.addView(t,lp);updateNightNow();}
    private void updateNightNow(){if(nightNowChoice==null)return;if(choices==null)choices=getSharedPreferences(Config.SETTINGS_NAME,MODE_PRIVATE);long now=System.currentTimeMillis();boolean active=NightState.isActive(choices,now);java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("h:mm a",java.util.Locale.getDefault());String value;if(active){long until=NightState.nextChangeMillis(choices,now);value=until==Long.MAX_VALUE?"On":"On until "+f.format(new java.util.Date(until));}else{value=NightState.overrideActive(choices,now)?"Off until "+f.format(new java.util.Date(choices.getLong(NightState.OVERRIDE_UNTIL,0L))):"Off";}NightEffects.apply(this,findViewById(R.id.settingsRoot),active);nightNowChoice.setText((active?"Stop now":"Start now")+": "+value);formatRow(R.id.nightNowChoice);}
    private void formatRows(){int[] ids={R.id.animationsChoice,R.id.advancedChoice,R.id.followThemeChoice,R.id.pillLabelsChoice,R.id.resetOrderChoice,R.id.unhideAppsChoice,R.id.defaultHomeChoice,R.id.accentChoice,R.id.screensaverStyleChoice,R.id.photoIntervalChoice,R.id.photoSourceChoice,R.id.photoStorageChoice,R.id.refreshPhotosChoice,R.id.startAfterChoice,R.id.startScreensaverChoice,R.id.photosMasterChoice,R.id.hinduMasterChoice,R.id.homeCalendarChoice,R.id.calendarTextSizeChoice,R.id.calendarPlaceChoice,R.id.worldCity1Choice,R.id.worldCity2Choice,R.id.worldCity3Choice,R.id.worldCity4Choice,R.id.worldDreamChoice,R.id.openPanchangChoice,R.id.openWorldClocksChoice,R.id.kidsChoice,R.id.weatherChoice,R.id.openWeatherChoice,R.id.homePhotosChoice,R.id.homePhotoIntervalChoice,R.id.clearCacheChoice,R.id.freeRamChoice,R.id.freeRamHomeChoice,R.id.tvSettingsChoice,R.id.dnsChoice,R.id.nightModeChoice,R.id.nightDimChoice,R.id.nightStartsChoice,R.id.nightEndsChoice};for(int id:ids)formatRow(id);}
    /** Title (and optional detail line) on the left, current value + toggle/chevron on the right. Re-parses only when the row text was set afresh. */
    private void formatRow(int id){View value=findViewById(id);if(!(value instanceof TextView))return;TextView row=(TextView)value;String raw=row.getText().toString();Object tag=row.getTag();String title,val,sub;
        if(tag instanceof String[]&&raw.equals(((String[])tag)[0])){String[] st=(String[])tag;title=st[1];val=st[2];sub=st[3];}
        else if(keepsOneLine(id)){title=raw.trim();val="";sub="";}
        else{int nl=raw.indexOf('\n');String first=nl<0?raw:raw.substring(0,nl);String rest=nl<0?"":raw.substring(nl+1).trim();int c=first.indexOf(": ");if(c>=0){title=first.substring(0,c).trim();val=first.substring(c+2).trim();sub=rest;}else{title=first.trim();int nl2=rest.indexOf('\n');val=nl2<0?rest:rest.substring(0,nl2).trim();sub=nl2<0?"":rest.substring(nl2+1).trim();}}
        boolean calendarRow=isCalendarRow(id);boolean homeDetail=id==R.id.homePhotosChoice||id==R.id.homePhotoIntervalChoice;int titleSp=calendarRow?16:homeDetail?15:17;int subSp=calendarRow?13:homeDetail?12:14;SpannableString styled=new SpannableString(sub.isEmpty()?title:title+"\n"+sub);styled.setSpan(new AbsoluteSizeSpan(titleSp,true),0,title.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);styled.setSpan(new ForegroundColorSpan(ink()),0,title.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);if(!sub.isEmpty()){styled.setSpan(new AbsoluteSizeSpan(subSp,true),title.length()+1,styled.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);styled.setSpan(new ForegroundColorSpan(RaiderTheme.theme(this).secondary),title.length()+1,styled.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}
        row.setText(styled);row.setTag(new String[]{styled.toString(),title,val,sub});
        android.graphics.drawable.Drawable end=row.getCompoundDrawablesRelative()[2];android.graphics.drawable.Drawable trailing=end instanceof RaiderTheme.ValueDrawable?((RaiderTheme.ValueDrawable)end).trailing:end;
        row.setCompoundDrawablePadding(dp(12));row.setCompoundDrawablesRelativeWithIntrinsicBounds(rowIcon(row),null,new RaiderTheme.ValueDrawable(this,val,SettingsStyle.value(this),trailing,dp(320)),null);if(keepsOneLine(id)){row.setSingleLine(true);row.setMaxLines(1);row.setEllipsize(android.text.TextUtils.TruncateAt.END);}}
    private boolean inside(View parent,View v){while(v!=null){if(v==parent)return true;android.view.ViewParent p=v.getParent();v=p instanceof View?(View)p:null;}return false;}
    private void keepFocusedRowVisible(View f){if(f==null||categoryScroll==null||!inside(categoryContent,f))return;if(selectedCategory==SettingsMenu.CALENDAR||selectedCategory==SettingsMenu.HOME){if(categoryScroll.getScrollY()!=0)categoryScroll.scrollTo(0,0);return;}android.graphics.Rect r=new android.graphics.Rect();f.getDrawingRect(r);categoryScroll.offsetDescendantRectToMyCoords(f,r);int m=dp(24),sy=categoryScroll.getScrollY(),h=categoryScroll.getHeight();if(r.top-m<sy)categoryScroll.smoothScrollTo(0,Math.max(0,r.top-m));else if(r.bottom+m>sy+h)categoryScroll.smoothScrollTo(0,r.bottom+m-h);}
    private TextView hint(String text){TextView t=categoryText(text,14,0xFFA9ABC7);t.setPadding(dp(14),dp(2),dp(14),dp(8));t.setIncludeFontPadding(false);return t;}
    private void setToggle(View view,boolean on){if(view instanceof TextView){TextView text=(TextView)view;text.setCompoundDrawablePadding(dp(12));text.setCompoundDrawablesRelativeWithIntrinsicBounds(null,null,SettingsStyle.toggle(this,on,SettingsStyle.accent(this)),null);}}
    private String weatherStatus(){long tried=choices.getLong("weather_last_try_at",0);String error=choices.getString("weather_last_error","");if(!error.isEmpty())return WeatherStatus.format(true,error,"",0);WeatherData d=WeatherClient.cached(this);if(tried==0||d==null)return WeatherStatus.format(false,"","",0);java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("h:mm a",java.util.Locale.getDefault());return WeatherStatus.format(true,"",f.format(new java.util.Date(tried)),d.temperature);}
    private String formatNightTime(int total){int hour24=total/NIGHT_MINUTES_PER_HOUR;int hour=hour24%NIGHT_HOURS_PER_HALF_DAY;if(hour==0)hour=NIGHT_HOURS_PER_HALF_DAY;return getString(R.string.night_time_value,Integer.toString(hour),total%NIGHT_MINUTES_PER_HOUR,getString(hour24< NIGHT_HOURS_PER_HALF_DAY?R.string.night_am:R.string.night_pm));}
    private void chooseNightDim(){int[] steps=NightEffects.DIM_STEPS;String[] labels=new String[steps.length];int current=NightEffects.percent(choices.getInt(NightEffects.PREF_DIM,NightEffects.DEFAULT_DIM));int selected=0;for(int i=0;i<steps.length;i++){labels[i]=steps[i]+"%";if(steps[i]==current)selected=i;}SettingsPicker.single(this,"Night dim",labels,selected,w->{choices.edit().putInt(NightEffects.PREF_DIM,steps[w]).apply();setResult(RESULT_OK);refresh();});}
    private void chooseNightTime(String key,int fallback){int[] parts={0,0,0};int total=choices.getInt(key,fallback);int h=total/NIGHT_MINUTES_PER_HOUR;parts[0]=h%NIGHT_HOURS_PER_HALF_DAY==0?NIGHT_HOURS_PER_HALF_DAY:h%NIGHT_HOURS_PER_HALF_DAY;parts[1]=total%NIGHT_MINUTES_PER_HOUR;parts[2]=h>=NIGHT_HOURS_PER_HALF_DAY?1:0;LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.HORIZONTAL);int pad=dp(NIGHT_PICKER_PADDING_DP);panel.setPadding(pad,pad,pad,pad);TextView[] cells=new TextView[3];final android.app.Dialog[] dialogRef=new android.app.Dialog[1];for(int i=0;i<cells.length;i++){final int column=i;TextView cell=new TextView(this);cells[i]=cell;cell.setTextSize(30f);cell.setGravity(android.view.Gravity.CENTER);cell.setFocusable(true);cell.setFocusableInTouchMode(true);cell.setMinWidth(dp(NIGHT_PICKER_CELL_WIDTH_DP));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(NIGHT_PICKER_CELL_HEIGHT_DP),1f);panel.addView(cell,lp);cell.setOnFocusChangeListener((v,focus)->v.setSelected(focus));cell.setOnKeyListener((v,keyCode,event)->{if(event.getAction()!=android.view.KeyEvent.ACTION_DOWN)return false;if(keyCode==android.view.KeyEvent.KEYCODE_DPAD_LEFT||keyCode==android.view.KeyEvent.KEYCODE_DPAD_RIGHT){cells[Math.max(0,Math.min(2,column+(keyCode==android.view.KeyEvent.KEYCODE_DPAD_LEFT?-1:1)))].requestFocus();return true;}if(keyCode==android.view.KeyEvent.KEYCODE_DPAD_UP||keyCode==android.view.KeyEvent.KEYCODE_DPAD_DOWN){int step=keyCode==android.view.KeyEvent.KEYCODE_DPAD_UP?1:-1;if(column==0)parts[0]=Math.floorMod(parts[0]-1+step,NIGHT_HOURS_PER_HALF_DAY)+1;else if(column==1)parts[1]=Math.floorMod(parts[1]+step*NIGHT_MINUTE_STEP,NIGHT_MINUTES_PER_HOUR);else parts[2]=1-parts[2];updateNightPicker(cells,parts);return true;}if(keyCode==android.view.KeyEvent.KEYCODE_DPAD_CENTER||keyCode==android.view.KeyEvent.KEYCODE_ENTER){choices.edit().putInt(key,(parts[0]%NIGHT_HOURS_PER_HALF_DAY+parts[2]*NIGHT_HOURS_PER_HALF_DAY)*NIGHT_MINUTES_PER_HOUR+parts[1]).apply();refresh();if(dialogRef[0]!=null)dialogRef[0].dismiss();return true;}return false;});}updateNightPicker(cells,parts);dialogRef[0]=SettingsPicker.form(this,getString(R.string.night_time_picker),panel,getString(android.R.string.cancel),null,null,null,getString(android.R.string.ok),()->{choices.edit().putInt(key,(parts[0]%NIGHT_HOURS_PER_HALF_DAY+parts[2]*NIGHT_HOURS_PER_HALF_DAY)*NIGHT_MINUTES_PER_HOUR+parts[1]).apply();refresh();});}
    private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+0.5f);}
    private void updateNightPicker(TextView[] cells,int[] parts){cells[0].setText(Integer.toString(parts[0]));cells[1].setText(String.format(java.util.Locale.ROOT,"%02d",parts[1]));cells[2].setText(getString(parts[2]==0?R.string.night_am:R.string.night_pm));}
    private void chooseCalendarTextSize(){String[] labels={getString(R.string.calendar_size_small),getString(R.string.calendar_size_medium),getString(R.string.calendar_size_large)};SettingsPicker.single(this,getString(R.string.calendar_text_size_title),labels,choices.getInt("home_calendar_text_size",2),w->{choices.edit().putInt("home_calendar_text_size",w).apply();updateScreensaverRows();});}
    private void updatePhotoSummary(){updatePhotoSummaryRaw();formatRow(R.id.refreshPhotosChoice);}
    private void updatePhotoSummaryRaw(){if(refreshPhotosChoice!=null){android.content.SharedPreferences p=getSharedPreferences(Config.SETTINGS_NAME,MODE_PRIVATE);long at=p.getLong("photo_last_refresh_at",0);String when=at==0?"never":new java.text.SimpleDateFormat("MMM d, h:mm a",java.util.Locale.getDefault()).format(new java.util.Date(at));String error=p.getString("photo_last_error","");refreshPhotosChoice.setText(getString(R.string.refresh_photos,PhotoStore.summary(this))+"\nLast refresh "+when+(error.isEmpty()?"":"\nLast error: "+error));formatRows();}}
    private static final int[] COLUMN_TINTS={0x4D26A69A,0x4D5C6BC0,0x4DEC407A,0x4DFFA726};
    private int screensaverStyleIndex(){if(!choices.getBoolean("dream_photos",true))return 0;return choices.getBoolean("dream_photo_calendar",true)?2:1;}
    private String screensaverStyleLabel(){int i=screensaverStyleIndex();return i==0?getString(R.string.screensaver_clock_only):i==1?"Photos + clock":getString(R.string.screensaver_photos);}
    private void chooseScreensaverStyle(){String[] labels={getString(R.string.screensaver_clock_only),"Photos + clock",getString(R.string.screensaver_photos)};SettingsPicker.single(this,getString(R.string.screensaver_settings),labels,screensaverStyleIndex(),w->{choices.edit().putBoolean("dream_photos",w>0).putBoolean("dream_photo_calendar",w==2).apply();refreshScreensaverLabels();});}
    /** One toggle per photo source; sources whose API key is missing are hidden (and skipped by PhotoStore). */
    private void choosePhotoSource(){java.util.ArrayList<String> keys=new java.util.ArrayList<>();for(String s:PhotoSources.KNOWN)if(ExtraPhotoSources.available(s))keys.add(s);String[] labels=new String[keys.size()];boolean[] checked=new boolean[keys.size()];java.util.Set<String> sel=PhotoStore.selectedSources(this);for(int i=0;i<keys.size();i++){labels[i]=ExtraPhotoSources.label(keys.get(i));checked[i]=sel.contains(keys.get(i));}
        SettingsPicker.multi(this,getString(R.string.photo_source_title),labels,checked,(w,on)->{java.util.LinkedHashSet<String> v=new java.util.LinkedHashSet<>();for(String s:sel)if(!keys.contains(s))v.add(s);for(int i=0;i<keys.size();i++)if(i==w?on:checked[i])v.add(keys.get(i));boolean any=false;for(String s:v)if(ExtraPhotoSources.available(s))any=true;if(!any)return false;choices.edit().putString("photo_sources",PhotoSources.serialize(v)).remove("photo_source").apply();updateScreensaverRows();updatePhotoSummary();PhotoStore.schedule(this,false);return true;},()->PhotoStore.schedule(this,true));}
    private void choosePhotoStorage(){int[] values={100,200,500};String[] labels={"100 MB","200 MB","500 MB"};int current=(int)(PhotoStore.cap(this)/(1024L*1024L)),selected=current==100?0:current==500?2:1;SettingsPicker.single(this,getString(R.string.photo_storage_title),labels,selected,w->{choices.edit().putInt("photo_storage_mb",values[w]).apply();PhotoStore.enforceCap(this);updateScreensaverRows();updatePhotoSummary();});}
    private void choosePhotoInterval(){long[] values=HomePhotoSettings.INTERVALS_MS;String[] labels=HomePhotoSettings.LABELS;long current=choices.getLong(HomePhotoSettings.INTERVAL,HomeFocusPhoto.INTERVAL_DEFAULT_MS);int selected=HomePhotoSettings.selectedIndex(current);SettingsPicker.single(this,getString(R.string.photo_interval),labels,selected,w->{choices.edit().putLong(HomePhotoSettings.INTERVAL,values[w]).apply();refreshScreensaverLabels();});}
    private void chooseStartAfter(){int[] values={5,10,15,30};String[] labels={"5 min","10 min","15 min","30 min"};int selected=0;int now=Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_OFF_TIMEOUT,600000);for(int i=0;i<values.length;i++)if(Math.abs(now-values[i]*60000)<60000)selected=i;SettingsPicker.single(this,getString(R.string.start_after_title),labels,selected,w->{if(Settings.System.canWrite(this))Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_OFF_TIMEOUT,values[w]*60000);updateScreensaverRows();});}

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
        followThemeChoice.setText("Reset theme choices: " + (hasOnTvChoices() ? "Custom" : "Theme defaults"));
        TextView resetOrder = findViewById(R.id.resetOrderChoice);
        if (resetOrder != null) resetOrder.setText("Reset app order");
        TextView startNow = findViewById(R.id.startScreensaverChoice);
        if (startNow != null) startNow.setText("Start now");
        View unhideRow = findViewById(R.id.unhideAppsChoice);
        if (unhideRow instanceof TextView) { int hiddenCount = getSharedPreferences("app_layout", MODE_PRIVATE).getStringSet("hidden", java.util.Collections.emptySet()).size(); ((TextView) unhideRow).setText(getString(R.string.unhide_apps) + ": " + (hiddenCount == 0 ? "None hidden" : hiddenCount + " hidden")); }
        if (appsPerRowValue != null) appsPerRowValue.setText(columns + " apps");
        dnsChoice.setText(getString(R.string.network_dns_current, currentDnsShort()));
        defaultHomeChoice.setText("Default Home: " + currentHomeLabel());
        TextView tvSettings = findViewById(R.id.tvSettingsChoice);
        if (tvSettings != null) tvSettings.setText("TV settings");
        updateScreensaverRows();
        updatePhotosMaster();
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
                if (isFinishing() || clearingCache) return;
                if (clearCacheChoice != null) { clearCacheChoice.setText(storageSummary(snapshot)); formatRow(R.id.clearCacheChoice); }
                setCacheHeader(snapshot);
                updateResources(snapshot);
                updateFreeRamRow();
                if (aboutReadout != null) {
                    String storage;
                    if (snapshot.freeBytes < 0 || snapshot.totalBytes < 0) storage = "Storage unavailable";
                    else storage = "Storage " + android.text.format.Formatter.formatShortFileSize(LauncherSettingsActivity.this, snapshot.freeBytes) + " free of " + android.text.format.Formatter.formatShortFileSize(LauncherSettingsActivity.this, snapshot.totalBytes);
                    aboutReadout.setText(ramReadout() + "\n" + storage);
                }
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
                Formatter.formatShortFileSize(this, snapshot.totalBytes),
                cache);
    }

    private void setCacheHeader(StorageSnapshot snapshot) {
        if (clearCacheHeader == null || clearingCache) return;
        if (snapshot == null || snapshot.freeBytes < 0) clearCacheHeader.setText("Clear cache");
        else clearCacheHeader.setText("Clear cache · " + Formatter.formatShortFileSize(this, snapshot.freeBytes));
    }

    private void clearAppCache() {
        if (clearingCache) return;
        clearingCache = true;
        if (clearCacheChoice != null) {
            clearCacheChoice.setText(R.string.clearing_cache);
            formatRow(R.id.clearCacheChoice);
        }
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
                if (clearCacheChoice != null) {
                    clearCacheChoice.setText(storageSummary(after));
                    formatRow(R.id.clearCacheChoice);
                }
                setCacheHeader(after);
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
        SettingsPicker.buttons(this, getString(R.string.clear_app_cache), message, null,
                null, null, getString(R.string.open_storage_settings), this::openStorageSettings,
                getString(android.R.string.ok), null);
    }

    private TextView freeRamChoice;
    private TextView freeRamHomeChoice;
    private boolean freeingRam;

    /** First row of the landing panel, so Free RAM is on screen as soon as Settings opens. */
    private void addFreeRamHome(LinearLayout card) {
        freeRamHomeChoice = freeRamRow(R.id.freeRamHomeChoice);
        card.addView(freeRamHomeChoice, rowParams());
    }

    /** Advanced, directly under Clear app cache. */
    private void addFreeRamRow(LinearLayout card) {
        freeRamChoice = freeRamRow(R.id.freeRamChoice);
        card.addView(freeRamChoice, rowParams());
    }

    private TextView freeRamRow(int id) {
        TextView row = settingsRow(id);
        row.setOnClickListener(v -> SettingsPicker.confirm(this, "Free RAM",
                "This closes background apps to free memory. It does not remove apps, photos, or your settings.",
                this::freeRam));
        row.setText("Free RAM\nCloses background apps. Asks before it runs.");
        return row;
    }

    private void updateFreeRamRow() {
        if (freeingRam) return;
        android.app.ActivityManager.MemoryInfo info = memoryInfo();
        String value = info.totalMem <= 0 ? "Unavailable" : Formatter.formatShortFileSize(this, info.availMem) + " free";
        showFreeRamRows("Free RAM: " + value);
        if (freeRamHeader != null) freeRamHeader.setText(info.totalMem <= 0 ? "Free RAM" : "Free RAM · " + Formatter.formatShortFileSize(this, info.availMem));
    }

    private void showFreeRamRows(String text) {
        if (freeRamChoice != null) {
            freeRamChoice.setText(text);
            formatRow(R.id.freeRamChoice);
        }
        if (freeRamHomeChoice != null) {
            freeRamHomeChoice.setText(text);
            formatRow(R.id.freeRamHomeChoice);
        }
    }

    private void freeRam() {
        if (freeingRam) return;
        freeingRam = true;
        showFreeRamRows("Free RAM: Closing apps\u2026");
        if (freeRamHeader != null) freeRamHeader.setText("Freeing\u2026");
        final String self = getPackageName();
        final String home = defaultHomePackage();
        new Thread(() -> {
            long before = memoryInfo().availMem;
            int closed = closeBackgroundApps(self, home);
            try {
                Thread.sleep(350L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            long after = memoryInfo().availMem;
            long total = memoryInfo().totalMem;
            mainHandler.post(() -> {
                freeingRam = false;
                if (isFinishing()) return;
                updateFreeRamRow();
                updateStorageSummary();
                showFreeRamResult(before, after, total, closed);
            });
        }, "launcher-free-ram").start();
    }

    private int closeBackgroundApps(String self, String home) {
        android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (am == null) return 0;
        PackageManager pm = getPackageManager();
        java.util.ArrayList<RamFree.Proc> procs = new java.util.ArrayList<>();
        java.util.List<android.app.ActivityManager.RunningAppProcessInfo> running = null;
        try {
            running = am.getRunningAppProcesses();
        } catch (Throwable ignored) {
            running = null;
        }
        if (running != null) {
            int[] pids = new int[running.size()];
            for (int i = 0; i < running.size(); i++) pids[i] = running.get(i).pid;
            android.os.Debug.MemoryInfo[] mem = null;
            try {
                mem = am.getProcessMemoryInfo(pids);
            } catch (Throwable ignored) {
                mem = null;
            }
            for (int i = 0; i < running.size(); i++) {
                android.app.ActivityManager.RunningAppProcessInfo info = running.get(i);
                String pkg = info.pkgList != null && info.pkgList.length > 0 ? info.pkgList[0] : info.processName;
                if (pkg == null) continue;
                int colon = pkg.indexOf(':');
                if (colon > 0) pkg = pkg.substring(0, colon);
                long pss = mem != null && i < mem.length && mem[i] != null ? mem[i].getTotalPss() * 1024L : 0L;
                procs.add(new RamFree.Proc(pkg, info.importance, pss, isSystemPackage(pm, pkg), self.equals(pkg), home.equals(pkg)));
            }
        }
        java.util.List<String> targets = RamFree.targets(procs);
        if (targets.isEmpty()) targets = RamFree.targets(installedThirdParty(pm, self, home));
        int closed = 0;
        for (String pkg : targets) {
            try {
                am.killBackgroundProcesses(pkg);
                closed++;
            } catch (Throwable ignored) {
                // A TV that refuses the call leaves that app running.
            }
        }
        return closed;
    }

    private java.util.List<RamFree.Proc> installedThirdParty(PackageManager pm, String self, String home) {
        java.util.ArrayList<RamFree.Proc> procs = new java.util.ArrayList<>();
        java.util.List<ApplicationInfo> apps;
        try {
            apps = pm.getInstalledApplications(0);
        } catch (Throwable ignored) {
            return procs;
        }
        for (ApplicationInfo info : apps) {
            if (info == null || info.packageName == null) continue;
            boolean system = (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            procs.add(new RamFree.Proc(info.packageName, 0, 0L, system, self.equals(info.packageName), home.equals(info.packageName)));
        }
        return procs;
    }

    private static boolean isSystemPackage(PackageManager pm, String pkg) {
        try {
            ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
            return (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private String defaultHomePackage() {
        ResolveInfo resolved = getPackageManager().resolveActivity(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY);
        if (resolved == null || resolved.activityInfo == null || resolved.activityInfo.packageName == null) return "";
        return resolved.activityInfo.packageName;
    }

    private android.app.ActivityManager.MemoryInfo memoryInfo() {
        android.app.ActivityManager.MemoryInfo info = new android.app.ActivityManager.MemoryInfo();
        android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (am != null) am.getMemoryInfo(info);
        return info;
    }

    private void showFreeRamResult(long before, long after, long total, int closed) {
        String beforeText = before <= 0 ? "unavailable" : Formatter.formatShortFileSize(this, before);
        String afterText = after <= 0 ? "unavailable" : Formatter.formatShortFileSize(this, after);
        String totalText = total <= 0 ? "" : " of " + Formatter.formatShortFileSize(this, total);
        long delta = before > 0 && after > before ? after - before : 0L;
        String message;
        if (closed == 0) message = "No background apps to close.";
        else if (delta >= 1024L * 1024L) message = "Freed " + Formatter.formatShortFileSize(this, delta) + " by closing " + closed + " background apps.";
        else message = "Closed " + closed + " background apps.";
        message += "\nRAM " + beforeText + " free \u2192 " + afterText + " free" + totalText + ".";
        SettingsPicker.buttons(this, "Free RAM", message, null, null, null, null, null, getString(android.R.string.ok), null);
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
        return getString(R.string.dns_manual);
    }

    /** Short label for the Advanced row. The picker still shows the full provider name. */
    private String currentDnsShort() {
        String mode = Global.getString(getContentResolver(), PRIVATE_DNS_MODE);
        if ("off".equals(mode)) return getString(R.string.dns_off);
        if (!"hostname".equals(mode)) return getString(R.string.dns_automatic);
        String host = Global.getString(getContentResolver(), PRIVATE_DNS_SPECIFIER);
        if (host == null || host.trim().isEmpty()) return getString(R.string.dns_automatic);
        for (int i = 2; i < DNS_HOSTS.length; i++) {
            if (DNS_HOSTS[i].equalsIgnoreCase(host)) return DNS_LABELS[i];
        }
        return getString(R.string.dns_manual);
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
        SettingsPicker.single(this, getString(R.string.network_dns), options, checked, which -> {
            if (which == DNS_HOSTS.length) showCustomDns();
            else if (which == 0) writeDns("off", null);
            else if (which == 1) writeDns("opportunistic", null);
            else writeDns("hostname", DNS_HOSTS[which]);
        });
    }

    private void showCustomDns() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setHint(R.string.dns_hostname_hint);
        input.setText(Global.getString(getContentResolver(), PRIVATE_DNS_SPECIFIER));
        input.setSelection(input.length());
        input.setTextColor(ink());
        input.setHintTextColor(0xFFA9ABC7);
        android.widget.FrameLayout box = new android.widget.FrameLayout(this);
        box.setPadding(dp(16), dp(4), dp(16), dp(4));
        box.addView(input, new android.widget.FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        SettingsPicker.buttons(this, getString(R.string.dns_custom), getString(R.string.dns_custom_hint), box,
                getString(android.R.string.cancel), null, null, null, getString(android.R.string.ok), () -> {
                    String host = input.getText().toString().trim();
                    if (!validHostname(host)) {
                        Toast.makeText(this, R.string.dns_invalid_hostname, Toast.LENGTH_LONG).show();
                    } else {
                        writeDns("hostname", host);
                    }
                });
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
            SettingsPicker.notice(this, getString(R.string.dns_permission_title), getString(R.string.dns_permission_message));
            return;
        }
        try {
            if ("hostname".equals(mode)) Global.putString(getContentResolver(), PRIVATE_DNS_SPECIFIER, hostname);
            Global.putString(getContentResolver(), PRIVATE_DNS_MODE, mode);
            refresh();
            Toast.makeText(this, R.string.dns_saved, Toast.LENGTH_SHORT).show();
        } catch (SecurityException ignored) {
            SettingsPicker.notice(this, getString(R.string.dns_permission_title), getString(R.string.dns_permission_message));
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
        SettingsPicker.list(this, getString(R.string.unhide_apps), labels, which -> {
            Set<String> updated = new HashSet<>(layout.getStringSet("hidden", Collections.emptySet()));
            updated.remove(packages.get(which));
            layout.edit().putStringSet("hidden", updated).apply();
            setResult(RESULT_OK);
            showHiddenApps();
        });
    }

    // ---- 2.11.5: per-area fonts (small sub-page of Look) and input names (Apps) ----
    private LinearLayout lookCard,fontsCard;private TextView fontsEntry;private final int[] fontRowIds=new int[Fonts.AREAS.length];
    private TextView settingsRow(int id){TextView t=new TextView(this);t.setId(id);t.setBackground(SettingsStyle.focus(this,SettingsStyle.focusColor(this),false));t.setTextColor(ink());t.setTextSize(17);t.setTypeface(Fonts.of(this,Fonts.SETTINGS,R.font.outfit_medium));t.setPadding(dp(16),dp(6),dp(16),dp(6));t.setMinHeight(dp(rowHeight));t.setMinimumHeight(dp(rowHeight));t.setSingleLine(false);t.setMaxLines(8);t.setEllipsize(null);t.setIncludeFontPadding(false);t.setGravity(android.view.Gravity.CENTER_VERTICAL|android.view.Gravity.START);t.setFocusable(true);t.setClickable(true);SettingsStyle.focusAnim(t);return t;}
    private LinearLayout.LayoutParams rowParams(){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,android.view.ViewGroup.LayoutParams.WRAP_CONTENT);lp.bottomMargin=dp(4);return lp;}
    private void addFontsEntry(LinearLayout card){fontsEntry=settingsRow(R.id.fontChoice);fontsEntry.setOnClickListener(v->showFonts());card.addView(fontsEntry,rowParams());updateFontsEntry();}
    private void updateFontsEntry(){if(fontsEntry==null)return;String first=Fonts.name(this,Fonts.AREAS[0]);boolean same=true;for(String a:Fonts.AREAS)if(!Fonts.name(this,a).equals(first))same=false;fontsEntry.setText("Fonts: "+(same?first:"Mixed")+"\nHome, settings, screensaver and Panchang");formatRow(R.id.fontChoice);if(same)setValueFace(fontsEntry,Fonts.family(this,Fonts.AREAS[0]));}
    private void buildFontsCard(){fontsCard=new LinearLayout(this);fontsCard.setOrientation(LinearLayout.VERTICAL);fontsCard.setPadding(dp(SettingsChrome.PANEL_PAD_H_DP),dp(10),dp(SettingsChrome.PANEL_PAD_H_DP),dp(10));fontsCard.setClipChildren(false);fontsCard.setClipToPadding(false);fontsCard.setBackground(SettingsStyle.card(this));fontsCard.setVisibility(View.GONE);TextView cap=categoryText("Fonts",14,SettingsStyle.accent(this));cap.setPadding(dp(14),dp(8),dp(14),dp(8));cap.setIncludeFontPadding(false);fontsCard.addView(cap);
        for(int i=0;i<Fonts.AREAS.length;i++){final int area=i;fontRowIds[i]=View.generateViewId();TextView t=settingsRow(fontRowIds[i]);t.setOnClickListener(v->chooseFont(area));fontsCard.addView(t,rowParams());}
        fontsCard.addView(hint("Back returns to Appearance. Home and Panchang switch the next time they open."));
        TextView reset=settingsRow(R.id.fontResetChoice);reset.setText("Reset fonts: "+Fonts.DEFAULT);reset.setOnClickListener(v->SettingsPicker.confirm(this,"Reset fonts","Home, settings, screensaver and Panchang go back to "+Fonts.DEFAULT+".",this::resetFonts));fontsCard.addView(reset,rowParams());
        categoryContent.addView(fontsCard,new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,-2));updateFontRows();}
    private void resetFonts(){android.content.SharedPreferences.Editor editor=choices.edit();for(String area:Fonts.AREAS)editor.remove(Fonts.key(area));editor.commit();setResult(RESULT_OK);getIntent().putExtra("open_fonts",true);recreate();}
    private void updateFontRows(){if(fontsCard==null)return;for(int i=0;i<Fonts.AREAS.length;i++){TextView t=fontsCard.findViewById(fontRowIds[i]);if(t==null)continue;t.setText(Fonts.AREA_LABELS[i]+": "+Fonts.name(this,Fonts.AREAS[i]));formatRow(fontRowIds[i]);setValueFace(t,Fonts.family(this,Fonts.AREAS[i]));}TextView reset=fontsCard.findViewById(R.id.fontResetChoice);if(reset!=null){reset.setText("Reset fonts: "+Fonts.DEFAULT);formatRow(R.id.fontResetChoice);}updateFontsEntry();}
    /** Redraw the row's value in the font it names. */
    private void setValueFace(TextView row,int family){Object tag=row.getTag();if(!(tag instanceof String[]))return;android.graphics.drawable.Drawable end=row.getCompoundDrawablesRelative()[2];android.graphics.drawable.Drawable trailing=end instanceof RaiderTheme.ValueDrawable?((RaiderTheme.ValueDrawable)end).trailing:end;row.setCompoundDrawablesRelativeWithIntrinsicBounds(row.getCompoundDrawablesRelative()[0],null,new RaiderTheme.ValueDrawable(this,((String[])tag)[2],SettingsStyle.value(this),trailing,dp(320),Fonts.regular(this,family)),null);}
    private void chooseFont(int area){String key=Fonts.AREAS[area];int cur=Fonts.family(this,key);android.graphics.Typeface[] faces=new android.graphics.Typeface[Fonts.NAMES.length];for(int i=0;i<faces.length;i++)faces[i]=Fonts.regular(this,i);SettingsPicker.single(this,Fonts.AREA_LABELS[area]+" font",Fonts.NAMES,cur,faces,w->{if(w==cur)return;choices.edit().putString(Fonts.key(key),Fonts.NAMES[w]).apply();setResult(RESULT_OK);if(Fonts.SETTINGS.equals(key)){getIntent().putExtra("open_fonts",true);recreate();return;}updateFontRows();});}
    private void showFonts(){if(fontsCard==null||lookCard==null)return;categoryScroll.post(this::fitCurrentCard);lookCard.setVisibility(View.GONE);categoryCards[1]=fontsCard;fontsCard.setVisibility(View.VISIBLE);categoryScroll.scrollTo(0,0);View f=fontsCard.findViewById(fontRowIds[0]);if(f!=null)f.requestFocus();}
    private void hideFonts(){if(fontsCard==null||lookCard==null)return;fontsCard.setVisibility(View.GONE);categoryCards[1]=lookCard;lookCard.setVisibility(View.VISIBLE);if(fontsEntry!=null)fontsEntry.requestFocus();}
    private void addInputNamesRow(LinearLayout card){TextView t=settingsRow(R.id.inputNamesChoice);t.setOnClickListener(v->showInputNamesList());card.addView(t,rowParams());updateInputNamesRow();}
    private void updateInputNamesRow(){TextView t=findViewById(R.id.inputNamesChoice);if(t==null)return;int n=InputNames.count(this);t.setText("Input names: "+(n==0?"None":n+" named"));formatRow(R.id.inputNamesChoice);}
    private void showInputNamesList(){java.util.List<String[]> inputs=InputNames.list(this);String[] items=new String[inputs.size()+1];for(int i=0;i<inputs.size();i++)items[i]=InputNames.display(this,inputs.get(i)[0],inputs.get(i)[1]);items[inputs.size()]="Reset all input names";
        SettingsPicker.list(this,"Input names",items,w->{if(w==inputs.size()){SettingsPicker.buttons(this,"Reset all input names?","Every input goes back to its TV name.",null,getString(android.R.string.cancel),null,null,null,"Reset",()->{InputNames.clearAll(this);setResult(RESULT_OK);updateInputNamesRow();});return;}String[] in=inputs.get(w);InputNames.showRename(this,in[0],in[1],()->{setResult(RESULT_OK);updateInputNamesRow();showInputNamesList();});});}

    // ---- 2.11.7 look: 56 dp rows with a left icon, 48 dp when a page would otherwise scroll ----
    private int rowHeight=56;private int[] categoryIcons;
    /** Right-panel rows stay text-only; left rail keeps its icons. */
    private android.graphics.drawable.Drawable rowIcon(View row){return null;}
    private void fitCurrentCard(){if(categoryScroll==null||categoryCards==null)return;if(selectedCategory==SettingsMenu.HOME){fitHomeCard();return;}if(selectedCategory==4){fitCalendarCard();return;}if(selectedCategory==7){fitAdvancedCard();return;}View card=categoryCards[selectedCategory];if(card==null||card.getHeight()==0)return;if(card.getHeight()+dp(24)>categoryScroll.getHeight())compactRows(card);}
    /** Home Layout stays on one screen: the 2×2 and every control under it share the leanback viewport. */
    private void fitHomeCard(){if(categoryCards==null||categoryCards.length==0||categoryScroll==null||!(categoryCards[SettingsMenu.HOME] instanceof LinearLayout))return;int viewportPx=categoryScroll.getHeight();if(viewportPx<=0){categoryScroll.post(this::fitHomeCard);return;}float density=getResources().getDisplayMetrics().density;int viewportDp=Math.round(viewportPx/density);int bottomDp=categoryContent==null?0:Math.round(categoryContent.getPaddingBottom()/density);HomeLayoutFit.Spec spec=HomeLayoutFit.forViewport(viewportDp,bottomDp);LinearLayout card=(LinearLayout)categoryCards[SettingsMenu.HOME];card.setPadding(dp(SettingsChrome.PANEL_PAD_H_DP),dp(spec.cardPadDp),dp(SettingsChrome.PANEL_PAD_H_DP),dp(spec.cardPadDp));for(int i=0;i<card.getChildCount();i++){View child=card.getChildAt(i);android.widget.LinearLayout.LayoutParams lp=(android.widget.LinearLayout.LayoutParams)child.getLayoutParams();lp.bottomMargin=dp(spec.gapDp);if(child.getId()==R.id.homeLayoutGrid&&child instanceof LinearLayout){layoutHomeGrid((LinearLayout)child,spec);}else if(appsPerRowValue!=null&&child==appsPerRowValue.getParent()){lp.height=dp(spec.bandDp);}else if(child.findViewById(R.id.columns4)!=null&&child instanceof LinearLayout){lp.height=dp(spec.bandDp);layoutHomeChips((LinearLayout)child,spec);}else if(child instanceof TextView){TextView row=(TextView)child;boolean detail=row.getId()==R.id.homePhotosChoice||row.getId()==R.id.homePhotoIntervalChoice;int h=dp(detail?spec.detailDp:spec.rowDp);lp.height=h;row.setMinHeight(h);row.setMinimumHeight(h);row.setPadding(dp(12),dp(2),dp(12),dp(2));row.setGravity(android.view.Gravity.CENTER_VERTICAL|android.view.Gravity.START);if(detail){row.setSingleLine(false);row.setMaxLines(2);row.setEllipsize(android.text.TextUtils.TruncateAt.END);}else{row.setSingleLine(true);row.setMaxLines(1);row.setEllipsize(android.text.TextUtils.TruncateAt.END);}}child.setLayoutParams(lp);}if(selectedCategory==SettingsMenu.HOME&&categoryScroll.getScrollY()!=0)categoryScroll.scrollTo(0,0);}
    private void layoutHomeGrid(LinearLayout grid, HomeLayoutFit.Spec spec){for(int r=0;r<grid.getChildCount();r++){View line=grid.getChildAt(r);android.widget.LinearLayout.LayoutParams lp=(android.widget.LinearLayout.LayoutParams)line.getLayoutParams();lp.bottomMargin=r==0?dp(spec.moduleGapDp):0;line.setLayoutParams(lp);if(!(line instanceof LinearLayout))continue;LinearLayout row=(LinearLayout)line;for(int c=0;c<row.getChildCount();c++){View block=row.getChildAt(c);android.widget.LinearLayout.LayoutParams bp=(android.widget.LinearLayout.LayoutParams)block.getLayoutParams();bp.height=dp(spec.moduleDp);if(c==0)bp.setMarginEnd(dp(spec.moduleGapDp));block.setLayoutParams(bp);if(block instanceof TextView)((TextView)block).setTextSize(spec.moduleDp>=48?16:15);}}}
    private void layoutHomeChips(LinearLayout row, HomeLayoutFit.Spec spec){int h=dp(spec.bandDp);for(int i=0;i<row.getChildCount();i++){View chip=row.getChildAt(i);android.widget.LinearLayout.LayoutParams lp=(android.widget.LinearLayout.LayoutParams)chip.getLayoutParams();lp.height=h;lp.topMargin=0;lp.bottomMargin=0;chip.setLayoutParams(lp);if(chip instanceof TextView){((TextView)chip).setMinHeight(h);((TextView)chip).setMinimumHeight(h);}}}
    private void fitCalendarCard(){if(categoryCards==null||categoryCards.length<=4||categoryScroll==null||!(categoryCards[4] instanceof LinearLayout))return;int viewportPx=categoryScroll.getHeight();if(viewportPx<=0){categoryScroll.post(this::fitCalendarCard);return;}float density=getResources().getDisplayMetrics().density;int viewportDp=Math.round(viewportPx/density);int bottomDp=categoryContent==null?0:Math.round(categoryContent.getPaddingBottom()/density);CalendarPanelFit.Spec spec=CalendarPanelFit.forViewport(viewportDp,bottomDp);LinearLayout card=(LinearLayout)categoryCards[4];card.setPadding(dp(SettingsChrome.PANEL_PAD_H_DP),dp(spec.cardPadDp),dp(SettingsChrome.PANEL_PAD_H_DP),dp(spec.cardPadDp));for(int i=0;i<card.getChildCount();i++){View child=card.getChildAt(i);android.widget.LinearLayout.LayoutParams lp=(android.widget.LinearLayout.LayoutParams)child.getLayoutParams();lp.bottomMargin=dp(spec.gapDp);if(child.getId()==R.id.worldClocksCaption){child.setPadding(dp(12),0,dp(12),0);lp.height=dp(spec.captionDp);}else if(child.getId()==R.id.worldClockCityGrid&&child instanceof LinearLayout){lp.height=android.view.ViewGroup.LayoutParams.WRAP_CONTENT;layoutCityGrid((LinearLayout)child,spec);}else if(child instanceof TextView){int h=dp(spec.rowDp);lp.height=h;TextView row=(TextView)child;row.setMinHeight(h);row.setMinimumHeight(h);row.setPadding(dp(12),0,dp(12),0);row.setGravity(android.view.Gravity.CENTER_VERTICAL|android.view.Gravity.START);row.setSingleLine(true);row.setMaxLines(1);row.setEllipsize(android.text.TextUtils.TruncateAt.END);}child.setLayoutParams(lp);}if(selectedCategory==4&&categoryScroll.getScrollY()!=0)categoryScroll.scrollTo(0,0);}
    /** City 1–4 share one gap between columns and between the two rows. */
    private void layoutCityGrid(LinearLayout grid, CalendarPanelFit.Spec spec){int h=dp(spec.cityDp);int gap=dp(spec.cityGapDp);for(int r=0;r<grid.getChildCount();r++){View line=grid.getChildAt(r);android.widget.LinearLayout.LayoutParams lp=(android.widget.LinearLayout.LayoutParams)line.getLayoutParams();lp.height=h;lp.bottomMargin=r==0?gap:0;line.setLayoutParams(lp);if(!(line instanceof LinearLayout))continue;LinearLayout row=(LinearLayout)line;for(int c=0;c<row.getChildCount();c++){View block=row.getChildAt(c);android.widget.LinearLayout.LayoutParams bp=(android.widget.LinearLayout.LayoutParams)block.getLayoutParams();bp.height=h;bp.setMarginEnd(c==0?gap:0);block.setLayoutParams(bp);if(block instanceof TextView){TextView city=(TextView)block;city.setMinHeight(h);city.setMinimumHeight(h);city.setPadding(dp(12),0,dp(12),0);city.setGravity(android.view.Gravity.CENTER_VERTICAL|android.view.Gravity.START);city.setSingleLine(true);city.setMaxLines(1);city.setEllipsize(android.text.TextUtils.TruncateAt.END);}}}}
    private static boolean isCityRow(int id){return id==R.id.worldCity1Choice||id==R.id.worldCity2Choice||id==R.id.worldCity3Choice||id==R.id.worldCity4Choice;}
    private static boolean keepsOneLine(int id){return isCityRow(id)||id==R.id.openPanchangChoice||id==R.id.openWorldClocksChoice||id==R.id.openWeatherChoice;}
    private static boolean isCalendarRow(int id){return id==R.id.hinduMasterChoice||id==R.id.homeCalendarChoice||id==R.id.calendarChoice||id==R.id.calendarPlaceChoice||id==R.id.calendarTextSizeChoice||id==R.id.worldDreamChoice||id==R.id.openPanchangChoice||id==R.id.openWorldClocksChoice||isCityRow(id);}
    private void compactRows(View v){if(v instanceof TextView){TextView t=(TextView)v;if(t.getMinHeight()==dp(56)){t.setMinHeight(dp(48));t.setMinimumHeight(dp(48));}}if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++)compactRows(g.getChildAt(i));}}
    /** Clear app cache pill: reached by UP from the first row or the first category; DOWN returns to where focus came from. */
    private View headerReturn;
    private void focusHeaderFrom(View from){headerReturn=from;if(clearCacheHeader==null)return;if(from!=null&&from.getId()!=View.NO_ID){clearCacheHeader.setNextFocusDownId(from.getId());if(freeRamHeader!=null)freeRamHeader.setNextFocusDownId(from.getId());}clearCacheHeader.requestFocus();}
    private void wireHeaderActions(){clearCacheHeader=findViewById(R.id.clearCacheHeader);freeRamHeader=findViewById(R.id.freeRamHeader);styleHeaderPill(clearCacheHeader);styleHeaderPill(freeRamHeader);if(clearCacheHeader!=null)clearCacheHeader.setOnClickListener(v->SettingsPicker.confirm(this,"Clear cache","This frees cached data for apps on this TV. It does not remove apps, photos, or your settings.",this::clearAppCache));if(freeRamHeader!=null)freeRamHeader.setOnClickListener(v->SettingsPicker.confirm(this,"Free RAM","This closes background apps to free memory. It does not remove apps, photos, or your settings.",this::freeRam));if(clearCacheHeader!=null&&freeRamHeader!=null){clearCacheHeader.setNextFocusRightId(freeRamHeader.getId());freeRamHeader.setNextFocusLeftId(clearCacheHeader.getId());}}
    private void styleHeaderPill(TextView pill){if(pill==null)return;pill.setTextColor(ink());pill.setMinWidth(0);pill.setMinimumWidth(0);pill.setBackground(headerChip(false));pill.setOnFocusChangeListener((v,focused)->{v.setBackground(headerChip(focused));SettingsStyle.scale(v,focused);});android.graphics.drawable.Drawable icon=pill.getCompoundDrawablesRelative()[0];if(icon!=null){icon=icon.mutate();icon.setBounds(0,0,dp(16),dp(16));pill.setCompoundDrawablesRelative(icon,null,null,null);}}
    /** Clear cache and Free RAM. Idle and focus both use the shared 4dp settings corner, not a capsule. */
    private android.graphics.drawable.Drawable headerChip(boolean focused){return focused?SettingsStyle.focus(this,SettingsStyle.focusColor(this),true):SettingsStyle.card(this);}
    /** Equal gutter on both sides of the right-hand card, and the same inset on the header's end. */
    private void insetContentPanel(){int gutter=dp(SettingsChrome.PANEL_GUTTER_DP);if(categoryScroll!=null){LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)categoryScroll.getLayoutParams();lp.setMarginStart(gutter);lp.setMarginEnd(gutter);categoryScroll.setLayoutParams(lp);}View header=findViewById(R.id.settingsHeaderBar);if(header!=null)header.setPaddingRelative(header.getPaddingStart(),header.getPaddingTop(),gutter,header.getPaddingBottom());}
    private int railSpaceTries;
    /** Even gaps between rail rows, using the real column height so a 1080p screen is not cramped at the top. */
    private void spaceRail(){if(categoryRail==null||railItems==null)return;categoryRail.post(()->{android.view.ViewParent parent=categoryRail.getParent();if(!(parent instanceof View))return;int viewport=((View)parent).getHeight();if(viewport<=0){if(railSpaceTries++<3)categoryRail.post(this::spaceRail);return;}float density=getResources().getDisplayMetrics().density;int[] gaps=SettingsChrome.railGapsPx(viewport,railItems.length,density);int pad=Math.round(SettingsChrome.RAIL_PAD_DP*density);categoryRail.setPadding(pad,pad,pad,pad);int row=Math.round(SettingsChrome.RAIL_ROW_DP*density);for(int i=0;i<railItems.length;i++){LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)railItems[i].getLayoutParams();lp.height=row;lp.bottomMargin=i<gaps.length?gaps[i]:0;railItems[i].setLayoutParams(lp);}});}
    private void tightenRail(){if(categoryRail==null||railItems==null)return;int max=0;int height=dp(SettingsChrome.RAIL_ROW_DP);for(View item:railItems){item.measure(View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));if(item.getMeasuredWidth()>max)max=item.getMeasuredWidth();}if(max<=0)return;for(View item:railItems){LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)item.getLayoutParams();lp.width=max;item.setLayoutParams(lp);}}
    private void fitAdvancedCard(){if(categoryCards==null||categoryCards.length<=7||categoryScroll==null||!(categoryCards[7] instanceof LinearLayout))return;int viewportPx=categoryScroll.getHeight();if(viewportPx<=0){categoryScroll.post(this::fitAdvancedCard);return;}float density=getResources().getDisplayMetrics().density;int viewportDp=Math.round(viewportPx/density);int bottomDp=categoryContent==null?0:Math.round(categoryContent.getPaddingBottom()/density);AdvancedPanelFit.Spec spec=AdvancedPanelFit.forViewport(viewportDp,bottomDp);LinearLayout card=(LinearLayout)categoryCards[7];card.setPadding(dp(SettingsChrome.PANEL_PAD_H_DP),dp(spec.cardPadDp),dp(SettingsChrome.PANEL_PAD_H_DP),dp(spec.cardPadDp));for(int i=0;i<card.getChildCount();i++){View child=card.getChildAt(i);android.widget.LinearLayout.LayoutParams lp=(android.widget.LinearLayout.LayoutParams)child.getLayoutParams();lp.bottomMargin=dp(spec.gapDp);if(child instanceof TextView){TextView t=(TextView)child;boolean caption=t.getId()==View.NO_ID;int h=dp(caption?spec.captionDp:spec.rowDp);lp.height=h;t.setMinHeight(h);t.setMinimumHeight(h);t.setMaxLines(1);t.setSingleLine(true);t.setEllipsize(android.text.TextUtils.TruncateAt.END);t.setPadding(dp(12),0,dp(12),0);t.setGravity(android.view.Gravity.CENTER_VERTICAL|android.view.Gravity.START);if(!caption)t.setTextSize(15);}child.setLayoutParams(lp);}if(selectedCategory==7&&categoryScroll.getScrollY()!=0)categoryScroll.scrollTo(0,0);}
    // ---- Weather city (Open-Meteo geocoding) and units. The city picker is the only location control. ----
    private void addWeatherRows(LinearLayout card){TextView open=settingsRow(R.id.openWeatherChoice);open.setText(getString(R.string.open_weather_now));open.setOnClickListener(v->open(new Intent(this,WeatherActivity.class)));card.addView(open,0,rowParams());TextView city=settingsRow(R.id.weatherCityChoice);city.setOnClickListener(v->showCitySearch(""));card.addView(city,rowParams());TextView units=settingsRow(R.id.weatherUnitsChoice);units.setOnClickListener(v->{WeatherLocation.setFahrenheit(this,!WeatherLocation.fahrenheit(this));setResult(RESULT_OK);updateWeatherRows();});card.addView(units,rowParams());updateWeatherRows();}
    private void updateWeatherRows(){TextView city=findViewById(R.id.weatherCityChoice),units=findViewById(R.id.weatherUnitsChoice);if(city==null)return;boolean hasCity=WeatherLocation.hasCity(this);String name=WeatherLocation.cityName(this);
        city.setText("City: "+(hasCity?(name.isEmpty()?"Saved location":name):"Not set")+"\nSearch by name; also sets the Panchang location");formatRow(R.id.weatherCityChoice);
        units.setText("Units: "+(WeatherLocation.fahrenheit(this)?"\u00B0F":"\u00B0C"));formatRow(R.id.weatherUnitsChoice);}
    private void showCitySearch(String initial){android.widget.EditText in=new android.widget.EditText(this);in.setSingleLine(true);in.setHint("City name");in.setText(initial);in.setSelection(in.getText().length());in.setTextColor(ink());in.setHintTextColor(0xFFA9ABC7);in.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);in.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);android.widget.FrameLayout box=new android.widget.FrameLayout(this);box.setPadding(dp(16),dp(4),dp(16),dp(4));box.addView(in,new android.widget.FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        final android.app.Dialog[] held=new android.app.Dialog[1];in.setOnEditorActionListener((v,a,e)->{if(a==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH||a==android.view.inputmethod.EditorInfo.IME_ACTION_DONE){if(held[0]!=null)held[0].dismiss();searchCity(in.getText().toString());return true;}return false;});held[0]=SettingsPicker.form(this,"Weather city",box,getString(android.R.string.cancel),null,null,null,"Search",()->searchCity(in.getText().toString()));}
    private void searchCity(String q){if(q==null||q.trim().length()<2){Toast.makeText(this,"Type at least 2 letters",Toast.LENGTH_SHORT).show();return;}Toast.makeText(this,"Searching\u2026",Toast.LENGTH_SHORT).show();new Thread(()->{java.util.List<WeatherLocation.Place> r;String err=null;try{r=WeatherLocation.search(q);}catch(Exception e){r=java.util.Collections.emptyList();err=e.getClass().getSimpleName();}final java.util.List<WeatherLocation.Place> res=r;final String error=err;runOnUiThread(()->{if(isFinishing())return;if(error!=null){Toast.makeText(this,"City search failed ("+error+")",Toast.LENGTH_LONG).show();return;}if(res.isEmpty()){Toast.makeText(this,"No matches for \u201C"+q.trim()+"\u201D",Toast.LENGTH_SHORT).show();showCitySearch(q);return;}String[] items=new String[res.size()];for(int i=0;i<items.length;i++)items[i]=res.get(i).label;SettingsPicker.list(this,"Choose a city",items,w->{WeatherLocation.useCity(this,res.get(w));setResult(RESULT_OK);updateWeatherRows();Toast.makeText(this,"Weather: "+res.get(w).name,Toast.LENGTH_SHORT).show();});});},"city-search").start();}

    private void tintRailIcon(TextView rail, boolean selected, boolean focused){if(rail==null)return;android.graphics.drawable.Drawable icon=rail.getCompoundDrawablesRelative()[0];if(icon!=null)icon.setTint(selected?RaiderColors.inkOn(SettingsStyle.focusColor(this)):(focused?ink():RaiderTheme.theme(this).secondary));}
    private TextView aboutReadout;
    private void buildAbout(LinearLayout card){String link="github.com/apukale/raider-launcher";String head="Raider Launcher "+BuildConfig.VERSION_NAME+" ("+BuildConfig.VERSION_CODE+")\nMade by apukale\n";String tail="\nFork of CleanYourTV";android.text.SpannableString about=new android.text.SpannableString(head+link+tail);about.setSpan(new ForegroundColorSpan(SettingsStyle.value(this)),head.length(),head.length()+link.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);int fork=head.length()+link.length();about.setSpan(new AbsoluteSizeSpan(13,true),fork,about.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);about.setSpan(new ForegroundColorSpan(0xFFA9ABC7),fork,about.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);TextView t=categoryText("",16,0xFFF4F1EA);t.setText(about);t.setIncludeFontPadding(false);LinearLayout aboutRow=new LinearLayout(this);aboutRow.setOrientation(LinearLayout.HORIZONTAL);aboutRow.addView(t,new LinearLayout.LayoutParams(0,android.view.ViewGroup.LayoutParams.WRAP_CONTENT,1f));LinearLayout qrCol=new LinearLayout(this);qrCol.setOrientation(LinearLayout.VERTICAL);qrCol.setGravity(android.view.Gravity.CENTER_HORIZONTAL);qrCol.setPadding(dp(16),dp(8),dp(16),0);android.widget.ImageView qr=new android.widget.ImageView(this);qr.setImageResource(R.drawable.about_qr);qr.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);qr.setContentDescription("QR code for https://github.com/apukale/raider-launcher");android.graphics.drawable.Drawable qd=qr.getDrawable();if(qd instanceof android.graphics.drawable.BitmapDrawable)((android.graphics.drawable.BitmapDrawable)qd).setFilterBitmap(false);qrCol.addView(qr,new LinearLayout.LayoutParams(dp(112),dp(112)));TextView scan=categoryText("Scan for GitHub",13,SettingsStyle.SECONDARY);scan.setPadding(0,dp(6),0,0);scan.setIncludeFontPadding(false);qrCol.addView(scan);aboutRow.addView(qrCol,new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT,android.view.ViewGroup.LayoutParams.WRAP_CONTENT));card.addView(aboutRow);aboutReadout=categoryText("RAM\nStorage",15,0xFFCAC4D0);aboutReadout.setPadding(dp(8),dp(4),dp(8),dp(8));aboutReadout.setIncludeFontPadding(false);card.addView(aboutReadout);}
    private String ramReadout(){android.app.ActivityManager am=(android.app.ActivityManager)getSystemService(ACTIVITY_SERVICE);android.app.ActivityManager.MemoryInfo info=new android.app.ActivityManager.MemoryInfo();if(am!=null)am.getMemoryInfo(info);if(info.totalMem<=0)return "RAM unavailable";return "RAM "+android.text.format.Formatter.formatShortFileSize(this,info.availMem)+" free of "+android.text.format.Formatter.formatShortFileSize(this,info.totalMem);}
    private void addCalendarPlace(LinearLayout card){TextView row=settingsRow(R.id.calendarPlaceChoice);row.setOnClickListener(v->chooseCalendarPlace());card.addView(row,rowParams());updateCalendarPlace(choices.getBoolean(LauncherPrefs.HINDU,true));}
    private void updateCalendarPlace(boolean hindu){TextView row=findViewById(R.id.calendarPlaceChoice);if(row==null)return;boolean storedHome=choices.getBoolean(CalendarPlacement.HOME,true);boolean storedDream=choices.getBoolean(CalendarPlacement.DREAM,true);int mode=CalendarPlacement.mode(storedHome,storedDream);boolean home=CalendarPlacement.showOnHome(mode);boolean dream=CalendarPlacement.showOnScreensaver(mode);if(storedHome!=home||storedDream!=dream){choices.edit().putBoolean(CalendarPlacement.HOME,home).putBoolean(CalendarPlacement.DREAM,dream).apply();setResult(RESULT_OK);}row.setText("Show: "+CalendarPlacement.LABELS[mode]);row.setAlpha(hindu?1f:0.45f);}
    private void chooseCalendarPlace(){boolean storedHome=choices.getBoolean(CalendarPlacement.HOME,true);boolean storedDream=choices.getBoolean(CalendarPlacement.DREAM,true);SettingsPicker.single(this,"Hindu calendar",CalendarPlacement.LABELS,CalendarPlacement.mode(storedHome,storedDream),w->{choices.edit().putBoolean(CalendarPlacement.HOME,CalendarPlacement.showOnHome(w)).putBoolean(CalendarPlacement.DREAM,CalendarPlacement.showOnScreensaver(w)).apply();setResult(RESULT_OK);refresh();});}
    private void addOpenPanchang(LinearLayout card){TextView t=settingsRow(R.id.openPanchangChoice);t.setText(getString(R.string.open_panchang));t.setOnClickListener(v->open(new Intent(this,PanchangActivity.class)));card.addView(t,rowParams());}
    private void addHinduMaster(LinearLayout card){TextView t=settingsRow(R.id.hinduMasterChoice);t.setOnClickListener(v->{boolean on=choices.getBoolean(LauncherPrefs.HINDU,true);choices.edit().putBoolean(LauncherPrefs.HINDU,!on).apply();setResult(RESULT_OK);refresh();});card.addView(t,rowParams());}
    private static final int[] WORLD_CITY_IDS={R.id.worldCity1Choice,R.id.worldCity2Choice,R.id.worldCity3Choice,R.id.worldCity4Choice};
    private void addWorldClocks(LinearLayout card){TextView cap=categoryText("World clocks",13,SettingsStyle.accent(this));cap.setId(R.id.worldClocksCaption);cap.setPadding(dp(12),dp(2),dp(12),dp(2));cap.setIncludeFontPadding(false);card.addView(cap);TextView open=settingsRow(R.id.openWorldClocksChoice);open.setText(getString(R.string.open_world_clocks));open.setOnClickListener(v->open(new Intent(this,WorldClocksActivity.class)));open.setNextFocusDownId(WORLD_CITY_IDS[0]);card.addView(open,rowParams());card.addView(worldCityGrid(),rowParams());TextView dream=settingsRow(R.id.worldDreamChoice);dream.setOnClickListener(v->{choices.edit().putBoolean(WorldClocks.DREAM,!choices.getBoolean(WorldClocks.DREAM,false)).apply();setResult(RESULT_OK);refresh();});dream.setNextFocusUpId(WORLD_CITY_IDS[2]);card.addView(dream,rowParams());}
    /** City 1, City 2 on the first row; City 3, City 4 on the second. Same pickers as the old list. */
    private LinearLayout worldCityGrid(){LinearLayout grid=new LinearLayout(this);grid.setId(R.id.worldClockCityGrid);grid.setOrientation(LinearLayout.VERTICAL);grid.setClipChildren(false);grid.setClipToPadding(false);TextView[] cities=new TextView[WORLD_CITY_IDS.length];for(int row=0;row<CalendarPanelFit.CITY_ROWS;row++){LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);line.setClipChildren(false);line.setClipToPadding(false);for(int col=0;col<CalendarPanelFit.CITY_COLUMNS;col++){int slot=row*CalendarPanelFit.CITY_COLUMNS+col;TextView city=settingsRow(WORLD_CITY_IDS[slot]);styleCityBlock(city);final int picked=slot;city.setOnClickListener(v->chooseWorldCity(picked));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(40),1f);if(col==0)lp.setMarginEnd(dp(8));line.addView(city,lp);cities[slot]=city;}LinearLayout.LayoutParams rowLp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);if(row==0)rowLp.bottomMargin=dp(8);grid.addView(line,rowLp);}cities[0].setNextFocusRightId(cities[1].getId());cities[0].setNextFocusDownId(cities[2].getId());cities[0].setNextFocusUpId(R.id.openWorldClocksChoice);cities[1].setNextFocusLeftId(cities[0].getId());cities[1].setNextFocusDownId(cities[3].getId());cities[1].setNextFocusRightId(cities[1].getId());cities[1].setNextFocusUpId(R.id.openWorldClocksChoice);cities[2].setNextFocusUpId(cities[0].getId());cities[2].setNextFocusRightId(cities[3].getId());cities[2].setNextFocusDownId(R.id.worldDreamChoice);cities[3].setNextFocusUpId(cities[1].getId());cities[3].setNextFocusLeftId(cities[2].getId());cities[3].setNextFocusRightId(cities[3].getId());cities[3].setNextFocusDownId(R.id.worldDreamChoice);return grid;}
    private void styleCityBlock(TextView city){city.setBackground(homeLayoutRest());city.setOnFocusChangeListener((v,focused)->{v.setBackground(focused?SettingsStyle.focus(this,SettingsStyle.focusColor(this),true):homeLayoutRest());SettingsStyle.scale(v,focused);});}
    private String storedClock(String key){return choices.contains(key)?choices.getString(key,""):null;}
    private void updateWorldClockRows(){TextView first=findViewById(WORLD_CITY_IDS[0]);if(first==null)return;for(int i=0;i<WORLD_CITY_IDS.length;i++){TextView city=findViewById(WORLD_CITY_IDS[i]);String storedName=storedClock(WorldClocks.nameKey(i));String storedZone=storedClock(WorldClocks.zoneKey(i));String label=WorldClocks.nameAt(i,storedName,storedZone);city.setText(WorldClocks.settingsLine(i,label));}TextView dream=findViewById(R.id.worldDreamChoice);dream.setText("Show world clocks on Screensaver: "+getString(choices.getBoolean(WorldClocks.DREAM,false)?R.string.on:R.string.off));}
    private void chooseWorldCity(int slot){String storedName=storedClock(WorldClocks.nameKey(slot));String storedZone=storedClock(WorldClocks.zoneKey(slot));String zone=WorldClocks.zoneAt(slot,storedZone);String label=WorldClocks.nameAt(slot,storedName,storedZone);String time=WorldClocks.formatTime(zone,System.currentTimeMillis());String message=label.equals(zone)?zone+"\n"+time:label+"\n"+zone+" \u00B7 "+time;boolean custom=storedName!=null||storedZone!=null;String clearLabel=custom?"Clear":null;Runnable clear=clearLabel==null?null:()->{choices.edit().remove(WorldClocks.nameKey(slot)).remove(WorldClocks.zoneKey(slot)).apply();setResult(RESULT_OK);refresh();};SettingsPicker.buttons(this,"City "+(slot+1),message,null,"Timezone",()->pickWorldZone(slot),"Rename",()->renameWorldSlot(slot),clearLabel,clear);}
    private void pickWorldZone(int slot){String current=WorldClocks.zoneAt(slot,storedClock(WorldClocks.zoneKey(slot)));String[] zones=WorldClocks.zones();SettingsPicker.filter(this,"Timezone",q->{java.util.List<String> matches=WorldClocks.filterZones(zones,q);return matches.toArray(new String[0]);},current,zone->{choices.edit().putString(WorldClocks.zoneKey(slot),zone).apply();setResult(RESULT_OK);refresh();});}
    private void renameWorldSlot(int slot){android.widget.EditText in=new android.widget.EditText(this);in.setSingleLine(true);in.setHint("Display name");String name=choices.getString(WorldClocks.nameKey(slot),"");String zone=choices.getString(WorldClocks.zoneKey(slot),"");if(name!=null)in.setText(name.trim());in.setSelection(in.getText().length());in.setTextColor(ink());in.setHintTextColor(0xFFA9ABC7);in.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);in.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);android.widget.FrameLayout box=new android.widget.FrameLayout(this);box.setPadding(dp(16),dp(4),dp(16),dp(4));box.addView(in,new android.widget.FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,android.view.ViewGroup.LayoutParams.WRAP_CONTENT));final android.app.Dialog[] held=new android.app.Dialog[1];Runnable save=()->{String typed=in.getText().toString().trim();if(typed.isEmpty()||typed.equals(zone==null?"":zone.trim()))choices.edit().remove(WorldClocks.nameKey(slot)).apply();else choices.edit().putString(WorldClocks.nameKey(slot),typed).apply();setResult(RESULT_OK);refresh();};in.setOnEditorActionListener((v,a,e)->{if(a==android.view.inputmethod.EditorInfo.IME_ACTION_DONE){if(held[0]!=null)held[0].dismiss();save.run();return true;}return false;});held[0]=SettingsPicker.form(this,"Rename",box,getString(android.R.string.cancel),null,null,null,"Save",save);}
    private void addKidsRow(LinearLayout card){TextView t=settingsRow(R.id.kidsChoice);t.setOnClickListener(v->SettingsPicker.notice(this,"Kids","Coming soon."));card.addView(t,rowParams());t.setText("Kids: Coming soon");}
    /** Weather, World clocks, Panchang, Screensaver. Sits above the other Home Layout controls. */
    private void addHomeLayoutModules(LinearLayout card) {
        int[] ids = {R.id.homeLayoutWeather, R.id.homeLayoutClocks, R.id.homeLayoutPanchang, R.id.homeLayoutScreensaver};
        int[] labels = {R.string.home_module_weather, R.string.home_module_clocks, R.string.home_module_panchang, R.string.home_module_screensaver};
        int[] icons = {R.drawable.ic_cat_weather, R.drawable.ic_clock, R.drawable.ic_cat_calendar, R.drawable.ic_cat_screensaver};
        LinearLayout grid = new LinearLayout(this);
        grid.setId(R.id.homeLayoutGrid);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setClipChildren(false);
        grid.setClipToPadding(false);
        TextView[] blocks = new TextView[HomeModules.COUNT];
        for (int row = 0; row < 2; row++) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setClipChildren(false);
            for (int col = 0; col < HomeModules.COLUMNS; col++) {
                int index = row * HomeModules.COLUMNS + col;
                TextView block = homeLayoutBlock(ids[index], labels[index], icons[index]);
                final int picked = index;
                block.setOnClickListener(v -> openHomeLayoutModule(picked));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(56), 1f);
                if (col == 0) lp.setMarginEnd(dp(8));
                line.addView(block, lp);
                blocks[index] = block;
            }
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (row == 0) rowLp.bottomMargin = dp(8);
            grid.addView(line, rowLp);
        }
        blocks[HomeModules.WEATHER].setNextFocusRightId(blocks[HomeModules.CLOCKS].getId());
        blocks[HomeModules.WEATHER].setNextFocusDownId(blocks[HomeModules.PANCHANG].getId());
        blocks[HomeModules.CLOCKS].setNextFocusLeftId(blocks[HomeModules.WEATHER].getId());
        blocks[HomeModules.CLOCKS].setNextFocusDownId(blocks[HomeModules.SCREENSAVER].getId());
        blocks[HomeModules.CLOCKS].setNextFocusRightId(blocks[HomeModules.CLOCKS].getId());
        blocks[HomeModules.PANCHANG].setNextFocusUpId(blocks[HomeModules.WEATHER].getId());
        blocks[HomeModules.PANCHANG].setNextFocusRightId(blocks[HomeModules.SCREENSAVER].getId());
        blocks[HomeModules.PANCHANG].setNextFocusDownId(R.id.columns4);
        blocks[HomeModules.SCREENSAVER].setNextFocusUpId(blocks[HomeModules.CLOCKS].getId());
        blocks[HomeModules.SCREENSAVER].setNextFocusLeftId(blocks[HomeModules.PANCHANG].getId());
        blocks[HomeModules.SCREENSAVER].setNextFocusRightId(blocks[HomeModules.SCREENSAVER].getId());
        blocks[HomeModules.SCREENSAVER].setNextFocusDownId(R.id.columns6);
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        gridLp.bottomMargin = dp(12);
        card.addView(grid, gridLp);
    }

    private TextView homeLayoutBlock(int id, int label, int icon) {
        TextView block = new TextView(this);
        block.setId(id);
        block.setText(label);
        block.setTextColor(ink());
        block.setTextSize(16);
        block.setTypeface(Fonts.of(this, Fonts.SETTINGS, R.font.outfit_medium));
        block.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START);
        block.setPadding(dp(14), 0, dp(12), 0);
        block.setSingleLine(true);
        block.setEllipsize(android.text.TextUtils.TruncateAt.END);
        block.setIncludeFontPadding(false);
        block.setFocusable(true);
        block.setClickable(true);
        android.graphics.drawable.Drawable mark = getDrawable(icon);
        if (mark != null) {
            mark = mark.mutate();
            mark.setTint(SettingsStyle.accent(this));
            int size = dp(18);
            mark.setBounds(0, 0, size, size);
            block.setCompoundDrawablesRelative(mark, null, null, null);
            block.setCompoundDrawablePadding(dp(8));
        }
        block.setBackground(homeLayoutRest());
        block.setOnFocusChangeListener((v, focused) -> {
            v.setBackground(focused ? SettingsStyle.focus(this, SettingsStyle.focusColor(this), true) : homeLayoutRest());
            SettingsStyle.scale(v, focused);
        });
        return block;
    }

    private GradientDrawable homeLayoutRest() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(RaiderTheme.theme(this).surface);
        drawable.setCornerRadius(dp(SettingsStyle.RAIL_CORNER_DP));
        return drawable;
    }

    private void openHomeLayoutModule(int index) {
        if (index == HomeModules.WEATHER) open(new Intent(this, WeatherActivity.class));
        else if (index == HomeModules.CLOCKS) open(new Intent(this, WorldClocksActivity.class));
        else if (index == HomeModules.PANCHANG) open(new Intent(this, PanchangActivity.class));
        else if (index == HomeModules.SCREENSAVER) open(new Intent(this, ScreensaverActivity.class));
    }

    private void linkHomeLayoutModules() {
        int[] left = {R.id.columns4, R.id.columns5};
        int[] right = {R.id.columns6, R.id.columns7};
        for (int id : left) {
            View chip = findViewById(id);
            if (chip != null) chip.setNextFocusUpId(R.id.homeLayoutPanchang);
        }
        for (int id : right) {
            View chip = findViewById(id);
            if (chip != null) chip.setNextFocusUpId(R.id.homeLayoutScreensaver);
        }
    }

    private void addHomePhotoRows(LinearLayout card){TextView photos=settingsRow(R.id.homePhotosChoice);photos.setOnClickListener(v->{boolean on=PhotosMaster.on(this);choices.edit().putBoolean(HomePhotoSettings.MASTER,!on).apply();setResult(RESULT_OK);PhotoStore.schedule(this,true);refresh();});card.addView(photos,rowParams());TextView interval=settingsRow(R.id.homePhotoIntervalChoice);interval.setOnClickListener(v->{if(!HomePhotoSettings.intervalInteractive(PhotosMaster.on(this)))return;choosePhotoInterval();});card.addView(interval,rowParams());updateHomePhotoRows();}
    private void updateHomePhotoRows(){TextView photos=findViewById(R.id.homePhotosChoice);TextView interval=findViewById(R.id.homePhotoIntervalChoice);if(photos==null&&interval==null)return;boolean on=PhotosMaster.on(this);if(photos!=null){photos.setText("Photos: "+getString(on?R.string.on:R.string.off)+"\n"+getString(on?R.string.photos_home_on:R.string.photos_home_off));setToggle(photos,on);formatRow(R.id.homePhotosChoice);}if(interval!=null){long ms=choices.getLong(HomePhotoSettings.INTERVAL,HomeFocusPhoto.INTERVAL_DEFAULT_MS);String text=getString(R.string.photo_interval,HomeFocusPhoto.intervalLabel(ms));if(!on)text=text+"\n"+getString(R.string.photo_interval_paused);interval.setText(text);formatRow(R.id.homePhotoIntervalChoice);interval.setEnabled(HomePhotoSettings.intervalInteractive(on));interval.setAlpha(HomePhotoSettings.intervalAlpha(on));}}
    private void addPhotosMaster(LinearLayout card){TextView t=settingsRow(R.id.photosMasterChoice);t.setText("Photos: "+getString(R.string.on));t.setOnClickListener(v->{boolean on=PhotosMaster.on(this);choices.edit().putBoolean(PhotosMaster.PREF,!on).apply();setResult(RESULT_OK);PhotoStore.schedule(this,true);refresh();});card.addView(t,0,rowParams());}
    private void updatePhotosMaster(){TextView t=findViewById(R.id.photosMasterChoice);if(t==null)return;boolean on=PhotosMaster.on(this);t.setText("Photos: "+getString(on?R.string.on:R.string.off)+"\n"+getString(on?R.string.photos_home_on:R.string.photos_home_off));setToggle(t,on);formatRow(R.id.photosMasterChoice);}
    private TextView resourcesReadout;
    private void addResourcesRow(LinearLayout card){TextView t=settingsRow(R.id.resourcesChoice);t.setFocusable(false);t.setClickable(false);t.setText("Resources\nReading\u2026");resourcesReadout=t;card.addView(t,rowParams());}
    private void updateResources(StorageSnapshot snapshot){if(resourcesReadout==null)return;String storage=snapshot.freeBytes<0?"Storage unavailable":"Storage "+android.text.format.Formatter.formatShortFileSize(this,snapshot.freeBytes)+" free of "+android.text.format.Formatter.formatShortFileSize(this,snapshot.totalBytes);resourcesReadout.setText("Resources: "+launcherRam()+"\n"+ramReadout()+"\n"+storage);formatRow(R.id.resourcesChoice);}
    private String launcherRam(){android.app.ActivityManager am=(android.app.ActivityManager)getSystemService(ACTIVITY_SERVICE);if(am==null)return "Launcher RAM unavailable";android.os.Debug.MemoryInfo[] info=am.getProcessMemoryInfo(new int[]{android.os.Process.myPid()});if(info==null||info.length==0)return "Launcher RAM unavailable";return "Launcher "+android.text.format.Formatter.formatShortFileSize(this,info[0].getTotalPss()*1024L);}

}
