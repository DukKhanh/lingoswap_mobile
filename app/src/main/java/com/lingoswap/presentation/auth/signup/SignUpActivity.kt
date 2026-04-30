package com.lingoswap.presentation.auth.signup

import android.content.Intent
import android.text.InputType
import android.widget.Toast
import androidx.activity.viewModels
import com.lingoswap.activities.HomeActivity
import com.lingoswap.data.model.request.RegisterRequest
import com.lingoswap.databinding.ActivitySignUpBinding
import com.lingoswap.presentation.base.BaseActivity
import com.lingoswap.utils.Resource
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpActivity : BaseActivity<ActivitySignUpBinding>(ActivitySignUpBinding::inflate) {

    private val viewModel: SignUpViewModel by viewModels()
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

        binding.btnCreateAccount.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (fullName.isEmpty()) {
                binding.etFullName.error = "Full name is required"
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                binding.etEmail.error = "Email is required"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.etPassword.error = "Password is required"
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                binding.etConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            viewModel.register(RegisterRequest(fullName, email, password))
        }

        binding.tvSignIn.setOnClickListener {
            finish()
        }
    }

    override fun observeViewModel() {
        viewModel.registerState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.btnCreateAccount.isEnabled = false
                }
                is Resource.Success -> {
                    binding.btnCreateAccount.isEnabled = true
                    Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomeActivity::class.java))
                    finishAffinity()
                }
                is Resource.Error -> {
                    binding.btnCreateAccount.isEnabled = true
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
