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

    private static final String KEY_BIO     = "bio";
    private static final String KEY_COUNTRY = "country";

    @Override
    protected ActivityProfileBinding inflateBinding(LayoutInflater inflater) {
        return ActivityProfileBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        // ── Country Spinner ────────────────────────────────────────────
        String[] countries = getResources().getStringArray(R.array.countries_array);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, countries);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCountry.setAdapter(adapter);

        // ── Nạp dữ liệu từ UserPreferences (sau login) ────────────────
        SharedPreferences prefs = getSharedPreferences("lingoswap_prefs", MODE_PRIVATE);

        // Ưu tiên lấy từ UserPreferences (đã lưu khi login)
        // Fallback sang prefs local nếu user đã chỉnh sửa tay
        String fullName = userPreferences.getFullName();
        if (TextUtils.isEmpty(fullName)) {
            fullName = prefs.getString("full_name", "");
        }

        String email = userPreferences.getEmail();

        String bio = prefs.getString(KEY_BIO, "");

        // Country: map từ string code sang spinner index
        String countryCode = userPreferences.getCountry();
        int countryIndex   = getCountryIndex(countryCode, prefs.getInt(KEY_COUNTRY, 0));

        // ── Bind vào UI ────────────────────────────────────────────────
        binding.etFullName.setText(fullName);
        binding.etEmail.setText(email);
        binding.etBio.setText(bio);
        binding.tvHeroName.setText(fullName.isEmpty() ? "User" : fullName);
        binding.tvHeroEmail.setText(email);

        String initial = fullName.isEmpty() ? "U" : String.valueOf(fullName.charAt(0)).toUpperCase();
        binding.tvHeroInitial.setText(initial);

        binding.spinnerCountry.setSelection(countryIndex);

        // ── Áp dụng trạng thái theme / ngôn ngữ ───────────────────────
        updateAppearanceUI();
        updateLanguageButtonLabel();

        // ── Back ───────────────────────────────────────────────────────
        binding.tvBackHome.setOnClickListener(v -> finish());

        // ── Lưu thông tin cá nhân ──────────────────────────────────────
        binding.btnSaveProfile.setOnClickListener(v -> {
            String name     = binding.etFullName.getText().toString().trim();
            String newEmail = binding.etEmail.getText().toString().trim();
            String newBio   = binding.etBio.getText().toString().trim();
            int    countryPos = binding.spinnerCountry.getSelectedItemPosition();

            if (TextUtils.isEmpty(name)) {
                binding.etFullName.setError(getString(R.string.full_name) + " empty");
                binding.etFullName.requestFocus();
                return;
            }
            if (!TextUtils.isEmpty(newEmail) && !Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                binding.etEmail.setError("Email invalid");
                binding.etEmail.requestFocus();
                return;
            }

            // Cập nhật UI hero
            binding.tvHeroName.setText(name);
            binding.tvHeroEmail.setText(newEmail);
            binding.tvHeroInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());

            // Lưu vào SharedPreferences local
            prefs.edit()
                 .putString("full_name", name)
                 .putString(KEY_BIO, newBio)
                 .putInt(KEY_COUNTRY, countryPos)
                 .apply();

            // Lưu vào UserPreferences để đồng bộ
            userPreferences.saveProfile(name, newBio, getCountryCode(countryPos));

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

        // ── Appearance ─────────────────────────────────────────────────
        binding.appearLight.setOnClickListener(v -> {
            themeManager.setTheme(ThemeManager.LIGHT);
            recreate();
        });
        binding.appearDark.setOnClickListener(v -> {
            themeManager.setTheme(ThemeManager.DARK);
            recreate();
        });

        // ── App Language ───────────────────────────────────────────────
        binding.btnChangeLanguage.setOnClickListener(v ->
            new AppLanguageDialog().show(getSupportFragmentManager(), "app_lang")
        );

        // ── Save settings ──────────────────────────────────────────────
        binding.btnSaveSettings.setOnClickListener(v ->
            Toast.makeText(this, R.string.toast_settings_saved, Toast.LENGTH_SHORT).show()
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────

    /**
     * Map country code từ backend (VD: "vi", "en", "jp")
     * sang index trong countries_array.
     * Fallback về savedIndex nếu không khớp.
     */
    private int getCountryIndex(String countryCode, int fallback) {
        if (TextUtils.isEmpty(countryCode)) return fallback;
        String[] countries = getResources().getStringArray(R.array.countries_array);
        String lower = countryCode.toLowerCase();
        // Map code → keyword tìm trong tên quốc gia
        String keyword;
        switch (lower) {
            case "vi": case "vn": keyword = "Vietnam";       break;
            case "us": case "en": keyword = "United States"; break;
            case "jp": case "ja": keyword = "Japan";         break;
            case "kr": case "ko": keyword = "Korea";         break;
            case "fr":            keyword = "France";        break;
            case "de":            keyword = "Germany";       break;
            case "es":            keyword = "Spain";         break;
            default:              return fallback;
        }
        for (int i = 0; i < countries.length; i++) {
            if (countries[i].contains(keyword)) return i;
        }
        return fallback;
    }

    /** Map index spinner → country code để lưu vào UserPreferences */
    private String getCountryCode(int index) {
        switch (index) {
            case 0: return "vn";
            case 1: return "us";
            case 2: return "jp";
            case 3: return "kr";
            case 4: return "fr";
            case 5: return "de";
            case 6: return "es";
            default: return "";
        }
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
