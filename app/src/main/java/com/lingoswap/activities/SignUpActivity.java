package com.lingoswap.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.lingoswap.utils.GoogleSignInHelper;
import com.lingoswap.utils.Resource;

import com.lingoswap.R;
import com.lingoswap.databinding.ActivitySignUpBinding;
import com.lingoswap.presentation.auth.signup.SignUpViewModel;
import com.lingoswap.presentation.base.BaseActivity;
import com.lingoswap.utils.HeartbeatManager;
import com.lingoswap.utils.SocketManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SignUpActivity extends BaseActivity<ActivitySignUpBinding> {

    @Inject SocketManager socketManager;
    @Inject HeartbeatManager heartbeatManager;

    private boolean pwVisible = false;
    private boolean pwConfirmVisible = false;
    private SignUpViewModel viewModel;
    private GoogleSignInHelper googleHelper;

    @Override
    protected ActivitySignUpBinding inflateBinding(LayoutInflater inflater) {
        return ActivitySignUpBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(SignUpViewModel.class);
        googleHelper = new GoogleSignInHelper(this);

        setupObservers();

        binding.ivTogglePassword.setOnClickListener(v -> {
            pwVisible = !pwVisible;
            binding.etPassword.setInputType(pwVisible
                    ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            binding.etPassword.setSelection(binding.etPassword.getText().length());
            binding.ivTogglePassword.setImageResource(pwVisible
                    ? android.R.drawable.ic_menu_view
                    : android.R.drawable.ic_secure);
        });

        binding.ivToggleConfirmPassword.setOnClickListener(v -> {
            pwConfirmVisible = !pwConfirmVisible;
            binding.etConfirmPassword.setInputType(pwConfirmVisible
                    ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            binding.etConfirmPassword.setSelection(binding.etConfirmPassword.getText().length());
            binding.ivToggleConfirmPassword.setImageResource(pwConfirmVisible
                    ? android.R.drawable.ic_menu_view
                    : android.R.drawable.ic_secure);
        });

        binding.btnCreateAccount.setOnClickListener(v -> {
            String name      = binding.etFullName.getText().toString().trim();
            String email     = binding.etEmail.getText().toString().trim();
            String pw        = binding.etPassword.getText().toString();
            String pwConfirm = binding.etConfirmPassword.getText().toString();

            if (validateInput(name, email, pw, pwConfirm)) {
                // Default country "vi" for now
                viewModel.register(email, pw, pwConfirm, name, "vi");
            }
        });

        binding.btnGoogleSignUp.setOnClickListener(v ->
                startActivityForResult(
                        googleHelper.getSignInIntent(),
                        GoogleSignInHelper.RC_GOOGLE_SIGN_IN));

        binding.tvSignIn.setOnClickListener(v -> finish());
    }

    private void setupObservers() {
        viewModel.getRegisterResult().observe(this, resource -> {
            if (resource == null) return;
            switch (resource.getStatus()) {
                case LOADING:
                    setLoading(true);
                    break;
                case SUCCESS:
                    setLoading(false);
                    Toast.makeText(this, R.string.toast_profile_saved, Toast.LENGTH_SHORT).show();
                    socketManager.connect();
                    heartbeatManager.start();
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                    break;
                case ERROR:
                    setLoading(false);
                    Toast.makeText(this, resource.getMessage(), Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        viewModel.getGoogleLoginResult().observe(this, resource -> {
            if (resource == null) return;
            switch (resource.getStatus()) {
                case LOADING:
                    setLoading(true);
                    break;
                case SUCCESS:
                    setLoading(false);
                    Toast.makeText(this, R.string.toast_profile_saved, Toast.LENGTH_SHORT).show();
                    socketManager.reconnectWithNewToken();
                    heartbeatManager.start();
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                    break;
                case ERROR:
                    setLoading(false);
                    Toast.makeText(this, resource.getMessage(), Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.btnCreateAccount.setEnabled(!loading);
        binding.btnGoogleSignUp.setEnabled(!loading);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GoogleSignInHelper.RC_GOOGLE_SIGN_IN) {
            googleHelper.handleResult(data, new GoogleSignInHelper.Callback() {
                @Override
                public void onSuccess(String idToken) {
                    Log.d("SignUpActivity", "ID Token nhận được từ Google → gửi lên backend");
                    viewModel.googleLogin(idToken);
                }
                @Override
                public void onError(String message) {
                    Toast.makeText(SignUpActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private boolean validateInput(String name, String email, String pw, String pwConfirm) {
        if (TextUtils.isEmpty(name)) {
            binding.etFullName.setError(getString(R.string.full_name) + " empty");
            binding.etFullName.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError(getString(R.string.email) + " empty");
            binding.etEmail.requestFocus();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Email invalid");
            binding.etEmail.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(pw)) {
            binding.etPassword.setError(getString(R.string.password) + " empty");
            binding.etPassword.requestFocus();
            return false;
        }
        if (pw.length() < 8) {
            binding.etPassword.setError("Min 8 chars");
            binding.etPassword.requestFocus();
            return false;
        }
        if (!pw.equals(pwConfirm)) {
            binding.etConfirmPassword.setError("Not match");
            binding.etConfirmPassword.requestFocus();
            return false;
        }
        return true;
    }

    @Override
    protected void observeViewModel() {
    }
}
