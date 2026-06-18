package com.lingoswap.activities;

import android.content.Intent;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.Toast;

import com.lingoswap.data.repository.AuthRepository;
import com.lingoswap.databinding.ActivityResetPasswordBinding;
import com.lingoswap.presentation.base.BaseActivity;

import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ResetPasswordActivity extends BaseActivity<ActivityResetPasswordBinding> {

    @Inject AuthRepository authRepository;

    private CountDownTimer countDownTimer;
    private String email;
    private static final long OTP_TIMEOUT_MS = 5 * 60 * 1000;

    @Override
    protected ActivityResetPasswordBinding inflateBinding(LayoutInflater inflater) {
        return ActivityResetPasswordBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        email = getIntent().getStringExtra("email");

        startCountDown();

        binding.tvResendCode.setOnClickListener(v -> {
            if (binding.tvResendCode.isEnabled()) {
                resendOtp();
            }
        });

        binding.btnResetPassword.setOnClickListener(v -> {
            String otp = binding.etOtp.getText().toString().trim();
            String newPw = binding.etNewPassword.getText().toString();
            String confirm = binding.etConfirmNewPw.getText().toString();

            if (TextUtils.isEmpty(otp) || otp.length() < 4) {
                binding.etOtp.setError("Invalid OTP");
                binding.etOtp.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(newPw)) {
                binding.etNewPassword.setError("Password required");
                binding.etNewPassword.requestFocus();
                return;
            }
            if (newPw.length() < 8) {
                binding.etNewPassword.setError("Min 8 characters");
                binding.etNewPassword.requestFocus();
                return;
            }
            if (!newPw.equals(confirm)) {
                binding.etConfirmNewPw.setError("Passwords do not match");
                binding.etConfirmNewPw.requestFocus();
                return;
            }

            resetPassword(otp, newPw);
        });

        binding.tvBackToSignIn.setOnClickListener(v -> {
            if (countDownTimer != null) countDownTimer.cancel();
            Intent intent = new Intent(this, SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void observeViewModel() {
    }

    private void resendOtp() {
        binding.tvResendCode.setEnabled(false);
        authRepository.forgotPassword(email, new AuthRepository.SimpleCallback() {
            @Override public void onComplete() {
                Toast.makeText(ResetPasswordActivity.this, "Email đã được gửi", Toast.LENGTH_SHORT).show();
                startCountDown();
            }
            @Override public void onError(String message) {
                binding.tvResendCode.setEnabled(true);
                Toast.makeText(ResetPasswordActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resetPassword(String otp, String newPw) {
        binding.btnResetPassword.setEnabled(false);
        authRepository.resetPassword(email, otp, newPw, new AuthRepository.SimpleCallback() {
            @Override public void onComplete() {
                Toast.makeText(ResetPasswordActivity.this,
                        "Đặt lại mật khẩu thành công", Toast.LENGTH_SHORT).show();
                if (countDownTimer != null) countDownTimer.cancel();
                startActivity(new Intent(ResetPasswordActivity.this, ResetSuccessActivity.class));
                finish();
            }
            @Override public void onError(String message) {
                binding.btnResetPassword.setEnabled(true);
                Toast.makeText(ResetPasswordActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startCountDown() {
        if (countDownTimer != null) countDownTimer.cancel();

        binding.tvResendCode.setEnabled(false);
        binding.tvResendCode.setAlpha(0.4f);

        countDownTimer = new CountDownTimer(OTP_TIMEOUT_MS, 1000) {
            @Override
            public void onTick(long millisLeft) {
                long mins = millisLeft / 60000;
                long secs = (millisLeft % 60000) / 1000;
                binding.tvResendIn.setText(String.format(Locale.getDefault(), "Resend in %02d:%02d", mins, secs));
            }

            @Override
            public void onFinish() {
                binding.tvResendIn.setText("OTP expired");
                binding.tvResendCode.setEnabled(true);
                binding.tvResendCode.setAlpha(1f);
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
