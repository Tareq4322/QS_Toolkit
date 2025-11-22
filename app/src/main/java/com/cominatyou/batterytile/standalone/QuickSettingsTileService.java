package com.cominatyou.batterytile.standalone;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.cominatyou.batterytile.standalone.preferences.TileTextFormatter;

import java.time.Duration;
import java.util.Locale;

public class QuickSettingsTileService extends TileService {
    private boolean isTappableTileEnabled = false;
    private boolean shouldEmulatePowerSaveTile = false;
    private boolean isCharging = false;

    // Animation Logic
    private final Handler toggleHandler = new Handler(Looper.getMainLooper());
    private boolean showWattage = false; 
    private Intent lastBatteryIntent = null; 

    // The heartbeat of the tile: Flips every 1 second
    private final Runnable toggleRunnable = new Runnable() {
        @Override
        public void run() {
            showWattage = !showWattage; 
            if (lastBatteryIntent != null) {
                setBatteryInfo(lastBatteryIntent); 
            }
            // 1000ms = 1 second delay. Snappy.
            toggleHandler.postDelayed(this, 1000);
        }
    };

    /**
     * Draws text onto the icon.
     * Uses Condensed font to maximize size.
     * Now accepts a separate text string (textToSizeBy) to fix the font size based on the wider string.
     */
    private Icon createDynamicIcon(String textToDraw, String textToSizeBy) {
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        
        // USE CONDENSED FONT: Allows taller text that fits in the same width
        paint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        
        paint.setTextAlign(Paint.Align.CENTER);

        // Start even larger now that we are condensed
        float textSize = 80f;
        paint.setTextSize(textSize);
        
        // We allow it to go up to 98% width (1px padding on sides)
        final float maxWidth = 98f;

        // CRITICAL FIX: Base sizing on the reference text (which is always the widest one, the temperature)
        while (paint.measureText(textToSizeBy) > maxWidth) {
            textSize -= 1f;
            paint.setTextSize(textSize);
        }

        float yPos = (canvas.getHeight() / 2f) - ((paint.descent() + paint.ascent()) / 2f);
        canvas.drawText(textToDraw, canvas.getWidth() / 2f, yPos, paint);

        return Icon.createWithBitmap(bitmap);
    }

    private void setActiveLabelText(String text) {
        if (getSharedPreferences("preferences", Context.MODE_PRIVATE).getBoolean("infoInTitle", false)) {
            getQsTile().setLabel(text);
            getQsTile().setSubtitle(getString(R.string.battery_tile_label));
        } else {
            getQsTile().setSubtitle(text);
        }
    }

    private void setBatteryInfo(Intent intent) {
        lastBatteryIntent = intent; // Keep this cached for the timer

        final int batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        final int plugState = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        final int batteryState = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

        final boolean isPluggedIn = plugState == BatteryManager.BATTERY_PLUGGED_AC || plugState == BatteryManager.BATTERY_PLUGGED_USB || plugState == BatteryManager.BATTERY_PLUGGED_WIRELESS;
        final boolean isFullyCharged = isPluggedIn && batteryState == BatteryManager.BATTERY_STATUS_FULL;
        isCharging = batteryState == BatteryManager.BATTERY_STATUS_CHARGING;

        // --- ICON TEXT PRE-CALCULATION FOR SIZING ---
        
        // 1. Calculate Temperature text (this is the widest string, used for size reference)
        final float tempFloat = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0f;
        final String tempText = String.format(Locale.US, "%.1f°", tempFloat);
        
        // 2. Calculate Wattage text
        String wattageText = "";
        if (isCharging) {
            BatteryManager manager = (BatteryManager) getSystemService(BATTERY_SERVICE);
            int currentMicroAmps = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
            int voltageMilliVolts = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
            
            double currentAmps = Math.abs(currentMicroAmps) / 1000000.0;
            double voltageVolts = voltageMilliVolts / 1000.0;
            double wattage = currentAmps * voltageVolts;
            wattageText = String.format(Locale.US, "%.1fW", wattage);
        }

        // --- ICON DRAWING LOGIC ---
        Icon iconToDraw;
        String iconText;
        String sizeReferenceText = tempText; // Always size based on temperature

        if (isCharging && showWattage) {
            iconText = wattageText;
            iconToDraw = createDynamicIcon(iconText, sizeReferenceText);
        } else {
            iconText = tempText;
            iconToDraw = createDynamicIcon(iconText, sizeReferenceText);
        }
        // -----------------------

        if (isTappableTileEnabled) {
            getQsTile().setState(isCharging ? Tile.STATE_INACTIVE : (getSystemService(PowerManager.class).isPowerSaveMode() ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE));
        }

        if (getSharedPreferences("preferences", MODE_PRIVATE).getBoolean("percentage_as_icon", false)) {
            getQsTile().setIcon(iconToDraw);
        } 
        else if (isPluggedIn && getSharedPreferences("preferences", MODE_PRIVATE).getBoolean("dynamic_tile_icon", true)) {
             switch (plugState) {
                case BatteryManager.BATTERY_PLUGGED_AC -> getQsTile().setIcon(Icon.createWithResource(this, R.drawable.ic_power));
                case BatteryManager.BATTERY_PLUGGED_USB -> getQsTile().setIcon(Icon.createWithResource(this, R.drawable.ic_usb));
                case BatteryManager.BATTERY_PLUGGED_WIRELESS -> getQsTile().setIcon(Icon.createWithResource(this, R.drawable.ic_dock));
                default -> getQsTile().setIcon(Icon.createWithResource(this, R.drawable.ic_qs_battery));
            }
        }

        if (isFullyCharged) {
            final String customTileText = getSharedPreferences("preferences", MODE_PRIVATE).getString("charging_text", "");
            setActiveLabelText(customTileText.isEmpty() ? getString(R.string.fully_charged) : new TileTextFormatter(this).format(customTileText));
            if (!isTappableTileEnabled) getQsTile().setState(getTileState(true));
        } else if (isCharging) {
            final String customTileText = getSharedPreferences("preferences", MODE_PRIVATE).getString("charging_text", "");
            if (!isTappableTileEnabled) getQsTile().setState(getTileState(true));

            if (!customTileText.isEmpty()) {
                setActiveLabelText(new TileTextFormatter(this).format(customTileText));
            } else {
                final long remainingTime = getSystemService(BatteryManager.class).computeChargeTimeRemaining();
                if (remainingTime < 1) {
                    setActiveLabelText(getString(R.string.charging_no_time_estimate, batteryLevel));
                } else if (remainingTime <= 60000) {
                    setActiveLabelText(getString(R.string.charging_less_than_one_hour_left, batteryLevel, 1));
                } else {
                    Duration duration = Duration.ofMillis(remainingTime);
                    final long hours = duration.toHours();
                    final long minutes = duration.minusHours(hours).toMinutes();

                    if (hours > 0) {
                        setActiveLabelText(getString(R.string.charging_more_than_one_hour_left, batteryLevel, hours, minutes));
                    } else {
                        setActiveLabelText(getString(R.string.charging_less_than_one_hour_left, batteryLevel, minutes));
                    }
                }
            }
        } else {
            final String customTileText = getSharedPreferences("preferences", MODE_PRIVATE).getString("discharging_text", "");
            setActiveLabelText(customTileText.isEmpty() ? batteryLevel + "%" : new TileTextFormatter(this).format(customTileText));
            if (!isTappableTileEnabled) getQsTile().setState(getTileState(false));

            if (getSharedPreferences("preferences", MODE_PRIVATE).getBoolean("percentage_as_icon", false)) {
                getQsTile().setIcon(iconToDraw);
            } else {
                getQsTile().setIcon(Icon.createWithResource(this, R.drawable.ic_qs_battery));
            }
        }

        getQsTile().updateTile();
    }

    private void setPowerSaveInfo() {
        final boolean shouldEmulatePowerSaveTile = getSharedPreferences("preferences", MODE_PRIVATE).getBoolean("emulatePowerSaveTile", false);

        if (getSystemService(PowerManager.class).isPowerSaveMode()) {
            getQsTile().setState(Tile.STATE_ACTIVE);
            if (shouldEmulatePowerSaveTile)
                getQsTile().setSubtitle(getString(R.string.power_saver_tile_on_subtitle));
        } else {
            getQsTile().setState(Tile.STATE_INACTIVE);
            if (shouldEmulatePowerSaveTile)
                getQsTile().setSubtitle(getString(R.string.power_saver_tile_off_subtitle));
        }

        getQsTile().updateTile();
    }

    BroadcastReceiver batteryStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setBatteryInfo(intent);
        }
    };

    BroadcastReceiver powerSaveModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setPowerSaveInfo();
        }
    };

    @Override
    public void onStartListening() {
        shouldEmulatePowerSaveTile = getSharedPreferences("preferences", MODE_PRIVATE).getBoolean("emulatePowerSaveTile", false);
        isTappableTileEnabled = getSharedPreferences("preferences", MODE_PRIVATE).getBoolean("tappableTileEnabled", false);

        final Intent batteryChangedIntent = registerReceiver(batteryStateReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        
        if (batteryChangedIntent != null) {
            final int status = batteryChangedIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
            
            // Kill any old timers first so we don't get double speed
            toggleHandler.removeCallbacks(toggleRunnable);
            if (isCharging) {
                toggleHandler.post(toggleRunnable);
            } else {
                showWattage = false; 
            }
        }

        if (shouldEmulatePowerSaveTile) {
            unregisterReceiver(batteryStateReceiver);
            registerReceiver(powerSaveModeReceiver, new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED));
            getQsTile().setLabel(getString(R.string.power_save_tile_label));
            getQsTile().setIcon(Icon.createWithResource(this, R.drawable.ic_battery_saver));

            if (isCharging) {
                getQsTile().setState(Tile.STATE_UNAVAILABLE);
                getQsTile().setSubtitle(getString(R.string.power_save_tile_unavailable_subtitle));
                getQsTile().updateTile();
            } else {
                setPowerSaveInfo();
            }
        } else {
            getQsTile().setIcon(Icon.createWithResource(this, R.drawable.ic_qs_battery));
            getQsTile().setLabel(getString(R.string.battery_tile_label));

            if (!isTappableTileEnabled) {
                getQsTile().setState(getTileState(isCharging));
            } else {
                final IntentFilter powerSaveChangedFilter = new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
                registerReceiver(powerSaveModeReceiver, powerSaveChangedFilter);
                setPowerSaveInfo();
            }

            if (batteryChangedIntent != null) {
                setBatteryInfo(batteryChangedIntent);
            }
        }
    }

    private int getTileState(boolean isCharging) {
        return switch (getSharedPreferences("preferences", MODE_PRIVATE).getInt("tileState", 0)) {
            case 0 -> Tile.STATE_ACTIVE;
            case 1 -> isCharging ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
            default -> Tile.STATE_INACTIVE;
        };
    }

    @Override
    public void onClick() {
        super.onClick();
        if (!isTappableTileEnabled || isCharging) return;

        final boolean isInPowerSaveMode = getSystemService(PowerManager.class).isPowerSaveMode();

        Settings.Global.putInt(getContentResolver(), "low_power", isInPowerSaveMode ? 0 : 1);
        getQsTile().setState(isInPowerSaveMode ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        getQsTile().updateTile();
    }

    @Override
    public void onStopListening() {
        // Save battery: Kill the loop when the drawer closes
        toggleHandler.removeCallbacks(toggleRunnable);

        if (isTappableTileEnabled) {
            unregisterReceiver(powerSaveModeReceiver);
        }
        if (!shouldEmulatePowerSaveTile) {
            unregisterReceiver(batteryStateReceiver);
        }
    }
}
