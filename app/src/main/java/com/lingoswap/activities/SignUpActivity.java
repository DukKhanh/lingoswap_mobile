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
    private SignUpViewModel viewModel;

    @Override
    protected ActivitySignUpBinding inflateBinding(LayoutInflater inflater) {
        return ActivitySignUpBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(SignUpViewModel.class);

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
            Toast.makeText(this, "Google Sign-Up — chưa tích hợp", Toast.LENGTH_SHORT).show()
        );

        binding.tvSignIn.setOnClickListener(v -> finish());
    }

    private void setupObservers() {
        viewModel.getRegisterResult().observe(this, resource -> {
            if (resource == null) return;
            switch (resource.getStatus()) {
                case LOADING:
                    // TODO: show progress bar
                    break;
                case SUCCESS:
                    Toast.makeText(this, R.string.toast_profile_saved, Toast.LENGTH_SHORT).show();
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
