package com.lingoswap.presentation.auth.forgotpassword

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lingoswap.domain.usecase.auth.ForgotPasswordUseCase
import com.lingoswap.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val forgotPasswordUseCase: ForgotPasswordUseCase
) : ViewModel() {

    private val _forgotPasswordState = MutableLiveData<Resource<Unit>>()
    val forgotPasswordState: LiveData<Resource<Unit>> = _forgotPasswordState

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            forgotPasswordUseCase(email).onEach { result ->
                _forgotPasswordState.value = result
            }.launchIn(viewModelScope)
        }
    }
}
