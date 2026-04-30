package com.lingoswap.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.lingoswap.R;
import com.lingoswap.presentation.auth.forgotpassword.ForgotPasswordActivity;

public class SignInActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private ImageView ivTogglePassword;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);

        Button   btnSignIn       = findViewById(R.id.btnSignIn);
        Button   btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        TextView btnLangVI       = findViewById(R.id.btnLangVI);
        TextView btnLangEN       = findViewById(R.id.btnLangEN);
        TextView tvForgotPw      = findViewById(R.id.tvForgotPassword);
        TextView tvSignUp        = findViewById(R.id.tvSignUp);

        // ── Hiện / ẩn mật khẩu ─────────────────────────────────────
        ivTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            etPassword.setInputType(passwordVisible
                    ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            etPassword.setSelection(etPassword.getText().length());
            ivTogglePassword.setImageResource(passwordVisible
                    ? android.R.drawable.ic_menu_view
                    : android.R.drawable.ic_secure);
        });

        // ── Đăng nhập ───────────────────────────────────────────────
        btnSignIn.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pw    = etPassword.getText().toString();

            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Email không được để trống");
                etEmail.requestFocus();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Email không hợp lệ");
                etEmail.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(pw)) {
                etPassword.setError("Mật khẩu không được để trống");
                etPassword.requestFocus();
                return;
            }
            if (pw.length() < 8) {
                etPassword.setError("Mật khẩu phải ít nhất 8 ký tự");
                etPassword.requestFocus();
                return;
            }

            // TODO: gọi API xác thực thực tế ở đây
            // Hiện tại chuyển thẳng vào HomeActivity
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });

        // ── Google Sign-In ──────────────────────────────────────────
        btnGoogleSignIn.setOnClickListener(v ->
            Toast.makeText(this, "Google Sign-In — chưa tích hợp", Toast.LENGTH_SHORT).show()
        );

        // ── Quên mật khẩu ──────────────────────────────────────────
        tvForgotPw.setOnClickListener(v ->
            startActivity(new Intent(this, ForgotPasswordActivity.class))
        );

        // ── Đăng ký ────────────────────────────────────────────────
        tvSignUp.setOnClickListener(v ->
            startActivity(new Intent(this, SignUpActivity.class))
        );

        // ── Ngôn ngữ giao diện ─────────────────────────────────────
        btnLangVI.setOnClickListener(v -> setAppLanguage("vi",   btnLangVI, btnLangEN));
        btnLangEN.setOnClickListener(v -> setAppLanguage("en",   btnLangVI, btnLangEN));
    }

    /**
     * Chuyển ngôn ngữ ứng dụng (locale).
     */
    private void setAppLanguage(String lang, TextView btnVI, TextView btnEN) {
        boolean isVI = "vi".equals(lang);
        btnVI.setBackgroundResource(isVI  ? R.drawable.bg_tab_active   : android.R.color.transparent);
        btnVI.setTextColor(getColor(isVI  ? R.color.white              : R.color.text_muted));
        btnEN.setBackgroundResource(!isVI ? R.drawable.bg_tab_active   : android.R.color.transparent);
        btnEN.setTextColor(getColor(!isVI ? R.color.white              : R.color.text_muted));
        Toast.makeText(this,
                isVI ? "Đã chọn Tiếng Việt" : "English selected",
                Toast.LENGTH_SHORT).show();
    }
}
