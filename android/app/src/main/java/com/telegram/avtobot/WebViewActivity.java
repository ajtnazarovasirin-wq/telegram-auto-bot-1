package com.telegram.avtobot;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import org.json.JSONObject;

public class WebViewActivity extends Activity {
    private static final String REPO = "ajtnazarovasirin-wq/telegram-auto-bot-1";
    // Public identifier of a GitHub OAuth App with Device Flow enabled. Replace only this public value.
    private static final String GITHUB_CLIENT_ID = "REPLACE_WITH_GITHUB_OAUTH_CLIENT_ID";
    private final Handler handler = new Handler();
    private WebView web;
    private SecureStore secure;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        secure = new SecureStore(this);
        web = new WebView(this);
        WebSettings s = web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true); s.setAllowContentAccess(false);
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(), "Android");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    private void js(String code) { runOnUiThread(() -> web.evaluateJavascript(code, null)); }
    private String quote(String text) { return JSONObject.quote(text == null ? "" : text); }

    private final class Bridge {
        @JavascriptInterface public String getRepo() { return REPO; }
        @JavascriptInterface public String getUser() {
            String token = secure.getGithubToken();
            if (token.isEmpty()) return "";
            try { return GitHubClient.user(token).optString("login", ""); }
            catch (Exception e) { return ""; }
        }
        @JavascriptInterface public void loginGithub() {
            if (GITHUB_CLIENT_ID.startsWith("REPLACE")) {
                js("window.appError('В APK ещё не задан публичный GitHub Client ID. Его нужно один раз вставить в WebViewActivity.java.');"); return;
            }
            new Thread(() -> {
                try {
                    JSONObject code = GitHubClient.requestDeviceCode(GITHUB_CLIENT_ID);
                    String device = code.getString("device_code");
                    String url = code.optString("verification_uri", "https://github.com/login/device");
                    String userCode = code.getString("user_code");
                    js("window.showDeviceCode(" + quote(userCode) + "," + quote(url) + ");");
                    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) {}
                    int interval = Math.max(5, code.optInt("interval", 5));
                    long expires = System.currentTimeMillis() + code.optLong("expires_in", 900) * 1000L;
                    while (System.currentTimeMillis() < expires) {
                        Thread.sleep(interval * 1000L);
                        String token = GitHubClient.pollDeviceToken(GITHUB_CLIENT_ID, device);
                        if (token != null) {
                            secure.putGithubToken(token);
                            String user = GitHubClient.user(token).optString("login", "GitHub");
                            js("window.githubConnected(" + quote(user) + ");"); return;
                        }
                    }
                    js("window.appError('Код входа истёк. Нажми «Войти через GitHub» ещё раз.');");
                } catch (Exception e) { js("window.appError(" + quote(e.getMessage()) + ");"); }
            }).start();
        }
        @JavascriptInterface public void logoutGithub() { secure.clear(); js("window.githubConnected('');"); }
        @JavascriptInterface public void saveSettings(final String raw) {
            new Thread(() -> {
                try {
                    JSONObject input = new JSONObject(raw);
                    String token = secure.getGithubToken();
                    if (token.isEmpty()) { js("window.appError('Сначала войди через GitHub.');"); return; }
                    String config = input.toString(2);
                    GitHubClient.updateConfig(REPO, token, config);
                    getSharedPreferences("settings", MODE_PRIVATE).edit().putString("last_config", config).apply();
                    js("window.saveDone();");
                } catch (Exception e) { js("window.appError(" + quote(e.getMessage()) + ");"); }
            }).start();
        }
        @JavascriptInterface public void runNow() {
            new Thread(() -> { try {
                String token = secure.getGithubToken();
                if (token.isEmpty()) { js("window.appError('Сначала войди через GitHub.');"); return; }
                GitHubClient.runNow(REPO, token); js("window.runDone();");
            } catch (Exception e) { js("window.appError(" + quote(e.getMessage()) + ");"); }}).start();
        }
        @JavascriptInterface public String localConfig() { return getSharedPreferences("settings", MODE_PRIVATE).getString("last_config", ""); }
    }
}
