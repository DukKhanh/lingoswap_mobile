package com.lingoswap.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.lingoswap.R;
import com.lingoswap.databinding.ActivitySignInBinding;
import com.lingoswap.presentation.auth.signin.SignInViewModel;
import com.lingoswap.presentation.base.BaseActivity;
import com.lingoswap.utils.HeartbeatManager;
import com.lingoswap.utils.SocketManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SignInActivity extends BaseActivity<ActivitySignInBinding> {

    @Inject SocketManager socketManager;
    @Inject HeartbeatManager heartbeatManager;

    private boolean passwordVisible = false;
    private SignInViewModel viewModel;

    @Override
    protected ActivitySignInBinding inflateBinding(LayoutInflater inflater) {
        return ActivitySignInBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(SignInViewModel.class);

        setupObservers();

        binding.ivTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            binding.etPassword.setInputType(passwordVisible
                    ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            binding.etPassword.setSelection(binding.etPassword.getText().length());
            binding.ivTogglePassword.setImageResource(passwordVisible
                    ? android.R.drawable.ic_menu_view
                    : android.R.drawable.ic_secure);
        });

        binding.btnSignIn.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String pw    = binding.etPassword.getText().toString();

            if (validateInput(email, pw)) {
                viewModel.login(email, pw);
            }
        });

        binding.btnGoogleSignIn.setOnClickListener(v ->
            Toast.makeText(this, "Google Sign-In — chưa tích hợp", Toast.LENGTH_SHORT).show()
        );

        binding.tvForgotPassword.setOnClickListener(v ->
            startActivity(new Intent(this, ForgotPasswordActivity.class))
        );

        binding.tvSignUp.setOnClickListener(v ->
            startActivity(new Intent(this, SignUpActivity.class))
        );
    }

    private void setupObservers() {
        viewModel.getLoginResult().observe(this, resource -> {
            if (resource == null) return;
            switch (resource.getStatus()) {
                case LOADING:
                    // TODO: show progress bar
                    break;
                case SUCCESS:
                    socketManager.connect();
                    heartbeatManager.start();
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                    break;
                case ERROR:
                    Toast.makeText(this, resource.getMessage(), Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }

    private boolean validateInput(String email, String pw) {
        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError(getString(R.string.sign_in_label_email) + " empty");
            binding.etEmail.requestFocus();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Email invalid");
            binding.etEmail.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(pw)) {
            binding.etPassword.setError(getString(R.string.sign_in_label_password) + " empty");
            binding.etPassword.requestFocus();
            return false;
        }
        if (pw.length() < 8) {
            binding.etPassword.setError("Min 8 chars");
            binding.etPassword.requestFocus();
            return false;
        }
        return true;
    }

    @Override
    protected void observeViewModel() {
    }
}
