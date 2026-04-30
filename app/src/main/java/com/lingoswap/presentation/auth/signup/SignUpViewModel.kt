package com.lingoswap.presentation.auth.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lingoswap.data.model.request.RegisterRequest
import com.lingoswap.data.model.response.AuthResponse
import com.lingoswap.domain.usecase.auth.RegisterUseCase
import com.lingoswap.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _registerState = MutableLiveData<Resource<AuthResponse>>()
    val registerState: LiveData<Resource<AuthResponse>> = _registerState

    fun register(request: RegisterRequest) {
        viewModelScope.launch {
            registerUseCase(request).onEach { result ->
                _registerState.value = result
            }.launchIn(viewModelScope)
        }
    }
}
