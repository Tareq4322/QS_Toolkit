package com.cominatyou.batterytile.standalone;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

public class KeepAwakeService extends Service {

    public static boolean isRunning = false;
    private PowerManager.WakeLock wakeLock;
    
    // Use our custom receiver class
    private ScreenOffReceiver screenOffReceiver;

    private static final String CHANNEL_ID = "keep_awake_channel";
    
    public static final String ACTION_START_DIM = "com.cominatyou.batterytile.START_DIM";
    public static final String ACTION_START_BRIGHT = "com.cominatyou.batterytile.START_BRIGHT";
    public static final String ACTION_STOP = "com.cominatyou.batterytile.STOP";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        
        // Initialize the custom receiver
        // When screen turns off -> run "stopSelf()"
        screenOffReceiver = new ScreenOffReceiver(this::stopSelf);
        
        registerReceiver(screenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;

        if (ACTION_STOP.equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        // Default to Bright if no specific action or intent is null
        boolean allowDim = ACTION_START_DIM.equals(action);

        acquireWakeLock(allowDim);

        Notification notification = createNotification(allowDim);
        
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(1, notification);
        }

        isRunning = true;
        CaffeineTileService.requestUpdate(this);
        
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
        
        if (screenOffReceiver != null) {
            try {
                unregisterReceiver(screenOffReceiver);
            } catch (Exception e) {
                // Already unregistered
            }
        }

        isRunning = false;
        CaffeineTileService.requestUpdate(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void acquireWakeLock(boolean allowDim) {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        // SCREEN_DIM_WAKE_LOCK is deprecated but required for this specific feature
        int levelAndFlags = allowDim ? PowerManager.SCREEN_DIM_WAKE_LOCK : PowerManager.SCREEN_BRIGHT_WAKE_LOCK;
        
        wakeLock = powerManager.newWakeLock(levelAndFlags, "QSToolkit:KeepAwake");
        wakeLock.acquire();
    }

    private Notification createNotification(boolean allowDim) {
        Intent stopIntent = new Intent(this, KeepAwakeService.class);
        stopIntent.setAction(ACTION_STOP);
        
        PendingIntent stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Action stopAction = new Notification.Action.Builder(
            R.drawable.ic_stop, 
            "Stop", 
            stopPendingIntent
        ).build();

        String contentText = allowDim ? "Keeping screen on (Dim)" : "Keeping screen on (Bright)";

        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Caffeine is Active")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_coffee)
                .setOngoing(true)
                .addAction(stopAction);
                
        if (Build.VERSION.SDK_INT >= 31) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        }
        
        return builder.build();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Caffeine Service",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Notifications for the Keep Awake service");
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
