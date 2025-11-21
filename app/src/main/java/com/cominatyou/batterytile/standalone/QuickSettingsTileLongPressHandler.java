package com.cominatyou.batterytile.standalone;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import com.cominatyou.batterytile.standalone.DnsTileService;
import com.cominatyou.batterytile.standalone.LockTileService;
import com.cominatyou.batterytile.standalone.QuickSettingsTileService;
import com.cominatyou.batterytile.standalone.VolumeTileService;

public class QuickSettingsTileLongPressHandler extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ComponentName componentName = getIntent().getParcelableExtra(Intent.EXTRA_COMPONENT_NAME);

        if (componentName == null) {
            launchAppSettings();
            finish();
            return;
        }

        String className = componentName.getClassName();
        Intent targetIntent = null;

        // --- ROUTING LOGIC ---

        // 1. Battery -> Battery Settings
        if (className.equals(QuickSettingsTileService.class.getName())) {
            targetIntent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
        }

        // 2. Volume -> Sound Settings
        else if (className.equals(VolumeTileService.class.getName())) {
            targetIntent = new Intent(Settings.ACTION_SOUND_SETTINGS);
        }

        // 3. DNS -> Network Settings
        else if (className.equals(DnsTileService.class.getName())) {
            targetIntent = new Intent("android.settings.NETWORK_AND_INTERNET_SETTINGS");
        }

        // 4. Lock Screen -> App Settings
        else if (className.equals(LockTileService.class.getName())) {
            launchAppSettings();
            finish();
            return;
        }

        // --- EXECUTE ---

        if (targetIntent != null) {
            try {
                targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(targetIntent);
            } catch (Exception e) {
                // Fallback for DNS/Network
                if (className.equals(DnsTileService.class.getName())) {
                    try {
                        Intent fallback = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                        fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(fallback);
                    } catch (Exception ex) {
                        launchAppSettings(); // Give up and show app settings
                    }
                } else {
                    // Fallback for others: Main Settings
                    try {
                        startActivity(new Intent(Settings.ACTION_SETTINGS));
                    } catch (Exception ex2) {
                        launchAppSettings();
                    }
                }
            }
        } else {
            launchAppSettings();
        }

        finish();
    }

    // --- THE FAIL-SAFE LAUNCHER ---
    private void launchAppSettings() {
        try {
            // Plan A: Try the standard "Open Preferences" action defined in Manifest
            Intent intent = new Intent("android.intent.action.APPLICATION_PREFERENCES");
            intent.setPackage(getPackageName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } catch (Exception e) {
            try {
                // Plan B: Try explicit Class Name
                Intent intent = new Intent();
                intent.setClassName(getPackageName(), "com.cominatyou.batterytile.preferences.PreferencesActivity");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } catch (Exception ex) {
                // Plan C: The "Nuclear Option" - Open System App Info Page
                // This ALWAYS works and lets you manage permissions/uninstall
                try {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    Toast.makeText(this, "Opening System App Info", Toast.LENGTH_SHORT).show();
                } catch (Exception finalEx) {
                    Toast.makeText(this, "Error: Could not open settings", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}package com.cominatyou.batterytile.standalone;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

// Only importing the services we need to check against
import com.cominatyou.batterytile.standalone.DnsTileService;
import com.cominatyou.batterytile.standalone.LockTileService;
import com.cominatyou.batterytile.standalone.QuickSettingsTileService;
import com.cominatyou.batterytile.standalone.VolumeTileService;

public class QuickSettingsTileLongPressHandler extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ComponentName componentName = getIntent().getParcelableExtra(Intent.EXTRA_COMPONENT_NAME);

        if (componentName == null) {
            launchAppSettings();
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

        // 2. Volume Tile -> System Sound Settings
        else if (className.equals(VolumeTileService.class.getName())) {
            targetIntent = new Intent(Settings.ACTION_SOUND_SETTINGS);
        }

        // 3. DNS Tile -> System Network Settings
        else if (className.equals(DnsTileService.class.getName())) {
            // Try the specific Network & Internet page first
            targetIntent = new Intent("android.settings.NETWORK_AND_INTERNET_SETTINGS");
        }

        // 4. Lock Screen Tile -> App Settings (Tile Toolkit)
        else if (className.equals(LockTileService.class.getName())) {
            launchAppSettings();
            finish();
            return;
        }

        // --- EXECUTE ---

        if (targetIntent != null) {
            try {
                targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(targetIntent);
            } catch (Exception e) {
                // ERROR HANDLER
                if (className.equals(DnsTileService.class.getName())) {
                    try {
                        // Fallback for DNS
                        Intent fallback = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                        fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(fallback);
                    } catch (Exception ex) {
                        Toast.makeText(this, "Could not find Network Settings", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Fallback for others
                    try {
                        startActivity(new Intent(Settings.ACTION_SETTINGS));
                    } catch (Exception ex2) {
                        launchAppSettings();
                    }
                }
            }
        } else {
            launchAppSettings();
        }

        finish();
    }

    // FIX: Explicitly point to the Preferences Activity by its full Java path.
    // This bypasses import errors and package name confusion.
    private void launchAppSettings() {
        try {
            Intent intent = new Intent();
            // This string MUST match the package declaration inside PreferencesActivity.java
            String settingsActivity = "com.cominatyou.batterytile.preferences.PreferencesActivity";
            
            intent.setClassName(this, settingsActivity);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } catch (Exception e) {
            // If this fails, it means the file PreferencesActivity.java was moved or renamed.
            Toast.makeText(this, "Error: Could not find PreferencesActivity", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}
