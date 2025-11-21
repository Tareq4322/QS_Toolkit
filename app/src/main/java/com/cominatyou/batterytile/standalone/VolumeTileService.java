package com.cominatyou.batterytile.standalone;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class VolumeTileService extends TileService {

    @Override
    public void onStartListening() {
        Tile tile = getQsTile();
        if (tile == null) return;
        
        tile.setLabel("Volume");
        tile.setState(Tile.STATE_ACTIVE);
        
        // UPDATED: Now using the proper volume icon
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_volume));
        
        tile.updateTile();
    }

    @Override
    public void onClick() {
        // 1. Get the Audio Manager
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        
        // 2. The "Magic Trick": 
        // ADJUST_SAME = Don't actually change the volume level
        // FLAG_SHOW_UI = Force the volume slider to appear
        audioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
        
        // 3. Important: Close the Quick Settings panel so you can actually SEE the volume slider
        // (The slider usually appears at the side of the screen, covered by the panel)
        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }
}
