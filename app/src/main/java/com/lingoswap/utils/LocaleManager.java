package com.lingoswap.utils;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.lingoswap.data.local.UserPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LocaleManager {
    public static final String LANG_EN = "en";
    public static final String LANG_VI = "vi";
    public static final String LANG_JA = "ja";
    public static final String LANG_KO = "ko";
    public static final String LANG_ZH = "zh";
    public static final String LANG_FR = "fr";
    public static final String LANG_DE = "de";
    public static final String LANG_ES = "es";

    private final UserPreferences userPreferences;

    @Inject
    public LocaleManager(UserPreferences userPreferences) {
        this.userPreferences = userPreferences;
    }

    // Locale do AppCompat quản (setApplicationLocales) nên không cần bọc context thủ công.
    public static Context setLocale(Context context) { return context; }

    public Context wrap(Context ctx) { return ctx; }

    public void setLocale(Context ctx, String lang) {
        userPreferences.setAppLanguage(lang);
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang));
    }

    /** Áp ngôn ngữ đã lưu lúc khởi động (khi AppCompat chưa có locale nào). */
    public void applyPersisted() {
        if (AppCompatDelegate.getApplicationLocales().isEmpty()) {
            AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(userPreferences.getAppLanguage()));
        }
    }

    public String getLanguage() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (!locales.isEmpty() && locales.get(0) != null) {
            return locales.get(0).getLanguage();
        }
        return userPreferences.getAppLanguage();
    }
}
