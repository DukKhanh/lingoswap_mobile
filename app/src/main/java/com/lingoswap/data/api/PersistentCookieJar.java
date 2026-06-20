package com.lingoswap.data.api;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * PersistentCookieJar — lưu cookie (đặc biệt là refreshToken httpOnly)
 * vào SharedPreferences để tồn tại qua các lần mở app.
 *
 * Backend LingoSwap set:  Set-Cookie: refreshToken=xxx; httpOnly; ...
 * Android cần lưu và gửi lại cookie này khi gọi POST /api/auth/refresh-token
 */
public class PersistentCookieJar implements CookieJar {

    private static final String PREF_NAME = "lingoswap_cookies";
    private static PersistentCookieJar instance;
    private SharedPreferences prefs;

    private PersistentCookieJar() {}

    public static PersistentCookieJar getInstance() {
        if (instance == null) {
            instance = new PersistentCookieJar();
        }
        return instance;
    }

    public void init(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        if (prefs == null) return;
        SharedPreferences.Editor editor = prefs.edit();
        for (Cookie cookie : cookies) {
            // Lưu dưới dạng: "host|cookieName" = "cookieValue|expiresAt"
            String key = url.host() + "|" + cookie.name();
            String value = cookie.value() + "|" + cookie.expiresAt();
            editor.putString(key, value);
        }
        editor.apply();
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        List<Cookie> cookies = new ArrayList<>();
        if (prefs == null) return cookies;

        Set<String> keys = prefs.getAll().keySet();
        for (String key : keys) {
            if (key.startsWith(url.host() + "|")) {
                String cookieName = key.split("\\|")[1];
                String raw = prefs.getString(key, null);
                if (raw == null) continue;
                String cookieValue = raw.split("\\|")[0];

                Cookie cookie = new Cookie.Builder()
                        .domain(url.host())
                        .path("/")
                        .name(cookieName)
                        .value(cookieValue)
                        .httpOnly()
                        .build();
                cookies.add(cookie);
            }
        }
        return cookies;
    }

    public void clear() {
        if (prefs != null) prefs.edit().clear().apply();
    }
}
