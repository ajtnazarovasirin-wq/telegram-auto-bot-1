package com.telegram.avtobot;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlarmReceiver extends BroadcastReceiver {
    private static final int ID = 77;
    public static void schedule(Context c) {
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        Intent i=new Intent(c,AlarmReceiver.class); PendingIntent pi=PendingIntent.getBroadcast(c,ID,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Calendar x=Calendar.getInstance(); x.set(Calendar.HOUR_OF_DAY,new AppPrefs(c).sendHour()); x.set(Calendar.MINUTE,new AppPrefs(c).sendMinute()); x.set(Calendar.SECOND,0); x.set(Calendar.MILLISECOND,0); if(x.getTimeInMillis()<=System.currentTimeMillis()) x.add(Calendar.DAY_OF_YEAR,1);
        if(Build.VERSION.SDK_INT>=31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,x.getTimeInMillis(),pi); else if(Build.VERSION.SDK_INT>=23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,x.getTimeInMillis(),pi); else am.setExact(AlarmManager.RTC_WAKEUP,x.getTimeInMillis(),pi);
    }
    public static void cancel(Context c){ AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE); Intent i=new Intent(c,AlarmReceiver.class); PendingIntent pi=PendingIntent.getBroadcast(c,ID,i,PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE); if(pi!=null) am.cancel(pi); }
    @Override public void onReceive(Context c,Intent i){ final Context app=c.getApplicationContext(); schedule(app); if(!new AppPrefs(app).getBoolean("enabled",false)) return; final PowerManager.WakeLock wl=((PowerManager)app.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"AutoBot:send"); wl.acquire(3*60*1000L); Executors.newSingleThreadExecutor().execute(()->{ try { Sender.run(app); } finally { wl.release(); }}); }
}
