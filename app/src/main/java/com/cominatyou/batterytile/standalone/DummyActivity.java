package com.cominatyou.batterytile.standalone;

import android.app.Activity;
import android.os.Bundle;

// This activity exists solely to trick Android into collapsing the notification shade.
// It starts transparently and dies immediately. RIP.
public class DummyActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish(); // Die instantly
    }
}
