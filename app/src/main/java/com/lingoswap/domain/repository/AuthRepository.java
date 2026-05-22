package com.lingoswap.domain.repository;

import com.lingoswap.data.model.ApiResponse;
import com.lingoswap.data.model.AuthResponse;
import com.lingoswap.data.model.ResetPasswordRequest;
import com.lingoswap.data.model.request.RegisterRequest;
import com.lingoswap.data.repository.RepositoryCallback;

public interface AuthRepository {
    void login(String email, String password, RepositoryCallback<AuthResponse> callback);
    void register(RegisterRequest request, RepositoryCallback<AuthResponse> callback);
    void logout(RepositoryCallback<Void> callback);
    void forgotPassword(String email, RepositoryCallback<ApiResponse> callback);
    void resetPassword(ResetPasswordRequest request, RepositoryCallback<ApiResponse> callback);
}
