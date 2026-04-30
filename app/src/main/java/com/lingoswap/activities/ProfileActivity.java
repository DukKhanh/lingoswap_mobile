package com.lingoswap.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import com.lingoswap.R;

public class ProfileActivity extends AppCompatActivity {

    private EditText etFullName, etBio, etEmail;
    private EditText etCurrentPw, etNewPw, etConfirmNewPw;
    private TextView tvHeroName, tvHeroEmail, tvHeroInitial;
    private Spinner  spinnerCountry;
    private boolean  isDarkMode = false;
    private boolean  isEnglish  = true;

    private static final String PREF_NAME      = "lingoswap_prefs";
    private static final String KEY_DARK_MODE  = "dark_mode";
    private static final String KEY_LANGUAGE   = "language";
    private static final String KEY_FULL_NAME  = "full_name";
    private static final String KEY_BIO        = "bio";
    private static final String KEY_EMAIL      = "email";
    private static final String KEY_COUNTRY    = "country";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // ── Khởi tạo view ──────────────────────────────────────────
        TextView tvBackHome   = findViewById(R.id.tvBackHome);
        tvHeroName            = findViewById(R.id.tvHeroName);
        tvHeroEmail           = findViewById(R.id.tvHeroEmail);
        tvHeroInitial         = findViewById(R.id.tvHeroInitial);
        
        etFullName            = findViewById(R.id.etFullName);
        etBio                 = findViewById(R.id.etBio);
        etEmail               = findViewById(R.id.etEmail);
        etCurrentPw           = findViewById(R.id.etCurrentPw);
        etNewPw               = findViewById(R.id.etNewPw);
        etConfirmNewPw        = findViewById(R.id.etConfirmNewPw);
        spinnerCountry        = findViewById(R.id.spinnerCountry);

        Button       btnSaveProfile = findViewById(R.id.btnSaveProfile);
        Button       btnChangePw    = findViewById(R.id.btnUpdatePassword);
        LinearLayout appearLight    = findViewById(R.id.appearLight);
        LinearLayout appearDark     = findViewById(R.id.appearDark);
        
        TextView     btnLangVI      = findViewById(R.id.btnLangVI);
        TextView     btnLangEN      = findViewById(R.id.btnLangEN);
        Button       btnDefaultVI   = findViewById(R.id.btnDefaultVI);
        Button       btnDefaultEN   = findViewById(R.id.btnDefaultEN);

        // ── Country Spinner ────────────────────────────────────────
        String[] countries = {"🇻🇳 Vietnam","🇺🇸 United States","🇯🇵 Japan",
                              "🇰🇷 Korea","🇫🇷 France","🇩🇪 Germany","🇪🇸 Spain"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, countries);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spinnerCountry != null) spinnerCountry.setAdapter(adapter);

        // ── Nạp dữ liệu đã lưu ────────────────────────────────────
        String savedName  = prefs.getString(KEY_FULL_NAME, "TruongKAr");
        String savedEmail = prefs.getString(KEY_EMAIL, "baotruong11298@gmail.com");
        String savedBio   = prefs.getString(KEY_BIO, "Truong");
        int savedCountry  = prefs.getInt(KEY_COUNTRY, 0);

        if (etFullName != null) etFullName.setText(savedName);
        if (etBio != null) etBio.setText(savedBio);
        if (etEmail != null) etEmail.setText(savedEmail);
        if (tvHeroName != null) tvHeroName.setText(savedName);
        if (tvHeroEmail != null) tvHeroEmail.setText(savedEmail);
        if (tvHeroInitial != null && !savedName.isEmpty()) tvHeroInitial.setText(String.valueOf(savedName.charAt(0)).toUpperCase());

        isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        isEnglish  = prefs.getBoolean(KEY_LANGUAGE, true);
        if (spinnerCountry != null) spinnerCountry.setSelection(savedCountry);

        // ── Áp dụng trạng thái ────────────────────────────────────
        applyDarkMode(isDarkMode, appearLight, appearDark);
        // Lưu ý: btnLangVI/EN ở top row chỉ là toggle hiển thị (như hình ảnh bạn gửi)
        
        // ── Back ───────────────────────────────────────────────────
        if (tvBackHome != null) tvBackHome.setOnClickListener(v -> finish());

        // ── Lưu thông tin cá nhân ──────────────────────────────────
        if (btnSaveProfile != null) {
            btnSaveProfile.setOnClickListener(v -> {
                String name  = etFullName.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                String bio   = etBio.getText().toString().trim();

                if (TextUtils.isEmpty(name)) {
                    etFullName.setError("Tên không được để trống");
                    etFullName.requestFocus();
                    return;
                }
                if (!TextUtils.isEmpty(email) && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    etEmail.setError("Email không hợp lệ");
                    etEmail.requestFocus();
                    return;
                }

                // Cập nhật giao diện Hero Card ngay lập tức
                if (tvHeroName != null) tvHeroName.setText(name);
                if (tvHeroEmail != null) tvHeroEmail.setText(email);
                if (tvHeroInitial != null && !name.isEmpty()) {
                    tvHeroInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
                }

                // Lưu vào SharedPreferences
                prefs.edit()
                     .putString(KEY_FULL_NAME, name)
                     .putString(KEY_BIO, bio)
                     .putString(KEY_EMAIL, email)
                     .putInt(KEY_COUNTRY, spinnerCountry.getSelectedItemPosition())
                     .apply();

                Toast.makeText(this, "Đã lưu hồ sơ!", Toast.LENGTH_SHORT).show();
            });
        }

        // ── Đổi mật khẩu ──────────────────────────────────────────
        if (btnChangePw != null) {
            btnChangePw.setOnClickListener(v -> {
                String curPw     = etCurrentPw.getText().toString();
                String newPw     = etNewPw.getText().toString();
                String confirmPw = etConfirmNewPw.getText().toString();

                if (TextUtils.isEmpty(curPw)) {
                    etCurrentPw.setError("Nhập mật khẩu hiện tại");
                    etCurrentPw.requestFocus();
                    return;
                }
                if (newPw.length() < 8) {
                    etNewPw.setError("Mật khẩu mới phải ít nhất 8 ký tự");
                    etNewPw.requestFocus();
                    return;
                }
                if (!newPw.equals(confirmPw)) {
                    etConfirmNewPw.setError("Mật khẩu xác nhận không khớp");
                    etConfirmNewPw.requestFocus();
                    return;
                }

                etCurrentPw.setText("");
                etNewPw.setText("");
                etConfirmNewPw.setText("");
                Toast.makeText(this, "Mật khẩu đã được cập nhật!", Toast.LENGTH_SHORT).show();
            });
        }

        // ── Dark / Light mode ──────────────────────────────────────
        if (appearLight != null) {
            appearLight.setOnClickListener(v -> {
                isDarkMode = false;
                applyDarkMode(false, appearLight, appearDark);
                prefs.edit().putBoolean(KEY_DARK_MODE, false).apply();
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            });
        }
        if (appearDark != null) {
            appearDark.setOnClickListener(v -> {
                isDarkMode = true;
                applyDarkMode(true, appearLight, appearDark);
                prefs.edit().putBoolean(KEY_DARK_MODE, true).apply();
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            });
        }

        // ── Default Language Settings ──────────────────────────────
        if (btnDefaultVI != null) {
            btnDefaultVI.setOnClickListener(v -> {
                isEnglish = false;
                applyLanguageToggle(false, btnDefaultVI, btnDefaultEN);
                prefs.edit().putBoolean(KEY_LANGUAGE, false).apply();
            });
        }
        if (btnDefaultEN != null) {
            btnDefaultEN.setOnClickListener(v -> {
                isEnglish = true;
                applyLanguageToggle(true, btnDefaultVI, btnDefaultEN);
                prefs.edit().putBoolean(KEY_LANGUAGE, true).apply();
            });
        }
    }

    private void applyDarkMode(boolean dark, LinearLayout light, LinearLayout darkLayout) {
        if (light != null) light.setBackgroundResource(dark ? 0 : R.drawable.bg_lang_option_selected);
        if (darkLayout != null) darkLayout.setBackgroundResource(dark ? R.drawable.bg_lang_option_selected : 0);
    }

    private void applyLanguageToggle(boolean english, Button btnVI, Button btnEN) {
        if (btnVI != null) {
            btnVI.setBackgroundResource(english ? android.R.color.transparent : R.drawable.bg_tab_active);
            btnVI.setTextColor(ContextCompat.getColor(this, english ? R.color.text_muted : R.color.white));
        }
        if (btnEN != null) {
            btnEN.setBackgroundResource(english ? R.drawable.bg_tab_active : android.R.color.transparent);
            btnEN.setTextColor(ContextCompat.getColor(this, english ? R.color.white : R.color.text_muted));
        }
    }
}
