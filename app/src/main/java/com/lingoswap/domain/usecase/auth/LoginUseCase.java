package com.lingoswap.domain.usecase.auth;

import com.lingoswap.data.model.AuthResponse;
import com.lingoswap.data.repository.RepositoryCallback;
import com.lingoswap.domain.repository.AuthRepository;

import javax.inject.Inject;

public class LoginUseCase {
    private final AuthRepository repository;

    @Inject
    public LoginUseCase(AuthRepository repository) {
        this.repository = repository;
    }

    public void execute(String email, String password, RepositoryCallback<AuthResponse> callback) {
        repository.login(email, password, callback);
    }
}
