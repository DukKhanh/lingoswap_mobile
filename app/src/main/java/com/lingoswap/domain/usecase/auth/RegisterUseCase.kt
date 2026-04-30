package com.lingoswap.domain.usecase.auth

import com.lingoswap.data.model.request.RegisterRequest
import com.lingoswap.data.model.response.AuthResponse
import com.lingoswap.domain.repository.AuthRepository
import com.lingoswap.utils.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(request: RegisterRequest): Flow<Resource<AuthResponse>> {
        return repository.register(request)
    }
}
