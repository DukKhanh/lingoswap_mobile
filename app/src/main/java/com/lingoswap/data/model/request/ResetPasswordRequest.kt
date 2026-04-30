package com.lingoswap.data.model.request

data class ResetPasswordRequest(
    val email: String,
    val otp: String,
    val newPassword: String
)
