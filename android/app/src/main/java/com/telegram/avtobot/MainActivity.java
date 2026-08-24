package com.telegram.avtobot;

import android.app.*; import android.os.*; import android.content.*; import android.graphics.Color; import android.view.*; import android.widget.*;

public class MainActivity extends Activity {
 private static final String REPO="ajtnazarovasirin-wq/telegram-auto-bot-1";
 private TextView statusTitle,statusDetails;
 @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_main);statusTitle=findViewById(R.id.statusTitle);statusDetails=findViewById(R.id.statusDetails);findViewById(R.id.githubScreen).setOnClickListener(v->open(GitHubActivity.class));findViewById(R.id.telegramScreen).setOnClickListener(v->open(TelegramActivity.class));findViewById(R.id.aiScreen).setOnClickListener(v->open(AiActivity.class));findViewById(R.id.scheduleScreen).setOnClickListener(v->open(ScheduleActivity.class));findViewById(R.id.historyScreen).setOnClickListener(v->open(HistoryActivity.class));findViewById(R.id.sendNow).setOnClickListener(v->sendNow());updateStatus();}
 private void open(Class<?> c){startActivity(new Intent(this,c));}
 private void updateStatus(){boolean gh=!getSharedPreferences("settings",0).getString("github_token","").isEmpty();boolean tg=!getSharedPreferences("settings",0).getString("telegram_token","").isEmpty();boolean en=getSharedPreferences("settings",0).getBoolean("enabled",false);if(gh&&tg){statusTitle.setText("Статус: готово к работе");statusTitle.setTextColor(Color.rgb(19,121,91));statusDetails.setText(en?"Расписание включено":"Расписание выключено");}else{statusTitle.setText("Статус: нужна настройка");statusTitle.setTextColor(Color.rgb(181,71,8));statusDetails.setText("Открой разделы по порядку");}}
 private void sendNow(){String token=getSharedPreferences("settings",0).getString("github_token","");if(token.isEmpty()){Toast.makeText(this,"Сначала подключи GitHub",Toast.LENGTH_LONG).show();return;}new Thread(()->{try{GitHubClient.runNow(REPO,token);runOnUiThread(()->Toast.makeText(this,"Онлайн-запуск отправлен",Toast.LENGTH_LONG).show());}catch(Exception e){runOnUiThread(()->Toast.makeText(this,"Ошибка: "+e.getMessage(),Toast.LENGTH_LONG).show());}}).start();}
 @Override protected void onResume(){super.onResume();if(statusTitle!=null)updateStatus();}
}
