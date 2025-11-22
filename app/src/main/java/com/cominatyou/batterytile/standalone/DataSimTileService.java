package com.cominatyou.batterytile.standalone;

import android.content.Intent;
import android.graphics.drawable.Icon;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class DataSimTileService extends TileService {

    @Override
    public void onStartListening() {
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;

        tile.setLabel("Data SIM");
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_sim_dashboard));

        // Use the global setting value to READ the current data SIM
        // NOTE: This setting is usually the index (e.g., 1 for SIM 1, 2 for SIM 2).
        try {
            // Default to 1 if setting not found.
            int currentSub = Settings.Global.getInt(getContentResolver(), "multi_sim_data_call", 1);
            
            if (currentSub == 1) {
                tile.setSubtitle("SIM 1 Active");
                tile.setState(Tile.STATE_ACTIVE);
            } else if (currentSub == 2) {
                tile.setSubtitle("SIM 2 Active");
                tile.setState(Tile.STATE_ACTIVE);
            } else {
                // If it's 0 or something else, usually means no SIM selected or error
                tile.setSubtitle("No SIM Data");
                tile.setState(Tile.STATE_INACTIVE);
            }
        } catch (Exception e) {
            tile.setSubtitle("Tap to Manage");
            tile.setState(Tile.STATE_INACTIVE);
        }

        tile.updateTile();
    }

    @Override
    public void onClick() {
        // Launch directly to the SIM / Mobile Network settings
        // ACTION_NETWORK_OPERATOR_SETTINGS usually lands on the right page to switch SIM
        Intent intent = new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivityAndCollapse(intent);
    }
}
