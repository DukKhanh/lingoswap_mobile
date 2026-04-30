package com.lingoswap.presentation.auth.signin

import android.content.Intent
import android.text.InputType
import android.widget.Toast
import androidx.activity.viewModels
import com.lingoswap.activities.HomeActivity
import com.lingoswap.data.model.request.LoginRequest
import com.lingoswap.databinding.ActivitySignInBinding
import com.lingoswap.presentation.base.BaseActivity
import com.lingoswap.presentation.auth.signup.SignUpActivity
import com.lingoswap.presentation.auth.forgotpassword.ForgotPasswordActivity
import com.lingoswap.utils.Resource
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignInActivity : BaseActivity<ActivitySignInBinding>(ActivitySignInBinding::inflate) {

    private val viewModel: SignInViewModel by viewModels()
    private var passwordVisible = false

    override fun setupViews() {
        binding.ivTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            binding.etPassword.inputType = if (passwordVisible)
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.etPassword.setSelection(binding.etPassword.text.length)
            binding.ivTogglePassword.setImageResource(if (passwordVisible)
                android.R.drawable.ic_menu_view
            else
                android.R.drawable.ic_secure)
        }

        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (email.isEmpty()) {
                binding.etEmail.error = "Email is required"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.etPassword.error = "Password is required"
                return@setOnClickListener
            }

            viewModel.login(LoginRequest(email, password))
        }

        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    override fun observeViewModel() {
        viewModel.loginState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.btnSignIn.isEnabled = false
                }
                is Resource.Success -> {
                    binding.btnSignIn.isEnabled = true
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
                is Resource.Error -> {
                    binding.btnSignIn.isEnabled = true
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
