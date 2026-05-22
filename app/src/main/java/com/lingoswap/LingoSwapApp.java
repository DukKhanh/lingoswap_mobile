package com.lingoswap;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

import com.lingoswap.data.api.PersistentCookieJar;
import com.lingoswap.data.local.UserPreferences;
import com.lingoswap.utils.LocaleManager;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class LingoSwapApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // ✅ Init CookieJar để lưu refreshToken cookie từ backend
        PersistentCookieJar.getInstance().init(this);
    }

    // FIX #2: Override attachBaseContext ở Application để locale được
    // áp dụng ngay khi app khởi động lạnh (cold start), trước cả khi
    // Activity đầu tiên được tạo. Nếu thiếu, lần đầu mở app sẽ vẫn
    // hiển thị ngôn ngữ hệ thống dù user đã chọn ngôn ngữ khác.
    @Override
    protected void attachBaseContext(Context base) {
        UserPreferences prefs = new UserPreferences(base, true);
        LocaleManager localeManager = new LocaleManager(prefs);
        super.attachBaseContext(localeManager.wrap(base));
    }

    // FIX #2b: onConfigurationChanged đảm bảo locale được giữ nguyên
    // khi hệ thống thay đổi cấu hình (ví dụ: user đổi ngôn ngữ hệ thống
    // trong khi app đang chạy ở background).
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        UserPreferences prefs = new UserPreferences(this, true);
        LocaleManager localeManager = new LocaleManager(prefs);
        localeManager.wrap(this);
    }
}
