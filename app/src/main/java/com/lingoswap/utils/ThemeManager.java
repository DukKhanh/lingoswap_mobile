package com.lingoswap.utils;

import android.content.Context;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatDelegate;

import com.lingoswap.data.local.UserPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Cầu nối giữa UI và UserPreferences cho Dark Mode. */
@Singleton
public class ThemeManager {

    public static final int LIGHT  = AppCompatDelegate.MODE_NIGHT_NO;
    public static final int DARK   = AppCompatDelegate.MODE_NIGHT_YES;
    public static final int SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

    private final UserPreferences userPreferences;

    @Inject
    public ThemeManager(UserPreferences userPreferences) {
        this.userPreferences = userPreferences;
    }

    public void setTheme(int mode) {
        userPreferences.setNightMode(mode);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public void applyTheme() {
        AppCompatDelegate.setDefaultNightMode(userPreferences.getNightMode());
    }

    public boolean isDark(Context context) {
        return userPreferences.isDarkMode(context);
    }

    public int getSavedMode() {
        return userPreferences.getNightMode();
    }
}
