package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityEvent;

/** Opt-in bridge for opening Android's power menu; it does not inspect windows or handle events. */
public class PowerMenuService extends AccessibilityService {

    private static volatile PowerMenuService instance;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.eventTypes = 0;
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
            info.flags = 0;
            info.packageNames = null;
            setServiceInfo(info);
        }
        instance = this;
    }

    static boolean openPowerDialog() {
        PowerMenuService service = instance;
        if (service == null) return false;
        try {
            return service.performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        if (instance == this) instance = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }
}
