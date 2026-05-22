package com.lingoswap.domain.usecase.auth;

import com.lingoswap.data.model.AuthResponse;
import com.lingoswap.data.model.request.RegisterRequest;
import com.lingoswap.data.repository.RepositoryCallback;
import com.lingoswap.domain.repository.AuthRepository;

import javax.inject.Inject;

public class RegisterUseCase {
    private final AuthRepository repository;

    @Inject
    public RegisterUseCase(AuthRepository repository) {
        this.repository = repository;
    }

    public void execute(RegisterRequest request, RepositoryCallback<AuthResponse> callback) {
        repository.register(request, callback);
    }
}
