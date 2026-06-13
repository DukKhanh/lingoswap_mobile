package com.lingoswap.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.lingoswap.R;
import com.lingoswap.data.local.UserPreferences;
import com.lingoswap.databinding.ActivitySignInBinding;
import com.lingoswap.utils.GoogleSignInHelper;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * SignInActivity — màn hình đăng nhập.
 *
 * Thêm mới:
 *  - Nút "Đăng nhập bằng Google" (btnGoogleSignIn)
 *  - Giữ nguyên nút đổi ngôn ngữ / dark toggle TẠI ĐÂY
 *    (màn đăng nhập được giữ lại theo yêu cầu)
 *
 * Cần thêm vào layout activity_sign_in.xml:
 *   <com.google.android.gms.common.SignInButton
 *       android:id="@+id/btnGoogleSignIn"
 *       android:layout_width="match_parent"
 *       android:layout_height="48dp"
 *       android:layout_marginTop="12dp"/>
 *   hoặc Button thông thường với text "Đăng nhập bằng Google"
 */
@AndroidEntryPoint
public class SignInActivity extends AppCompatActivity {

    private static final String TAG = "SignInActivity";

    private ActivitySignInBinding binding;
    private GoogleSignInHelper     googleHelper;

    @Inject UserPreferences userPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        googleHelper = new GoogleSignInHelper(this);

        setupNormalLogin();
        setupGoogleLogin();
        setupLinks();
    }

    // ─── Email / Password login ───────────────────────────────────────────────

    private void setupNormalLogin() {
        binding.btnSignIn.setOnClickListener(v -> {
            String email    = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString();

            if (TextUtils.isEmpty(email)) {
                binding.etEmail.setError("Vui lòng nhập email");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                binding.etPassword.setError("Vui lòng nhập mật khẩu");
                return;
            }

            // TODO: gọi AuthViewModel.login(email, password)
            // Tạm thời navigate thẳng để demo
            navigateHome();
        });
    }

    // ─── Google Sign-In ───────────────────────────────────────────────────────

    private void setupGoogleLogin() {
        // btnGoogleSignIn cần có trong layout activity_sign_in.xml
        if (binding.btnGoogleSignIn == null) return;

        binding.btnGoogleSignIn.setOnClickListener(v -> {
            Intent signInIntent = googleHelper.getSignInIntent();
            startActivityForResult(signInIntent, GoogleSignInHelper.RC_GOOGLE_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GoogleSignInHelper.RC_GOOGLE_SIGN_IN) {
            googleHelper.handleResult(data, new GoogleSignInHelper.Callback() {
                @Override
                public void onSuccess(String idToken) {
                    Log.d(TAG, "Google ID Token nhận được, gửi lên backend...");
                    // TODO: gọi AuthViewModel.googleSignIn(idToken)
                    //   → POST /api/auth/google { idToken }
                    //   → nhận accessToken + refreshToken
                    //   → lưu vào UserPreferences
                    //   → navigateHome()

                    // Demo: navigate thẳng
                    Toast.makeText(SignInActivity.this,
                            "Đăng nhập Google thành công (demo)", Toast.LENGTH_SHORT).show();
                    navigateHome();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(SignInActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    // ─── Links ────────────────────────────────────────────────────────────────

    private void setupLinks() {
        if (binding.tvForgotPassword != null) {
            binding.tvForgotPassword.setOnClickListener(v ->
                    startActivity(new Intent(this, ForgotPasswordActivity.class)));
        }
        if (binding.tvSignUp != null) {
            binding.tvSignUp.setOnClickListener(v ->
                    startActivity(new Intent(this, SignUpActivity.class)));
        }
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    private void navigateHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
