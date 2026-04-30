package com.lingoswap.domain.usecase.auth

import com.lingoswap.domain.repository.AuthRepository
import com.lingoswap.utils.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ForgotPasswordUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String): Flow<Resource<Unit>> {
        return repository.forgotPassword(email)
    }
}
