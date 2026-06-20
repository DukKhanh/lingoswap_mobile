package com.lingoswap.data.api;

import android.util.Log;

import com.lingoswap.data.local.UserPreferences;

import org.json.JSONObject;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * AuthInterceptor — tự động:
 *  1. Đính kèm "Authorization: Bearer <token>" vào mọi request
 *  2. Khi nhận 401 → gọi POST /api/auth/refresh-token để lấy accessToken mới
 *     (refreshToken nằm trong cookie httpOnly, cần gửi kèm Set-Cookie)
 *  3. Retry request ban đầu với token mới
 */
@Singleton
public class AuthInterceptor implements Interceptor {

    private static final String TAG = "AuthInterceptor";
    private final UserPreferences userPreferences;

    // OkHttpClient riêng để gọi refresh — tránh vòng lặp vô tận
    private OkHttpClient refreshClient;

    @Inject
    public AuthInterceptor(UserPreferences userPreferences) {
        this.userPreferences = userPreferences;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        String token = userPreferences.getAccessToken();

        Request request = buildRequest(original, token);
        Response response = chain.proceed(request);

        // Nếu 401 → thử refresh token (single-flight: nhiều request 401 đồng thời
        // chỉ refresh 1 lần, các thread sau dùng lại token mới).
        if (response.code() == 401 && token != null) {
            response.close();

            String newToken;
            synchronized (this) {
                String latest = userPreferences.getAccessToken();
                if (latest != null && !latest.equals(token)) {
                    // Thread khác đã refresh xong → dùng token mới, không gọi lại API.
                    newToken = latest;
                } else {
                    Log.d(TAG, "Token hết hạn, đang refresh...");
                    newToken = refreshAccessToken();
                    if (newToken != null) userPreferences.saveAccessToken(newToken);
                }
            }

            if (newToken != null) {
                Log.d(TAG, "Refresh OK, retry request");
                return chain.proceed(buildRequest(original, newToken));
            }
            Log.w(TAG, "Refresh thất bại, clear session");
            userPreferences.clear();
        }

        return response;
    }

    private Request buildRequest(Request original, String token) {
        if (token == null || token.isEmpty()) return original;
        return original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();
    }

    /**
     * Gọi POST /api/auth/refresh-token
     * Backend đọc refreshToken từ httpOnly cookie.
     * Cần gửi kèm cookie đã lưu (nếu có).
     */
    private String refreshAccessToken() {
        try {
            if (refreshClient == null) {
                refreshClient = new OkHttpClient.Builder()
                        .cookieJar(PersistentCookieJar.getInstance())
                        .build();
            }

            String baseUrl = com.lingoswap.di.NetworkModule.BASE_URL;
            okhttp3.Request refreshRequest = new okhttp3.Request.Builder()
                    .url(baseUrl + "api/auth/refresh-token")
                    .post(RequestBody.create("", MediaType.parse("application/json")))
                    .build();

            try (okhttp3.Response refreshResponse = refreshClient.newCall(refreshRequest).execute()) {
                if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                    String body = refreshResponse.body().string();
                    JSONObject json = new JSONObject(body);
                    return json.optString("token", null);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi refresh token: " + e.getMessage());
        }
        return null;
    }
}
