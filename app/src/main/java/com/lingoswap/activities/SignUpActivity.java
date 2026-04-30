package com.lingoswap.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.lingoswap.R;

public class SignUpActivity extends AppCompatActivity {

    private EditText etFullName, etEmail, etPassword, etConfirmPassword;
    private ImageView ivTogglePassword;
    private boolean pwVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        etFullName        = findViewById(R.id.etFullName);
        etEmail           = findViewById(R.id.etEmail);
        etPassword        = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        ivTogglePassword  = findViewById(R.id.ivTogglePassword);

        Button   btnCreate   = findViewById(R.id.btnCreateAccount);
        Button   btnGoogle   = findViewById(R.id.btnGoogleSignUp);
        TextView tvSignIn    = findViewById(R.id.tvSignIn);

        // ── Hiện / ẩn mật khẩu ─────────────────────────────────────
        ivTogglePassword.setOnClickListener(v -> {
            pwVisible = !pwVisible;
            etPassword.setInputType(pwVisible
                    ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            etPassword.setSelection(etPassword.getText().length());
            ivTogglePassword.setImageResource(pwVisible
                    ? android.R.drawable.ic_menu_view
                    : android.R.drawable.ic_secure);
        });

        // ── Tạo tài khoản ───────────────────────────────────────────
        btnCreate.setOnClickListener(v -> {
            String name      = etFullName.getText().toString().trim();
            String email     = etEmail.getText().toString().trim();
            String pw        = etPassword.getText().toString();
            String pwConfirm = etConfirmPassword.getText().toString();

            // Kiểm tra tên
            if (TextUtils.isEmpty(name)) {
                etFullName.setError("Họ tên không được để trống");
                etFullName.requestFocus();
                return;
            }
            if (name.length() < 2) {
                etFullName.setError("Tên quá ngắn");
                etFullName.requestFocus();
                return;
            }

            // Kiểm tra email
            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Email không được để trống");
                etEmail.requestFocus();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Định dạng email không hợp lệ");
                etEmail.requestFocus();
                return;
            }

            // Kiểm tra mật khẩu
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
            if (!pw.matches(".*[A-Z].*")) {
                etPassword.setError("Mật khẩu phải có ít nhất 1 chữ hoa");
                etPassword.requestFocus();
                return;
            }
            if (!pw.matches(".*\\d.*")) {
                etPassword.setError("Mật khẩu phải có ít nhất 1 chữ số");
                etPassword.requestFocus();
                return;
            }

            // Xác nhận mật khẩu
            if (!pw.equals(pwConfirm)) {
                etConfirmNewPw_Logic(etConfirmPassword);
                return;
            }

            // TODO: gọi API đăng ký thực tế ở đây
            Toast.makeText(this, "Tạo tài khoản thành công!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });

        btnGoogle.setOnClickListener(v ->
            Toast.makeText(this, "Google Sign-Up — chưa tích hợp", Toast.LENGTH_SHORT).show()
        );

        tvSignIn.setOnClickListener(v -> finish());
    }

    private void etConfirmNewPw_Logic(EditText etConfirmPassword) {
        etConfirmPassword.setError("Mật khẩu xác nhận không khớp");
        etConfirmPassword.requestFocus();
    }
}
