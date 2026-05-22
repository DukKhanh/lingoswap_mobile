package com.lingoswap.activities;

import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.lingoswap.R;
import com.lingoswap.data.local.UserPreferences;
import com.lingoswap.databinding.ActivityProfileBinding;
import com.lingoswap.presentation.base.BaseActivity;
import com.lingoswap.utils.LocaleManager;
import com.lingoswap.utils.ThemeManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ProfileActivity extends BaseActivity<ActivityProfileBinding> {

    @Inject UserPreferences userPreferences;

    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_BIO       = "bio";
    private static final String KEY_COUNTRY   = "country";

    @Override
    protected ActivityProfileBinding inflateBinding(LayoutInflater inflater) {
        return ActivityProfileBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        // ── Country Spinner ────────────────────────────────────────────
        // Sử dụng string-array từ resources để có thể dịch được
        String[] countries = getResources().getStringArray(R.array.countries_array);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, countries);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCountry.setAdapter(adapter);

        // ── Nạp dữ liệu đã lưu ────────────────────────────────────────
        SharedPreferences prefs = getSharedPreferences("lingoswap_prefs", MODE_PRIVATE);

        String savedName    = prefs.getString(KEY_FULL_NAME, "TruongKAr");
        String savedEmail   = userPreferences.getAccessToken() != null
                              ? "user@example.com" : "baotruong11298@gmail.com";
        String savedBio     = prefs.getString(KEY_BIO, "Truong");
        int    savedCountry = prefs.getInt(KEY_COUNTRY, 0);

        binding.etFullName.setText(savedName);
        binding.etBio.setText(savedBio);
        binding.etEmail.setText(savedEmail);
        binding.tvHeroName.setText(savedName);
        binding.tvHeroEmail.setText(savedEmail);
        if (!savedName.isEmpty()) {
            binding.tvHeroInitial.setText(String.valueOf(savedName.charAt(0)).toUpperCase());
        }
        binding.spinnerCountry.setSelection(savedCountry);

        // ── Áp dụng trạng thái theme / ngôn ngữ ───────────────────────
        updateAppearanceUI();
        updateLanguageButtonLabel();

        // ── Back ───────────────────────────────────────────────────────
        binding.tvBackHome.setOnClickListener(v -> finish());

        // ── Lưu thông tin cá nhân ──────────────────────────────────────
        binding.btnSaveProfile.setOnClickListener(v -> {
            String name  = binding.etFullName.getText().toString().trim();
            String email = binding.etEmail.getText().toString().trim();
            String bio   = binding.etBio.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                binding.etFullName.setError(getString(R.string.full_name) + " empty");
                binding.etFullName.requestFocus();
                return;
            }
            if (!TextUtils.isEmpty(email) && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.setError("Email invalid");
                binding.etEmail.requestFocus();
                return;
            }

            binding.tvHeroName.setText(name);
            binding.tvHeroEmail.setText(email);
            if (!name.isEmpty()) {
                binding.tvHeroInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
            }

            prefs.edit()
                 .putString(KEY_FULL_NAME, name)
                 .putString(KEY_BIO, bio)
                 .putInt(KEY_COUNTRY, binding.spinnerCountry.getSelectedItemPosition())
                 .apply();

            Toast.makeText(this, R.string.toast_profile_saved, Toast.LENGTH_SHORT).show();
        });

        // ── Đổi mật khẩu ──────────────────────────────────────────────
        binding.btnUpdatePassword.setOnClickListener(v -> {
            String curPw     = binding.etCurrentPw.getText().toString();
            String newPw     = binding.etNewPw.getText().toString();
            String confirmPw = binding.etConfirmNewPw.getText().toString();

            if (TextUtils.isEmpty(curPw)) {
                binding.etCurrentPw.setError(getString(R.string.current_password) + " empty");
                binding.etCurrentPw.requestFocus();
                return;
            }
            if (newPw.length() < 8) {
                binding.etNewPw.setError("Min 8 chars");
                binding.etNewPw.requestFocus();
                return;
            }
            if (!newPw.equals(confirmPw)) {
                binding.etConfirmNewPw.setError("Not match");
                binding.etConfirmNewPw.requestFocus();
                return;
            }

            binding.etCurrentPw.setText("");
            binding.etNewPw.setText("");
            binding.etConfirmNewPw.setText("");
            Toast.makeText(this, R.string.toast_password_updated, Toast.LENGTH_SHORT).show();
        });

        // ── Appearance (Dark / Light) ──────────────────────────────────
        binding.appearLight.setOnClickListener(v -> {
            themeManager.setTheme(ThemeManager.LIGHT);
            recreate();
        });
        binding.appearDark.setOnClickListener(v -> {
            themeManager.setTheme(ThemeManager.DARK);
            recreate();
        });

        // ── App Language – mở AppLanguageDialog ───────────────────────
        binding.btnChangeLanguage.setOnClickListener(v ->
            new AppLanguageDialog().show(getSupportFragmentManager(), "app_lang")
        );

        // ── Save settings ──────────────────────────────────────────────
        binding.btnSaveSettings.setOnClickListener(v ->
            Toast.makeText(this, R.string.toast_settings_saved, Toast.LENGTH_SHORT).show()
        );
    }

    private void updateAppearanceUI() {
        boolean dark = isDarkMode();
        binding.appearLight.setBackgroundResource(dark ? 0 : R.drawable.bg_lang_option_selected);
        binding.appearDark.setBackgroundResource(dark ? R.drawable.bg_lang_option_selected : 0);
        binding.frameLightCheck.setVisibility(dark ? View.GONE  : View.VISIBLE);
        binding.frameDarkCheck.setVisibility(dark ? View.VISIBLE : View.GONE);
    }

    private void updateLanguageButtonLabel() {
        String langName = getLangDisplayName(getCurrentLanguage());
        binding.btnChangeLanguage.setText("🌐 " + langName);
    }

    private String getLangDisplayName(String code) {
        switch (code) {
            case LocaleManager.LANG_VI: return "Tiếng Việt";
            case LocaleManager.LANG_JA: return "日本語";
            case LocaleManager.LANG_KO: return "한국어";
            case LocaleManager.LANG_ZH: return "中文";
            case LocaleManager.LANG_FR: return "Français";
            case LocaleManager.LANG_DE: return "Deutsch";
            case LocaleManager.LANG_ES: return "Español";
            default:                    return "English";
        }
    }

    @Override
    protected void observeViewModel() { }
}
