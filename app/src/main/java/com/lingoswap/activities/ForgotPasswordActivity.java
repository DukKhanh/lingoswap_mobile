package com.lingoswap.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.lingoswap.R;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmail = findViewById(R.id.etEmail);
        Button btnSendOtp   = findViewById(R.id.btnSendOtp);
        TextView tvBackToSignIn = findViewById(R.id.tvBackToSignIn);

        btnSendOtp.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

            // ── Validate ────────────────────────────────────────────
            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Vui lòng nhập email");
                etEmail.requestFocus();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Định dạng email không hợp lệ");
                etEmail.requestFocus();
                return;
            }

            // TODO: gọi API gửi OTP thực tế ở đây
            Toast.makeText(this, "OTP đã được gửi tới " + email, Toast.LENGTH_SHORT).show();

            // Chuyển sang màn hình nhập OTP kèm email
            Intent intent = new Intent(this, ResetPasswordActivity.class);
            intent.putExtra("email", email);
            startActivity(intent);
        });

        tvBackToSignIn.setOnClickListener(v -> finish());
    }
}
