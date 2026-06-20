package com.lingoswap.presentation.auth.signup;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lingoswap.data.model.AuthResponse;
import com.lingoswap.data.model.request.RegisterRequest;
import com.lingoswap.data.repository.AuthRepository;
import com.lingoswap.data.repository.RepositoryCallback;
import com.lingoswap.domain.usecase.auth.RegisterUseCase;
import com.lingoswap.utils.Resource;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SignUpViewModel extends ViewModel {
    private final RegisterUseCase registerUseCase;
    private final AuthRepository authRepository;
    
    private final MutableLiveData<Resource<AuthResponse>> registerResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<AuthResponse>> googleLoginResult = new MutableLiveData<>();

    @Inject
    public SignUpViewModel(RegisterUseCase registerUseCase, AuthRepository authRepository) {
        this.registerUseCase = registerUseCase;
        this.authRepository = authRepository;
    }

    public LiveData<Resource<AuthResponse>> getRegisterResult() { return registerResult; }
    public LiveData<Resource<AuthResponse>> getGoogleLoginResult() { return googleLoginResult; }

    public void register(String email, String password, String confirmPassword, String fullName, String country) {
        registerResult.setValue(Resource.loading());
        RegisterRequest request = new RegisterRequest(email, password, confirmPassword, fullName, country);
        registerUseCase.execute(request, new RepositoryCallback<AuthResponse>() {
            @Override
            public void onSuccess(AuthResponse data) {
                registerResult.postValue(Resource.success(data));
            }

            @Override
            public void onError(String message) {
                registerResult.postValue(Resource.error(message));
            }
        });
    }

    public void googleLogin(String idToken) {
        googleLoginResult.setValue(Resource.loading());
        authRepository.googleLogin(idToken, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(AuthResponse response) {
                googleLoginResult.postValue(Resource.success(response));
            }
            @Override
            public void onError(String message) {
                googleLoginResult.postValue(Resource.error(message));
            }
        });
    }
}
