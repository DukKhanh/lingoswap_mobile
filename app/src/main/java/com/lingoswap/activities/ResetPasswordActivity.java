package com.lingoswap.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.lingoswap.R;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etOtp, etNewPassword, etConfirmNewPw;
    private TextView tvResendIn, tvResendCode;
    private CountDownTimer countDownTimer;
    private String email;
    private static final long OTP_TIMEOUT_MS = 5 * 60 * 1000; // 5 phút

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        email = getIntent().getStringExtra("email");

        etOtp         = findViewById(R.id.etOtp);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmNewPw = findViewById(R.id.etConfirmNewPw);
        tvResendIn    = findViewById(R.id.tvResendIn);
        tvResendCode  = findViewById(R.id.tvResendCode);

        Button btnResetPassword = findViewById(R.id.btnResetPassword);
        TextView tvBackToSignIn = findViewById(R.id.tvBackToSignIn);

        // ── Khởi động đếm ngược ────────────────────────────────────
        startCountDown();

        // ── Gửi lại OTP ────────────────────────────────────────────
        tvResendCode.setOnClickListener(v -> {
            if (tvResendCode.isEnabled()) {
                // TODO: gọi API gửi lại OTP cho email
                Toast.makeText(this, "Đã gửi lại OTP tới " + email, Toast.LENGTH_SHORT).show();
                startCountDown(); // reset bộ đếm
            }
        });

        // ── Reset mật khẩu ─────────────────────────────────────────
        btnResetPassword.setOnClickListener(v -> {
            String otp     = etOtp.getText().toString().trim();
            String newPw   = etNewPassword.getText().toString();
            String confirm = etConfirmNewPw.getText().toString();

            if (TextUtils.isEmpty(otp) || otp.length() < 4) {
                etOtp.setError("Mã OTP không hợp lệ");
                etOtp.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(newPw)) {
                etNewPassword.setError("Mật khẩu không được để trống");
                etNewPassword.requestFocus();
                return;
            }
            if (newPw.length() < 8) {
                etNewPassword.setError("Mật khẩu phải ít nhất 8 ký tự");
                etNewPassword.requestFocus();
                return;
            }
            if (!newPw.equals(confirm)) {
                etConfirmNewPw.setError("Mật khẩu xác nhận không khớp");
                etConfirmNewPw.requestFocus();
                return;
            }

            // TODO: gọi API xác thực OTP + đặt lại mật khẩu

            if (countDownTimer != null) countDownTimer.cancel();
            startActivity(new Intent(this, ResetSuccessActivity.class));
            finish();
        });

        tvBackToSignIn.setOnClickListener(v -> {
            if (countDownTimer != null) countDownTimer.cancel();
            // Quay về SignIn (xóa toàn bộ stack)
            Intent intent = new Intent(this, SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    /** Bắt đầu (hoặc reset) bộ đếm ngược 5 phút */
    private void startCountDown() {
        if (countDownTimer != null) countDownTimer.cancel();

        // Vô hiệu hoá nút "Resend" trong khi đếm
        tvResendCode.setEnabled(false);
        tvResendCode.setAlpha(0.4f);

        countDownTimer = new CountDownTimer(OTP_TIMEOUT_MS, 1000) {
            @Override
            public void onTick(long millisLeft) {
                long mins = millisLeft / 60000;
                long secs = (millisLeft % 60000) / 1000;
                tvResendIn.setText(String.format("Resend in %02d:%02d", mins, secs));
            }

            @Override
            public void onFinish() {
                tvResendIn.setText("OTP đã hết hạn");
                tvResendCode.setEnabled(true);
                tvResendCode.setAlpha(1f);
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
