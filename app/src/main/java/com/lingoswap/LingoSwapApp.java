package com.lingoswap;

import android.app.Application;

import com.lingoswap.data.api.PersistentCookieJar;
import com.lingoswap.data.local.UserPreferences;
import com.lingoswap.utils.LocaleManager;
import com.lingoswap.utils.NotificationHelper;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class LingoSwapApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Áp ngôn ngữ đã lưu (AppCompat per-app locales). Lần đầu chưa có → lấy default trong prefs.
        new LocaleManager(new UserPreferences(this, true)).applyPersisted();

        PersistentCookieJar.getInstance().init(this);
        NotificationHelper.createChannels(this);
    }
}
