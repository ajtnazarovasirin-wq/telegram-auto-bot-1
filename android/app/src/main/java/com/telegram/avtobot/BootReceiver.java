package com.telegram.avtobot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        if (new AppPrefs(c).getBoolean("enabled", false)) AlarmReceiver.schedule(c.getApplicationContext());
    }
}
