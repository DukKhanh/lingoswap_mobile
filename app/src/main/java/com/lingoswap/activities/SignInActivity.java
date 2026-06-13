package com.lingoswap.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.lingoswap.data.local.UserPreferences;
import com.lingoswap.databinding.ActivitySignInBinding;
import com.lingoswap.presentation.auth.signin.SignInViewModel;
import com.lingoswap.presentation.base.BaseActivity;
import com.lingoswap.utils.GoogleSignInHelper;
import com.lingoswap.utils.Resource;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SignInActivity extends BaseActivity<ActivitySignInBinding> {

    private static final String TAG = "SignInActivity";

    private SignInViewModel   viewModel;
    private GoogleSignInHelper googleHelper;

    @Inject UserPreferences userPreferences;

    @Override
    protected ActivitySignInBinding inflateBinding(android.view.LayoutInflater inflater) {
        return ActivitySignInBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        viewModel    = new ViewModelProvider(this).get(SignInViewModel.class);
        googleHelper = new GoogleSignInHelper(this);

        setupNormalLogin();
        setupGoogleLogin();
        setupLinks();
    }

    @Override
    protected void observeViewModel() {
        // ── Email / Password ──────────────────────────────────────────
        viewModel.getLoginResult().observe(this, result -> {
            if (result == null) return;
            switch (result.getStatus()) {
                case LOADING:  setLoading(true);  break;
                case SUCCESS:  setLoading(false); navigateHome(); break;
                case ERROR:
                    setLoading(false);
                    Toast.makeText(this, result.getMessage(), Toast.LENGTH_LONG).show();
                    break;
            }
        });

        // ── Google Login ──────────────────────────────────────────────
        viewModel.getGoogleLoginResult().observe(this, result -> {
            if (result == null) return;
            switch (result.getStatus()) {
                case LOADING:  setLoading(true);  break;
                case SUCCESS:
                    setLoading(false);
                    Log.d(TAG, "✅ Google login thành công | email="
                            + (result.getData() != null ? result.getData().getEmail() : "?"));
                    navigateHome();
                    break;
                case ERROR:
                    setLoading(false);
                    Toast.makeText(this, result.getMessage(), Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }

    // ─── Email / Password ─────────────────────────────────────────────────────

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
            viewModel.login(email, password);
        });
    }

    // ─── Google Sign-In ───────────────────────────────────────────────────────

    private void setupGoogleLogin() {
        if (binding.btnGoogleSignIn == null) return;
        binding.btnGoogleSignIn.setOnClickListener(v ->
                startActivityForResult(
                        googleHelper.getSignInIntent(),
                        GoogleSignInHelper.RC_GOOGLE_SIGN_IN));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GoogleSignInHelper.RC_GOOGLE_SIGN_IN) {
            googleHelper.handleResult(data, new GoogleSignInHelper.Callback() {
                @Override
                public void onSuccess(String idToken) {
                    Log.d(TAG, "ID Token nhận được → gửi lên backend");
                    viewModel.googleLogin(idToken); // ← gọi API thật
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

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        binding.btnSignIn.setEnabled(!loading);
        if (binding.btnGoogleSignIn != null) {
            binding.btnGoogleSignIn.setEnabled(!loading);
        }
    }

    private void navigateHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
