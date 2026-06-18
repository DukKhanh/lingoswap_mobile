package com.lingoswap.data.repository;

import com.lingoswap.data.api.AuthApiService;
import com.lingoswap.data.local.UserPreferences;
import com.lingoswap.data.model.ApiResponse;
import com.lingoswap.data.model.AuthResponse;
import com.lingoswap.data.model.ForgotPasswordRequest;
import com.lingoswap.data.model.ResetPasswordRequest;
import com.lingoswap.data.model.request.LoginRequest;
import com.lingoswap.data.model.request.RegisterRequest;
import com.lingoswap.domain.repository.AuthRepository;
import com.lingoswap.utils.ErrorUtils;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepositoryImpl implements AuthRepository {
    private final AuthApiService apiService;
    private final UserPreferences preferences;

    @Inject
    public AuthRepositoryImpl(AuthApiService apiService, UserPreferences preferences) {
        this.apiService  = apiService;
        this.preferences = preferences;
    }

    @Override
    public void login(String email, String password, RepositoryCallback<AuthResponse> callback) {
        apiService.login(new LoginRequest(email, password)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    preferences.saveAuthResponse(response.body());
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(ErrorUtils.parseError(response));
                }
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                callback.onError("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    @Override
    public void register(RegisterRequest request, RepositoryCallback<AuthResponse> callback) {
        apiService.register(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    preferences.saveAuthResponse(response.body());
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(ErrorUtils.parseError(response));
                }
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                callback.onError("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    @Override
    public void logout(RepositoryCallback<Void> callback) {
        apiService.logout().enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                preferences.clear();
                callback.onSuccess(null);
            }
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                preferences.clear();
                callback.onSuccess(null);
            }
        });
    }

    @Override
    public void changePassword(String currentPassword, String newPassword,
                               RepositoryCallback<ApiResponse> callback) {
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("currentPassword", currentPassword);
        body.put("newPassword", newPassword);
        apiService.changePassword(body).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful()) callback.onSuccess(response.body());
                else callback.onError(ErrorUtils.parseError(response));
            }
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                callback.onError("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    @Override
    public void forgotPassword(String email, RepositoryCallback<ApiResponse> callback) {
        apiService.forgotPassword(new ForgotPasswordRequest(email)).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful()) callback.onSuccess(response.body());
                else callback.onError(ErrorUtils.parseError(response));
            }
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                callback.onError("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    @Override
    public void resetPassword(ResetPasswordRequest request, RepositoryCallback<ApiResponse> callback) {
        apiService.resetPassword(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful()) callback.onSuccess(response.body());
                else callback.onError(ErrorUtils.parseError(response));
            }
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                callback.onError("Lỗi kết nối: " + t.getMessage());
            }
        });
    }
}
