package com.lingoswap.domain.usecase.auth

import com.lingoswap.data.model.request.LoginRequest
import com.lingoswap.data.model.response.AuthResponse
import com.lingoswap.domain.repository.AuthRepository
import com.lingoswap.utils.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(request: LoginRequest): Flow<Resource<AuthResponse>> {
        return repository.login(request)
    }
}
