package com.lingoswap.data.repository

import com.lingoswap.data.api.AuthApiService
import com.lingoswap.data.local.UserPreferences
import com.lingoswap.data.model.request.LoginRequest
import com.lingoswap.data.model.request.RegisterRequest
import com.lingoswap.data.model.response.AuthResponse
import com.lingoswap.domain.repository.AuthRepository
import com.lingoswap.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val apiService: AuthApiService,
    private val userPreferences: UserPreferences
) : AuthRepository {

    override suspend fun login(request: LoginRequest): Flow<Resource<AuthResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.login(request)
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                userPreferences.saveAuthToken(authResponse.token)
                emit(Resource.Success(authResponse))
            } else {
                emit(Resource.Error(response.message() ?: "Unknown Error"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Network Error"))
        }
    }

    override suspend fun register(request: RegisterRequest): Flow<Resource<AuthResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.register(request)
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                userPreferences.saveAuthToken(authResponse.token)
                emit(Resource.Success(authResponse))
            } else {
                emit(Resource.Error(response.message() ?: "Unknown Error"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Network Error"))
        }
    }

    override suspend fun forgotPassword(email: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.forgotPassword(email)
            if (response.isSuccessful) {
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error(response.message() ?: "Unknown Error"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Network Error"))
        }
    }
}
