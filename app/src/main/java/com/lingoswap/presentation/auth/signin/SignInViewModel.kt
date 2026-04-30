package com.lingoswap.presentation.auth.signin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lingoswap.data.model.request.LoginRequest
import com.lingoswap.data.model.response.AuthResponse
import com.lingoswap.domain.usecase.auth.LoginUseCase
import com.lingoswap.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _loginState = MutableLiveData<Resource<AuthResponse>>()
    val loginState: LiveData<Resource<AuthResponse>> = _loginState

    fun login(request: LoginRequest) {
        viewModelScope.launch {
            loginUseCase(request).onEach { result ->
                _loginState.value = result
            }.launchIn(viewModelScope)
        }
    }
}
