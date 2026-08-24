package com.telegram.avtobot;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppPrefs {
    private static final String FILE = "settings";
    private final SharedPreferences p;
    public AppPrefs(Context c) { p = c.getSharedPreferences(FILE, Context.MODE_PRIVATE); }
    public String get(String k, String d) { return p.getString(k, d); }
    public boolean getBoolean(String k, boolean d) { return p.getBoolean(k, d); }
    public int getInt(String k, int d) { return p.getInt(k, d); }
    public void put(String k, String v) { p.edit().putString(k, v).apply(); }
    public void putBoolean(String k, boolean v) { p.edit().putBoolean(k, v).apply(); }
    public void putInt(String k, int v) { p.edit().putInt(k, v).apply(); }
    public String token() { return get("telegram_token", ""); }
    public String provider() { return get("provider", "Groq"); }
    public String key() { return get(provider().equals("Gemini") ? "gemini_key" : provider().equals("GPT/OpenAI") ? "openai_key" : "groq_key", ""); }
    public String model() { return get(provider().equals("Gemini") ? "gemini_model" : provider().equals("GPT/OpenAI") ? "openai_model" : "groq_model", provider().equals("Gemini") ? "gemini-3.6-flash" : provider().equals("GPT/OpenAI") ? "gpt-5-mini" : "openai/gpt-oss-120b"); }
    public String topic() { return get("topic", "Полезная ежедневная мысль"); }
    public String instruction() { return get("instruction", "Создай новый короткий текст без повторов, дружелюбно и естественно."); }
    public String language() { return get("language", "русский"); }
    public int maxLength() { return getInt("max_length", 500); }
    public int sendHour() { return getInt("send_hour", 9); }
    public int sendMinute() { return getInt("send_minute", 0); }
    public int checkDelay() { return getInt("check_delay", 5); }
    public int retries() { return getInt("retries", 2); }
    public boolean allChats() { return getBoolean("all_chats", false); }
}
