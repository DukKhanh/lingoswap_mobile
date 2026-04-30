package com.lingoswap.data.model.response

data class AuthResponse(
    val token: String,
    val type: String = "Bearer",
    val id: Long,
    val email: String,
    val fullName: String
)
