package com.cominatyou.batterytile.standalone;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import java.util.Locale;

public class InternetSpeedTileService extends TileService {

    private final Handler handler = new Handler(Looper.getMainLooper());
    
    // Track Download (Rx) only
    private long lastRxBytes = 0;

    // The "Game Loop"
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            updateSpeed();
            // Refresh every 1 second (1000ms)
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onStartListening() {
        Tile tile = getQsTile();
        if (tile == null) return;

        tile.setLabel("Download Speed");
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_speed));
        tile.setState(Tile.STATE_ACTIVE);
        tile.updateTile();

        // Initialize baseline so we don't get a massive spike on first update
        lastRxBytes = TrafficStats.getTotalRxBytes();
        
        // Start the loop
        handler.removeCallbacks(updateRunnable);
        handler.post(updateRunnable);
    }

    @Override
    public void onStopListening() {
        // Stop the loop immediately when panel closes to save battery
        handler.removeCallbacks(updateRunnable);
    }

    private void updateSpeed() {
        Tile tile = getQsTile();
        if (tile == null) return;

        // 1. Get current download bytes
        long currentRxBytes = TrafficStats.getTotalRxBytes();
        
        // 2. Calculate difference since last check (1 second ago)
        long deltaRx = currentRxBytes - lastRxBytes;

        // Update baseline for next tick
        lastRxBytes = currentRxBytes;

        // 3. Protect against negative numbers (reboot/overflow)
        if (deltaRx < 0) deltaRx = 0;

        // 4. Convert to Mbps (Megabits per second)
        // Formula: (Bytes * 8) / 1,000,000
        float rxMbps = (deltaRx * 8f) / 1_000_000f;

        String iconText;

        // Format logic: No decimals for high speeds to save space
        if (rxMbps >= 100) {
            iconText = String.format(Locale.US, "%.0f", rxMbps);
        } else {
            iconText = String.format(Locale.US, "%.1f", rxMbps);
        }

        // 5. Update the tile
        // We removed the arrow so the number can be BIGGER
        tile.setIcon(createDynamicIcon(iconText));
        tile.setSubtitle(String.format(Locale.US, "%.1f Mbps", rxMbps));
        tile.updateTile();
    }

    // Helper to draw text on the icon
    private Icon createDynamicIcon(String text) {
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        // Condensed font makes numbers look taller and cooler
        paint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);

        // Start large (we can go bigger now without the arrows)
        float textSize = 85f;
        paint.setTextSize(textSize);
        final float maxWidth = 98f;

        // Shrink to fit
        while (paint.measureText(text) > maxWidth) {
            textSize -= 1f;
            paint.setTextSize(textSize);
        }

        // Center vertically
        float yPos = (canvas.getHeight() / 2f) - ((paint.descent() + paint.ascent()) / 2f);
        canvas.drawText(text, canvas.getWidth() / 2f, yPos, paint);

        return Icon.createWithBitmap(bitmap);
    }
    
    @Override
    public void onClick() {
        // Refresh instantly
        updateSpeed();
    }
}
