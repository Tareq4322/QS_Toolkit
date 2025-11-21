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
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_volume));
        tile.updateTile();
    }

    @Override
    public void onClick() {
        // 1. Trigger the Volume Slider UI
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
        
        // 2. The "Ghost Activity" Trick to collapse the panel
        // This is the only reliable way to close the shade on Android 12+
        Intent intent = new Intent(this, DummyActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        // This method automatically collapses the shade when starting the intent
        startActivityAndCollapse(intent);
    }
}
