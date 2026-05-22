package com.lingoswap.presentation.auth.signin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lingoswap.data.model.AuthResponse;
import com.lingoswap.data.repository.RepositoryCallback;
import com.lingoswap.domain.usecase.auth.LoginUseCase;
import com.lingoswap.utils.Resource;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SignInViewModel extends ViewModel {
    private final LoginUseCase loginUseCase;
    private final MutableLiveData<Resource<AuthResponse>> loginResult = new MutableLiveData<>();

    @Inject
    public SignInViewModel(LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
    }

    public LiveData<Resource<AuthResponse>> getLoginResult() { return loginResult; }

    public void login(String email, String password) {
        loginResult.setValue(Resource.loading());
        loginUseCase.execute(email, password, new RepositoryCallback<AuthResponse>() {
            @Override
            public void onSuccess(AuthResponse data) {
                loginResult.postValue(Resource.success(data));
            }

            @Override
            public void onError(String message) {
                loginResult.postValue(Resource.error(message));
            }
        });
    }
}
