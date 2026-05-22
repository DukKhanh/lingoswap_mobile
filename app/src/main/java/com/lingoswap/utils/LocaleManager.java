package com.lingoswap.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.lingoswap.data.local.UserPreferences;

import java.util.Locale;
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

    /**
     * Static method to set locale and return wrapped context.
     * Used in attachBaseContext.
     */
    public static Context setLocale(Context context) {
        UserPreferences prefs = new UserPreferences(context, true);
        String lang = prefs.getAppLanguage();
        return applyLocale(context, lang);
    }

    public void setLocale(Context ctx, String lang) {
        userPreferences.setAppLanguage(lang);
        
        // Cập nhật qua AppCompatDelegate (chuẩn mới cho AppCompat 1.6+)
        LocaleListCompat appLocales = LocaleListCompat.forLanguageTags(lang);
        AppCompatDelegate.setApplicationLocales(appLocales);
        
        // Cập nhật thủ công để có hiệu lực ngay lập tức
        applyLocale(ctx, lang);
    }

    public Context wrap(Context ctx) {
        return applyLocale(ctx, userPreferences.getAppLanguage());
    }

    public String getLanguage() {
        return userPreferences.getAppLanguage();
    }

    private static Context applyLocale(Context ctx, String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Resources res = ctx.getResources();
        Configuration config = new Configuration(res.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(new LocaleList(locale));
        } else {
            config.setLocale(locale);
        }

        // Cập nhật Resources cho context hiện tại
        res.updateConfiguration(config, res.getDisplayMetrics());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return ctx.createConfigurationContext(config);
        }
        return ctx;
    }
}
