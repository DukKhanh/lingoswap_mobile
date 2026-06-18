package com.lingoswap.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.lingoswap.R;
import com.lingoswap.data.repository.AuthRepository;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * ForgotPasswordActivity — gửi OTP THẬT qua POST /api/auth/password/forgot.
 */
@AndroidEntryPoint
public class ForgotPasswordActivity extends AppCompatActivity {

    @Inject AuthRepository authRepository;

    private EditText etEmail;
    private Button   btnSendOtp;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmail = findViewById(R.id.etEmail);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        TextView tvBackToSignIn = findViewById(R.id.tvBackToSignIn);

        btnSendOtp.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

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
            sendOtp(email);
        });

        tvBackToSignIn.setOnClickListener(v -> finish());
    }

    private void sendOtp(String email) {
        btnSendOtp.setEnabled(false);
        authRepository.forgotPassword(email, new AuthRepository.SimpleCallback() {
            @Override public void onComplete() {
                btnSendOtp.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this,
                        "Email đã được gửi", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(ForgotPasswordActivity.this, ResetPasswordActivity.class);
                intent.putExtra("email", email);
                startActivity(intent);
            }
            @Override public void onError(String message) {
                btnSendOtp.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
