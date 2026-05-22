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
import com.lingoswap.utils.LocaleManager;
import com.lingoswap.utils.ThemeManager;

import javax.inject.Inject;

public abstract class BaseActivity<VB extends ViewBinding> extends AppCompatActivity {

    protected VB binding;

    @Inject protected ThemeManager themeManager;
    @Inject protected LocaleManager localeManager;

    protected abstract VB inflateBinding(LayoutInflater inflater);

    @Override
    protected void attachBaseContext(Context newBase) {
        // Step 3: Ensure BaseActivity apply locale
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Apply theme before inflating layout
        if (themeManager != null) {
            themeManager.applyTheme();
        }

        binding = inflateBinding(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupCommonToggles();
        setupViews();
        observeViewModel();
    }

    private void setupCommonToggles() {
        // Dark toggle
        View tvDarkToggle = findViewById(R.id.tvDarkToggle);
        if (tvDarkToggle instanceof TextView) {
            TextView tv = (TextView) tvDarkToggle;
            tv.setText(isDarkMode() ? "☀️" : "🌙");
            tv.setOnClickListener(v -> toggleTheme());
        }

        // Lang toggle
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

    // ── Helper methods ─────────────────────────────────────────────
    public void toggleTheme() {
        int newMode = themeManager.isDark(this) ? ThemeManager.LIGHT : ThemeManager.DARK;
        themeManager.setTheme(newMode);
        recreate();
    }

    /**
     * Changes the application language and restarts the app task stack.
     * Step 5: After changing language, restart all app stack.
     */
    public void changeLanguage(String langCode) {
        if (localeManager != null) {
            localeManager.setLocale(this, langCode);
            
            // Restart entire app task stack
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    public boolean isDarkMode() {
        return themeManager != null && themeManager.isDark(this);
    }

    public String getCurrentLanguage() {
        return localeManager != null ? localeManager.getLanguage() : LocaleManager.LANG_EN;
    }
}
