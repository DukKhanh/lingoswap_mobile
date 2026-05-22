package com.lingoswap.presentation.auth.forgotpassword;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lingoswap.data.model.ApiResponse;
import com.lingoswap.data.repository.RepositoryCallback;
import com.lingoswap.domain.usecase.auth.ForgotPasswordUseCase;
import com.lingoswap.utils.Resource;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ForgotPasswordViewModel extends ViewModel {
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final MutableLiveData<Resource<ApiResponse>> forgotPasswordResult = new MutableLiveData<>();

    @Inject
    public ForgotPasswordViewModel(ForgotPasswordUseCase forgotPasswordUseCase) {
        this.forgotPasswordUseCase = forgotPasswordUseCase;
    }

    public LiveData<Resource<ApiResponse>> getForgotPasswordResult() {
        return forgotPasswordResult;
    }

    public void forgotPassword(String email) {
        forgotPasswordResult.setValue(Resource.loading());
        forgotPasswordUseCase.execute(email, new RepositoryCallback<ApiResponse>() {
            @Override
            public void onSuccess(ApiResponse data) {
                forgotPasswordResult.postValue(Resource.success(data));
            }

            @Override
            public void onError(String message) {
                forgotPasswordResult.postValue(Resource.error(message));
            }
        });
    }
}
