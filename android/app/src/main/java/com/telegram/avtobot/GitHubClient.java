package com.telegram.avtobot;

import org.json.*; import java.io.*; import java.net.*; import java.nio.charset.StandardCharsets; import android.util.Base64;

public final class GitHubClient {
 private GitHubClient(){}
 private static String request(String method,String url,String token,String body)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setRequestMethod(method);c.setConnectTimeout(20000);c.setReadTimeout(30000);c.setRequestProperty("Accept","application/vnd.github+json");c.setRequestProperty("Authorization","Bearer "+token);c.setRequestProperty("X-GitHub-Api-Version","2022-11-28");if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream o=c.getOutputStream()){o.write(body.getBytes(StandardCharsets.UTF_8));}}int code=c.getResponseCode();InputStream in=code>=400?c.getErrorStream():c.getInputStream();StringBuilder s=new StringBuilder();try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String x;while((x=r.readLine())!=null)s.append(x);}if(code>=400)throw new Exception("GitHub "+code+": "+s);return s.toString();}
 public static void updateConfig(String repo,String token,String config)throws Exception{String u="https://api.github.com/repos/"+repo+"/contents/config.json";JSONObject current=new JSONObject(request("GET",u,token,null));String sha=current.optString("sha","");JSONObject body=new JSONObject().put("message","Update settings from Android app").put("content",Base64.encodeToString(config.getBytes(StandardCharsets.UTF_8),Base64.NO_WRAP));if(!sha.isEmpty())body.put("sha",sha);request("PUT",u,token,body.toString());}
 public static void runNow(String repo,String token)throws Exception{String u="https://api.github.com/repos/"+repo+"/actions/workflows/telegram-bot.yml/dispatches";request("POST",u,token,new JSONObject().put("ref","main").put("inputs",new JSONObject().put("send_now","true")).toString());}
}
