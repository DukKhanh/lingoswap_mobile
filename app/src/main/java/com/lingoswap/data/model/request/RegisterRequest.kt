package com.lingoswap.data.model.request

data class RegisterRequest(
    val fullName: String,
    val email: String,
    val password: String
)
