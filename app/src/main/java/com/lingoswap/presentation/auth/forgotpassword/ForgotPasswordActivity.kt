package com.lingoswap.presentation.auth.forgotpassword

import android.content.Intent
import android.widget.Toast
import androidx.activity.viewModels
import com.lingoswap.activities.ResetPasswordActivity
import com.lingoswap.databinding.ActivityForgotPasswordBinding
import com.lingoswap.presentation.base.BaseActivity
import com.lingoswap.utils.Resource
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPasswordActivity : BaseActivity<ActivityForgotPasswordBinding>(ActivityForgotPasswordBinding::inflate) {

    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun setupViews() {
        binding.btnSendOtp.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                binding.etEmail.error = "Email is required"
                return@setOnClickListener
            }
            viewModel.forgotPassword(email)
        }

        binding.tvBackToSignIn.setOnClickListener {
            finish()
        }
    }

    override fun observeViewModel() {
        viewModel.forgotPasswordState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.btnSendOtp.isEnabled = false
                }
                is Resource.Success -> {
                    binding.btnSendOtp.isEnabled = true
                    Toast.makeText(this, "OTP sent to your email", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, ResetPasswordActivity::class.java)
                    intent.putExtra("email", binding.etEmail.text.toString().trim())
                    startActivity(intent)
                }
                is Resource.Error -> {
                    binding.btnSendOtp.isEnabled = true
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
