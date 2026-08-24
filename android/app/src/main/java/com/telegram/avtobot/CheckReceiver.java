package com.telegram.avtobot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.os.Build;
import android.app.Notification;

public class CheckReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        String text=c.getSharedPreferences("chats",0).getString("last_text", "");
        c.getSharedPreferences("history",0).edit().putLong("checked_at",System.currentTimeMillis()).putString("status",text.isEmpty()?"Нет результата":"Telegram подтвердил запрос при отправке").apply();
        if(Build.VERSION.SDK_INT>=26){ NotificationManager n=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE); n.createNotificationChannel(new NotificationChannel("status","Статус отправки",NotificationManager.IMPORTANCE_DEFAULT)); }
        if(Build.VERSION.SDK_INT>=33 && c.checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=0) return;
        NotificationManager n=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE); Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,"status"):new Notification.Builder(c); b.setSmallIcon(com.telegram.avtobot.R.mipmap.ic_launcher).setContentTitle("Бот по расписанию").setContentText(text.isEmpty()?"Проверка: данных нет":"Проверка: запрос Telegram принят").setAutoCancel(true); n.notify(22,b.build());
    }
}
