package com.cominatyou.batterytile.standalone;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import com.cominatyou.batterytile.standalone.DnsTileService;
import com.cominatyou.batterytile.standalone.LockTileService;
import com.cominatyou.batterytile.standalone.QuickSettingsTileService;
import com.cominatyou.batterytile.standalone.VolumeTileService;

import java.util.List;

public class QuickSettingsTileLongPressHandler extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the component name of the tile that triggered this activity
        ComponentName componentName = getIntent().getParcelableExtra(Intent.EXTRA_COMPONENT_NAME);

        if (componentName == null) {
            openAppSettings();
            finish();
            return;
        }

        String className = componentName.getClassName();
        Intent targetIntent = null;

        // --- ROUTING LOGIC ---

        // 1. Battery Tile -> System Battery Settings
        if (className.equals(QuickSettingsTileService.class.getName())) {
            targetIntent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
        }
        
        // 2. Volume Tile -> TOGGLE RINGER MODE
        else if (className.equals(VolumeTileService.class.getName())) {
            toggleRingerMode();
            finish(); // Close immediately, no UI needed
            return;
        }

        // 3. DNS Tile -> System Network Settings
        else if (className.equals(DnsTileService.class.getName())) {
            targetIntent = new Intent("android.settings.NETWORK_AND_INTERNET_SETTINGS");
        }

        // 4. Lock Screen Tile -> App Settings (to manage Accessibility)
        else if (className.equals(LockTileService.class.getName())) {
            openAppSettings();
            finish();
            return;
        }
        
        // --- EXECUTE ---

        if (targetIntent != null) {
            try {
                targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(targetIntent);
            } catch (Exception e) {
                // Fallbacks
                if (className.equals(DnsTileService.class.getName())) {
                    try {
                        Intent fallback = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                        fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(fallback);
                    } catch (Exception ex) {
                        openAppSettings();
                    }
                } else {
                    try {
                        startActivity(new Intent(Settings.ACTION_SETTINGS));
                    } catch (Exception ex2) {
                        Toast.makeText(this, "Could not open settings", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } else {
            openAppSettings();
        }

        finish();
    }

    // Helper to toggle between Vibrate and Ring
    private void toggleRingerMode() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        
        try {
            int currentMode = audioManager.getRingerMode();

            if (currentMode == AudioManager.RINGER_MODE_NORMAL) {
                // Switch to Vibrate
                audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                Toast.makeText(this, "📳 Vibrate Mode", Toast.LENGTH_SHORT).show();
            } else {
                // Switch to Normal (Ring)
                // This handles Vibrate -> Normal AND Silent -> Normal
                audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                Toast.makeText(this, "🔔 Ringer Mode", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            // Fallback for Do Not Disturb permission issues
            Toast.makeText(this, "Check DND Permissions", Toast.LENGTH_LONG).show();
        }
    }

    // Robust method to find the App Settings page dynamically
    private void openAppSettings() {
        Intent intent = new Intent("android.intent.action.APPLICATION_PREFERENCES");
        intent.setPackage(getPackageName());
        
        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        if (activities != null && !activities.isEmpty()) {
            ResolveInfo info = activities.get(0);
            Intent launchIntent = new Intent();
            launchIntent.setClassName(info.activityInfo.packageName, info.activityInfo.name);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(launchIntent);
        } else {
            try {
                // Absolute fallback: App Info page
                Intent appInfoIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                appInfoIntent.setData(android.net.Uri.parse("package:" + getPackageName()));
                appInfoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(appInfoIntent);
            } catch (Exception e) {
                Toast.makeText(this, "Error: Could not open App Settings", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
