package com.telegram.avtobot;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import android.util.Base64;

public final class GitHubClient {
    private GitHubClient() {}
    private static String request(String method, String url, String token, String body) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod(method); c.setConnectTimeout(20000); c.setReadTimeout(30000);
        c.setRequestProperty("Accept", "application/vnd.github+json");
        c.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        if (token != null && !token.isEmpty()) c.setRequestProperty("Authorization", "Bearer " + token);
        if (body != null) {
            c.setDoOutput(true); c.setRequestProperty("Content-Type", "application/json");
            try (OutputStream o = c.getOutputStream()) { o.write(body.getBytes(StandardCharsets.UTF_8)); }
        }
        int code = c.getResponseCode();
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        StringBuilder s = new StringBuilder();
        if (in != null) try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String x; while ((x = r.readLine()) != null) s.append(x);
        }
        if (code >= 400) throw new Exception("GitHub " + code + ": " + s);
        return s.toString();
    }

    /** GitHub OAuth Device Flow. The client id is public; no client secret is embedded in the APK. */
    public static JSONObject requestDeviceCode(String clientId) throws Exception {
        return new JSONObject(request("POST", "https://github.com/login/device/code", "",
                new JSONObject().put("client_id", clientId).put("scope", "repo workflow").toString()));
    }

    /** Returns access_token when approved, null when still pending. */
    public static String pollDeviceToken(String clientId, String deviceCode) throws Exception {
        JSONObject result = new JSONObject(request("POST", "https://github.com/login/oauth/access_token", "",
                new JSONObject().put("client_id", clientId).put("device_code", deviceCode)
                        .put("grant_type", "urn:ietf:params:oauth:grant-type:device_code").toString()));
        if (result.has("access_token")) return result.getString("access_token");
        String error = result.optString("error", "authorization_pending");
        if ("authorization_pending".equals(error) || "slow_down".equals(error)) return null;
        throw new Exception("GitHub login: " + error);
    }

    public static JSONObject user(String token) throws Exception {
        return new JSONObject(request("GET", "https://api.github.com/user", token, null));
    }

    public static void updateConfig(String repo, String token, String config) throws Exception {
        String u = "https://api.github.com/repos/" + repo + "/contents/config.json";
        JSONObject current = new JSONObject(request("GET", u, token, null));
        JSONObject body = new JSONObject().put("message", "Update bot settings from Android app")
                .put("content", Base64.encodeToString(config.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP));
        String sha = current.optString("sha", ""); if (!sha.isEmpty()) body.put("sha", sha);
        request("PUT", u, token, body.toString());
    }

    public static void runNow(String repo, String token) throws Exception {
        String u = "https://api.github.com/repos/" + repo + "/actions/workflows/telegram-bot.yml/dispatches";
        request("POST", u, token, new JSONObject().put("ref", "main")
                .put("inputs", new JSONObject().put("send_now", "true")).toString());
    }
}
