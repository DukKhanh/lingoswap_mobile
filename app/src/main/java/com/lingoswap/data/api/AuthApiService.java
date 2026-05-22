package com.lingoswap.data.api;

import com.lingoswap.data.model.ApiResponse;
import com.lingoswap.data.model.AuthResponse;
import com.lingoswap.data.model.ForgotPasswordRequest;
import com.lingoswap.data.model.request.LoginRequest;
import com.lingoswap.data.model.request.RegisterRequest;
import com.lingoswap.data.model.ResetPasswordRequest;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.PATCH;
import retrofit2.http.POST;

public interface AuthApiService {
    @POST("api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("api/auth/google")
    Call<AuthResponse> googleLogin(@Body Map<String, String> body);

    @POST("api/auth/logout")
    Call<ApiResponse> logout();

    @POST("api/auth/token")
    Call<Map<String, String>> refreshToken();

    @PATCH("api/auth/password/change")
    Call<ApiResponse> changePassword(@Body Map<String, String> body);

    @POST("api/auth/password/forgot")
    Call<ApiResponse> forgotPassword(@Body ForgotPasswordRequest request);

    @POST("api/auth/password/reset")
    Call<ApiResponse> resetPassword(@Body ResetPasswordRequest request);
}
