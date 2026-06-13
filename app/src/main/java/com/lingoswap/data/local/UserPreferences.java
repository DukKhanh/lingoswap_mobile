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
    private static final String PREF_NAME    = "lingoswap_prefs";
    private static final String KEY_TOKEN    = "access_token";
    private static final String KEY_USER     = "user_json";
    private static final String KEY_ROLE     = "user_role";
    private static final String KEY_USER_ID  = "user_id";

    // ── Profile fields lưu riêng để đọc nhanh ────────────────────────
    private static final String KEY_FULL_NAME = "user_full_name";
    private static final String KEY_EMAIL     = "user_email";
    private static final String KEY_COUNTRY   = "user_country";
    private static final String KEY_AVATAR    = "user_avatar";

    private static final String KEY_NIGHT_MODE = "night_mode";
    private static final String KEY_APP_LANG   = "app_language";

    private final SharedPreferences prefs;
    private final Gson gson;

    @Inject
    public UserPreferences(@ApplicationContext Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson  = new Gson();
    }

    public UserPreferences(Context context, boolean manual) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson  = new Gson();
    }

    /** Lưu toàn bộ AuthResponse sau login/register/googleLogin */
    public void saveAuthResponse(AuthResponse response) {
        SharedPreferences.Editor editor = prefs.edit()
            .putString(KEY_TOKEN,   response.getAccessToken())
            .putString(KEY_ROLE,    response.getRole())
            .putString(KEY_USER_ID, response.getId())
            .putString(KEY_USER,    gson.toJson(response));

        // Lưu thêm profile fields để đọc nhanh
        if (response.getProfile() != null) {
            AuthResponse.Profile p = response.getProfile();
            if (p.getFullName() != null) editor.putString(KEY_FULL_NAME, p.getFullName());
            if (p.getCountry()  != null) editor.putString(KEY_COUNTRY,   p.getCountry());
            if (p.getAvatar()   != null) editor.putString(KEY_AVATAR,    p.getAvatar());
        }
        if (response.getEmail() != null) editor.putString(KEY_EMAIL, response.getEmail());

        editor.apply();
    }

    public void saveAccessToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    // ── Getters token / auth ──────────────────────────────────────────

    public String getAccessToken() { return prefs.getString(KEY_TOKEN,   null); }
    public String getRole()        { return prefs.getString(KEY_ROLE,    null); }
    public String getUserId()      { return prefs.getString(KEY_USER_ID, null); }
    public boolean isLoggedIn()    { return getAccessToken() != null; }

    // ── Getters profile ───────────────────────────────────────────────

    public String getFullName() { return prefs.getString(KEY_FULL_NAME, ""); }
    public String getEmail()    { return prefs.getString(KEY_EMAIL,     ""); }
    public String getCountry()  { return prefs.getString(KEY_COUNTRY,   ""); }
    public String getAvatar()   { return prefs.getString(KEY_AVATAR,    ""); }

    /** Lưu profile sau khi user chỉnh sửa trên ProfileActivity */
    public void saveProfile(String fullName, String bio, String country) {
        prefs.edit()
            .putString(KEY_FULL_NAME, fullName)
            .putString(KEY_COUNTRY,   country)
            .apply();
    }

    public void clear() { prefs.edit().clear().apply(); }

    // ── Dark Mode ─────────────────────────────────────────────────────

    public void setNightMode(int mode) { prefs.edit().putInt(KEY_NIGHT_MODE, mode).apply(); }

    public int getNightMode() {
        return prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public boolean isDarkMode(Context context) {
        int mode = getNightMode();
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) return true;
        if (mode == AppCompatDelegate.MODE_NIGHT_NO)  return false;
        int uiMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == Configuration.UI_MODE_NIGHT_YES;
    }

    // ── Language ──────────────────────────────────────────────────────

    public void setAppLanguage(String langCode) {
        prefs.edit().putString(KEY_APP_LANG, langCode).apply();
    }

    public String getAppLanguage() { return prefs.getString(KEY_APP_LANG, "en"); }
}
