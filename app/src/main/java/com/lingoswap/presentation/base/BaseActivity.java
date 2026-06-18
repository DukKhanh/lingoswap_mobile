package com.lingoswap.presentation.base;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewbinding.ViewBinding;

import com.lingoswap.R;
import com.lingoswap.activities.HomeActivity;
import com.lingoswap.utils.ChatUnreadStore;
import com.lingoswap.utils.LocaleManager;
import com.lingoswap.utils.SocketManager;
import com.lingoswap.utils.ThemeManager;

import javax.inject.Inject;

import io.socket.emitter.Emitter;

public abstract class BaseActivity<VB extends ViewBinding> extends AppCompatActivity {

    protected VB binding;

    @Inject protected ThemeManager themeManager;
    @Inject protected LocaleManager localeManager;
    @Inject protected SocketManager socketManager;

    private Emitter.Listener chatUnreadListener;

    protected abstract VB inflateBinding(LayoutInflater inflater);

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (themeManager != null) {
            themeManager.applyTheme();
        }

        binding = inflateBinding(getLayoutInflater());
        setContentView(binding.getRoot());

        setupCommonToggles();
        setupChatUnreadDot();
        setupViews();
        observeViewModel();
    }

    /** Cho phép màn chat tự tắt việc đánh dấu chưa đọc (đang đọc rồi). */
    protected boolean shouldTrackChatUnread() { return true; }

    private void setupChatUnreadDot() {
        View dot = findViewById(R.id.chatUnreadDot);
        if (dot == null) return;
        ChatUnreadStore.get().observe(this, unread ->
                dot.setVisibility(Boolean.TRUE.equals(unread) ? View.VISIBLE : View.GONE));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!shouldTrackChatUnread()) return;
        chatUnreadListener = args -> ChatUnreadStore.markUnread();
        socketManager.onReceiveMessage(chatUnreadListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (chatUnreadListener != null) {
            socketManager.off("receive_message", chatUnreadListener);
            chatUnreadListener = null;
        }
    }

    private void setupCommonToggles() {
        View tvDarkToggle = findViewById(R.id.tvDarkToggle);
        if (tvDarkToggle instanceof android.widget.ImageView) {
            android.widget.ImageView iv = (android.widget.ImageView) tvDarkToggle;
            iv.setImageResource(isDarkMode() ? R.drawable.ic_sun : R.drawable.ic_moon);
            iv.setOnClickListener(v -> toggleTheme());
        }

        View btnLangVI = findViewById(R.id.btnLangVI);
        View btnLangEN = findViewById(R.id.btnLangEN);
        
        if (btnLangVI instanceof TextView) {
            btnLangVI.setOnClickListener(v -> changeLanguage(LocaleManager.LANG_VI));
        }
        if (btnLangEN instanceof TextView) {
            btnLangEN.setOnClickListener(v -> changeLanguage(LocaleManager.LANG_EN));
        }
        
        if (btnLangVI instanceof TextView && btnLangEN instanceof TextView) {
            syncLangToggleUI((TextView) btnLangVI, (TextView) btnLangEN);
        }

        View btnAppLanguage = findViewById(R.id.btnAppLanguage);
        if (btnAppLanguage instanceof TextView) {
            ((TextView) btnAppLanguage).setText(langDisplayName(getCurrentLanguage()));
            btnAppLanguage.setOnClickListener(this::showLanguageMenu);
        }

        View btnThemeToggle = findViewById(R.id.btnThemeToggle);
        if (btnThemeToggle != null) {
            btnThemeToggle.setOnClickListener(v -> toggleTheme());
        }
    }

    /** Dropdown chọn ngôn ngữ giao diện (Tiếng Việt / English), dùng chung login & Profile. */
    public void showLanguageMenu(View anchor) {
        android.widget.PopupMenu menu = new android.widget.PopupMenu(this, anchor);
        menu.getMenu().add(0, 0, 0, "Tiếng Việt");
        menu.getMenu().add(0, 1, 1, "English");
        menu.setOnMenuItemClickListener(item -> {
            changeLanguage(item.getItemId() == 0 ? LocaleManager.LANG_VI : LocaleManager.LANG_EN);
            return true;
        });
        menu.show();
    }

    protected String langDisplayName(String code) {
        return LocaleManager.LANG_VI.equals(code) ? "Tiếng Việt" : "English";
    }

    protected void syncLangToggleUI(TextView btnVI, TextView btnEN) {
        if (btnVI == null || btnEN == null) return;
        boolean isVI = LocaleManager.LANG_VI.equals(getCurrentLanguage());
        btnVI.setBackgroundResource(isVI ? R.drawable.bg_tab_active : android.R.color.transparent);
        btnVI.setTextColor(getResources().getColor(isVI ? R.color.white : R.color.text_muted, getTheme()));
        btnEN.setBackgroundResource(!isVI ? R.drawable.bg_tab_active : android.R.color.transparent);
        btnEN.setTextColor(getResources().getColor(!isVI ? R.color.white : R.color.text_muted, getTheme()));
    }

    protected abstract void setupViews();
    protected abstract void observeViewModel();

    public void toggleTheme() {
        int newMode = themeManager.isDark(this) ? ThemeManager.LIGHT : ThemeManager.DARK;
        themeManager.setTheme(newMode);
        recreate();
    }

    public void changeLanguage(String langCode) {
        if (localeManager != null) {
            // setApplicationLocales tự recreate toàn bộ Activity của app → đồng bộ ngay.
            localeManager.setLocale(this, langCode);
        }
    }

    public boolean isDarkMode() {
        return themeManager != null && themeManager.isDark(this);
    }

    public String getCurrentLanguage() {
        return localeManager != null ? localeManager.getLanguage() : LocaleManager.LANG_EN;
    }
}
