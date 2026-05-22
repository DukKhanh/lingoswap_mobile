package com.lingoswap.data.repository;

import android.util.Log;

import com.lingoswap.data.api.AuthApiService;
import com.lingoswap.data.local.UserPreferences;
import com.lingoswap.data.model.ApiResponse;
import com.lingoswap.data.model.AuthResponse;
import com.lingoswap.data.model.ForgotPasswordRequest;
import com.lingoswap.data.model.ResetPasswordRequest;
import com.lingoswap.data.model.request.LoginRequest;
import com.lingoswap.data.model.request.RegisterRequest;
import com.lingoswap.utils.Resource;
import com.lingoswap.utils.SocketManager;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * AuthRepository — cầu nối giữa ViewModel và API/Socket.
 *
 * Sau login/register thành công:
 *   1. Lưu token + user vào UserPreferences
 *   2. Kết nối Socket.IO với token mới
 */
@Singleton
public class AuthRepository {

    private static final String TAG = "AuthRepository";

    private final AuthApiService apiService;
    private final UserPreferences userPreferences;
    private final SocketManager socketManager;

    @Inject
    public AuthRepository(
            AuthApiService apiService,
            UserPreferences userPreferences,
            SocketManager socketManager
    ) {
        this.apiService      = apiService;
        this.userPreferences = userPreferences;
        this.socketManager   = socketManager;
    }

    // ── Login ─────────────────────────────────────────────────────────

    public void login(String email, String password, AuthCallback callback) {
        LoginRequest request = new LoginRequest(email, password);
        apiService.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse auth = response.body();
                    userPreferences.saveAuthResponse(auth);
                    socketManager.connect(); // ✅ connect socket sau khi có token
                    callback.onSuccess(auth);
                } else {
                    String msg = parseError(response);
                    Log.w(TAG, "Login thất bại: " + msg);
                    callback.onError(msg);
                }
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Log.e(TAG, "Login network error: " + t.getMessage());
                callback.onError("Lỗi mạng: " + t.getMessage());
            }
        });
    }

    // ── Register ──────────────────────────────────────────────────────

    public void register(RegisterRequest request, AuthCallback callback) {
        apiService.register(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse auth = response.body();
                    userPreferences.saveAuthResponse(auth);
                    socketManager.connect();
                    callback.onSuccess(auth);
                } else {
                    callback.onError(parseError(response));
                }
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                callback.onError("Lỗi mạng: " + t.getMessage());
            }
        });
    }

    // ── Google Login ──────────────────────────────────────────────────

    public void googleLogin(String idToken, AuthCallback callback) {
        Map<String, String> body = new HashMap<>();
        body.put("idToken", idToken);
        apiService.googleLogin(body).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse auth = response.body();
                    userPreferences.saveAuthResponse(auth);
                    socketManager.connect();
                    callback.onSuccess(auth);
                } else {
                    callback.onError(parseError(response));
                }
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                callback.onError("Lỗi mạng: " + t.getMessage());
            }
        });
    }

    // ── Logout ────────────────────────────────────────────────────────

    public void logout(SimpleCallback callback) {
        apiService.logout().enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                // Dù server có lỗi cũng phải clear local
                userPreferences.clear();
                socketManager.disconnect();
                callback.onComplete();
            }
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                // Vẫn clear local ngay cả khi mất mạng
                userPreferences.clear();
                socketManager.disconnect();
                callback.onComplete();
            }
        });
    }

    // ── Forgot Password ───────────────────────────────────────────────

    public void forgotPassword(String email, SimpleCallback callback) {
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);
        apiService.forgotPassword(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful()) callback.onComplete();
                else callback.onError(parseError(response));
            }
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                callback.onError("Lỗi mạng: " + t.getMessage());
            }
        });
    }

    // ── Reset Password ────────────────────────────────────────────────

    public void resetPassword(String email, String otp, String newPassword, SimpleCallback callback) {
        ResetPasswordRequest request = new ResetPasswordRequest(email, otp, newPassword);
        apiService.resetPassword(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful()) callback.onComplete();
                else callback.onError(parseError(response));
            }
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                callback.onError("Lỗi mạng: " + t.getMessage());
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private String parseError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                // Parse {"error": "message"} từ backend
                String raw = response.errorBody().string();
                org.json.JSONObject json = new org.json.JSONObject(raw);
                return json.optString("error", "Lỗi không xác định");
            }
        } catch (Exception ignored) {}
        return "Lỗi " + response.code();
    }

    // ── Callback interfaces ───────────────────────────────────────────

    public interface AuthCallback {
        void onSuccess(AuthResponse response);
        void onError(String message);
    }

    public interface SimpleCallback {
        default void onComplete() {}
        default void onError(String message) {}
    }
}
