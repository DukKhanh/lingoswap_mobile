package com.lingoswap.data.api;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lingoswap.activities.SignInActivity;
import com.lingoswap.data.local.UserPreferences;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class TokenAuthenticator implements Authenticator {

    private final UserPreferences userPreferences;
    private final Provider<AuthApiService> authApiProvider; // Dùng Provider để tránh circular dependency
    private final Context context;

    @Inject
    public TokenAuthenticator(Context context, UserPreferences userPreferences, Provider<AuthApiService> authApiProvider) {
        this.context = context;
        this.userPreferences = userPreferences;
        this.authApiProvider = authApiProvider;
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) throws IOException {
        // 1. Chỉ thử refresh 1 lần
        if (responseCount(response) >= 2) {
            handleLogout();
            return null;
        }

        synchronized (this) {
            // FIX: Changed getToken() to getAccessToken() as defined in UserPreferences
            String currentToken = userPreferences.getAccessToken();
            
            // 2. Gọi API refresh token
            // Lưu ý: AuthApiService phải được cấu hình để không dùng Authenticator này (tránh loop)
            retrofit2.Response<Map<String, String>> refreshResponse = authApiProvider.get().refreshToken().execute();

            if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                String newToken = refreshResponse.body().get("token");
                if (newToken != null) {
                    // FIX: Changed saveToken() to saveAccessToken() as defined in UserPreferences
                    userPreferences.saveAccessToken(newToken);
                    
                    // 3. Retry request cũ với token mới
                    return response.request().newBuilder()
                            .header("Authorization", "Bearer " + newToken)
                            .build();
                }
            }
        }

        handleLogout();
        return null;
    }

    private int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }

    private void handleLogout() {
        userPreferences.clear();
        Intent intent = new Intent(context, SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
