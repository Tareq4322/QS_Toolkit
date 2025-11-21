package com.cominatyou.batterytile.standalone;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;

public class TileAccessibilityService extends AccessibilityService {

    public static final String ACTION_LOCK_SCREEN = "ACTION_LOCK_SCREEN";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_LOCK_SCREEN.equals(intent.getAction())) {
            // This is the magic line that locks the phone
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
        }
        return START_STICKY;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // We don't need to listen to events, just perform actions
    }

    @Override
    public void onInterrupt() {
        // Required method, but we don't need to do anything here
    }

    // Helper to check if the user has actually enabled this service in Settings
    public static boolean isServiceEnabled(Context context) {
        String expectedServiceName = context.getPackageName() + "/" + TileAccessibilityService.class.getCanonicalName();
        String enabledServices = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        
        if (TextUtils.isEmpty(enabledServices)) {
            return false;
        }
        
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabledServices);
        
        while (splitter.hasNext()) {
            String componentName = splitter.next();
            if (componentName.equalsIgnoreCase(expectedServiceName)) {
                return true;
            }
        }
        return false;
    }
}
