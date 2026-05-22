package com.lingoswap.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.gson.Gson;
import com.lingoswap.data.model.AuthResponse;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class UserPreferences {
    private static final String PREF_NAME  = "lingoswap_prefs";
    private static final String KEY_TOKEN  = "access_token";
    private static final String KEY_USER   = "user_json";
    private static final String KEY_ROLE   = "user_role";
    private static final String KEY_USER_ID = "user_id";

    private static final String KEY_NIGHT_MODE = "night_mode";
    private static final String KEY_APP_LANG = "app_language";

    private final SharedPreferences prefs;
    private final Gson gson;

    @Inject
    public UserPreferences(@ApplicationContext Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson  = new Gson();
    }

    // Constructor phụ để dùng trong attachBaseContext (trước Hilt)
    public UserPreferences(Context context, boolean manual) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public void saveAuthResponse(AuthResponse response) {
        prefs.edit()
            .putString(KEY_TOKEN, response.getAccessToken())
            .putString(KEY_ROLE,  response.getRole())
            .putString(KEY_USER_ID, response.getId())
            .putString(KEY_USER,  gson.toJson(response))
            .apply();
    }

    /** Cập nhật chỉ accessToken sau khi refresh thành công */
    public void saveAccessToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getAccessToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, null);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    // ── Dark Mode ─────────────────────────────────────────────────────
    public void setNightMode(int mode) {
        prefs.edit().putInt(KEY_NIGHT_MODE, mode).apply();
    }

    public int getNightMode() {
        return prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public boolean isDarkMode(Context context) {
        int mode = getNightMode();
        if (mode == AppCompatDelegate.MODE_NIGHT_YES)  return true;
        if (mode == AppCompatDelegate.MODE_NIGHT_NO)   return false;
        int uiMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == Configuration.UI_MODE_NIGHT_YES;
    }

    // ── Language ──────────────────────────────────────────────────────
    public void setAppLanguage(String langCode) {
        prefs.edit().putString(KEY_APP_LANG, langCode).apply();
    }

    public String getAppLanguage() {
        return prefs.getString(KEY_APP_LANG, "en");
    }
}
