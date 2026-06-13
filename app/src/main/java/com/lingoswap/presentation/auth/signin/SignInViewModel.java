package com.lingoswap.presentation.auth.signin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lingoswap.data.model.AuthResponse;
import com.lingoswap.data.repository.AuthRepository;
import com.lingoswap.data.repository.RepositoryCallback;
import com.lingoswap.domain.usecase.auth.LoginUseCase;
import com.lingoswap.utils.Resource;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SignInViewModel extends ViewModel {

    private final LoginUseCase   loginUseCase;
    private final AuthRepository authRepository;

    private final MutableLiveData<Resource<AuthResponse>> loginResult       = new MutableLiveData<>();
    private final MutableLiveData<Resource<AuthResponse>> googleLoginResult = new MutableLiveData<>();

    @Inject
    public SignInViewModel(LoginUseCase loginUseCase, AuthRepository authRepository) {
        this.loginUseCase   = loginUseCase;
        this.authRepository = authRepository;
    }

    public LiveData<Resource<AuthResponse>> getLoginResult()       { return loginResult; }
    public LiveData<Resource<AuthResponse>> getGoogleLoginResult() { return googleLoginResult; }

    /** Đăng nhập email / password */
    public void login(String email, String password) {
        loginResult.setValue(Resource.loading());
        loginUseCase.execute(email, password, new RepositoryCallback<>() {
            @Override public void onSuccess(AuthResponse data) {
                loginResult.postValue(Resource.success(data));
            }
            @Override public void onError(String message) {
                loginResult.postValue(Resource.error(message));
            }
        });
    }

    /** Đăng nhập Google — gửi idToken lên POST /api/auth/google */
    public void googleLogin(String idToken) {
        googleLoginResult.setValue(Resource.loading());
        authRepository.googleLogin(idToken, new AuthRepository.AuthCallback() {
            @Override public void onSuccess(AuthResponse response) {
                googleLoginResult.postValue(Resource.success(response));
            }
            @Override public void onError(String message) {
                googleLoginResult.postValue(Resource.error(message));
            }
        });
    }
}
