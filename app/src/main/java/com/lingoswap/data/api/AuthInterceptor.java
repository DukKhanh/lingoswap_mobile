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
 *  2. Khi nhận 401 → gọi POST /api/auth/token để lấy accessToken mới
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

        // Gửi request với token hiện tại
        Request request = buildRequest(original, token);
        Response response = chain.proceed(request);

        // Nếu 401 → thử refresh token
        if (response.code() == 401 && token != null) {
            response.close();
            Log.d(TAG, "Token hết hạn, đang refresh...");

            String newToken = refreshAccessToken();
            if (newToken != null) {
                userPreferences.saveAccessToken(newToken);
                // Cập nhật SocketManager token nếu cần (event-based)
                Log.d(TAG, "Refresh thành công, retry request");
                Request retryRequest = buildRequest(original, newToken);
                return chain.proceed(retryRequest);
            } else {
                // Refresh thất bại → xóa session, về màn login
                Log.w(TAG, "Refresh thất bại, clear session");
                userPreferences.clear();
            }
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
     * Gọi POST /api/auth/token
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
                    .url(baseUrl + "api/auth/token")
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
