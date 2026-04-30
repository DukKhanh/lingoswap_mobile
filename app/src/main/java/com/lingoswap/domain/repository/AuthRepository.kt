package com.lingoswap.domain.repository

import com.lingoswap.data.model.request.LoginRequest
import com.lingoswap.data.model.request.RegisterRequest
import com.lingoswap.data.model.response.AuthResponse
import com.lingoswap.utils.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(request: LoginRequest): Flow<Resource<AuthResponse>>
    suspend fun register(request: RegisterRequest): Flow<Resource<AuthResponse>>
    suspend fun forgotPassword(email: String): Flow<Resource<Unit>>
}
