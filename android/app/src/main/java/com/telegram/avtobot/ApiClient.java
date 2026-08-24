package com.telegram.avtobot;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class ApiClient {
    private ApiClient() {}
    private static String request(String method, String url, String body, String auth) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod(method); c.setConnectTimeout(20000); c.setReadTimeout(30000);
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        if (auth != null && !auth.isEmpty()) c.setRequestProperty("Authorization", "Bearer " + auth);
        if (body != null) { c.setDoOutput(true); OutputStream out = c.getOutputStream(); out.write(body.getBytes(StandardCharsets.UTF_8)); out.close(); }
        InputStream in = c.getResponseCode() >= 400 ? c.getErrorStream() : c.getInputStream();
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder s = new StringBuilder(); String line; while ((line = r.readLine()) != null) s.append(line); r.close();
        if (c.getResponseCode() >= 400) throw new Exception(s.toString());
        return s.toString();
    }
    public static String telegram(String token, String method, JSONObject params) throws Exception {
        return request("POST", "https://api.telegram.org/bot" + token + "/" + method, params.toString(), null);
    }
    public static String telegramGetUpdates(String token) throws Exception {
        return telegram(token, "getUpdates", new JSONObject().put("limit", 100).put("timeout", 0));
    }
    public static String telegramSend(String token, String chatId, String text) throws Exception {
        return telegram(token, "sendMessage", new JSONObject().put("chat_id", chatId).put("text", text));
    }
    public static JSONArray listModels(String provider, String key) throws Exception { String u; if("Gemini".equals(provider)) u="https://generativelanguage.googleapis.com/v1beta/models?key="+URLEncoder.encode(key,"UTF-8"); else u=("Groq".equals(provider)?"https://api.groq.com/openai/v1/models":"https://api.openai.com/v1/models"); JSONObject out=new JSONObject(request("GET",u,null,"Gemini".equals(provider)?null:key)); JSONArray data=out.optJSONArray("models"); if(data==null) data=out.optJSONArray("data"); JSONArray ids=new JSONArray(); if(data!=null) for(int i=0;i<data.length();i++){ JSONObject m=data.getJSONObject(i); String id=m.optString("id",m.optString("name","")); if(id.startsWith("models/")) id=id.substring(7); if(!id.isEmpty()) ids.put(id); } return ids; }
    public static String generate(String provider, String key, String model, String prompt, int max) throws Exception {
        if ("Gemini".equals(provider)) {
            String u = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + URLEncoder.encode(key, "UTF-8");
            JSONObject part = new JSONObject().put("text", prompt);
            JSONObject body = new JSONObject().put("contents", new JSONArray().put(new JSONObject().put("parts", new JSONArray().put(part))));
            JSONObject out = new JSONObject(request("POST", u, body.toString(), null));
            return out.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").trim();
        }
        String base = "Groq".equals(provider) ? "https://api.groq.com/openai/v1/chat/completions" : "https://api.openai.com/v1/chat/completions";
        JSONObject message = new JSONObject().put("role", "user").put("content", prompt);
        JSONObject body = new JSONObject().put("model", model).put("messages", new JSONArray().put(message)).put("max_tokens", max);
        JSONObject out = new JSONObject(request("POST", base, body.toString(), key));
        return out.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim();
    }
}
