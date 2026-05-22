package com.lingoswap.domain.usecase.auth;

import com.lingoswap.data.model.ApiResponse;
import com.lingoswap.data.model.ForgotPasswordRequest;
import com.lingoswap.data.repository.RepositoryCallback;
import com.lingoswap.domain.repository.AuthRepository;

import javax.inject.Inject;

public class ForgotPasswordUseCase {
    private final AuthRepository repository;

    @Inject
    public ForgotPasswordUseCase(AuthRepository repository) {
        this.repository = repository;
    }

    public void execute(String email, RepositoryCallback<ApiResponse> callback) {
        // AuthRepository interface needs forgotPassword method
    }
}
