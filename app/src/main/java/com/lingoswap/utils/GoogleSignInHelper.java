package com.lingoswap.utils;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.activity.result.ActivityResult;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.lingoswap.R;

/**
 * GoogleSignInHelper — tiện ích đăng nhập Google.
 *
 * Hướng dẫn tích hợp:
 * 1. Thêm vào build.gradle (app):
 *    implementation 'com.google.android.gms:play-services-auth:21.0.0'
 *
 * 2. Tạo project trên https://console.cloud.google.com
 *    → APIs & Services → Credentials → Create OAuth 2.0 Client ID (Android)
 *    → Lấy Web Client ID → đặt vào strings.xml:
 *    <string name="google_web_client_id">YOUR_WEB_CLIENT_ID.apps.googleusercontent.com</string>
 *
 * 3. Dùng trong Activity:
 *    private GoogleSignInHelper googleHelper;
 *
 *    @Override protected void onCreate(...) {
 *        googleHelper = new GoogleSignInHelper(this);
 *        binding.btnGoogleSignIn.setOnClickListener(v ->
 *            startActivityForResult(googleHelper.getSignInIntent(), RC_GOOGLE_SIGN_IN));
 *    }
 *
 *    @Override protected void onActivityResult(int requestCode, ...) {
 *        if (requestCode == RC_GOOGLE_SIGN_IN) {
 *            googleHelper.handleResult(data, idToken -> {
 *                // Gửi idToken lên backend POST /api/auth/google
 *                viewModel.googleSignIn(idToken);
 *            });
 *        }
 *    }
 */
public class GoogleSignInHelper {

    private static final String TAG = "GoogleSignInHelper";
    public  static final int    RC_GOOGLE_SIGN_IN = 9001;

    private final GoogleSignInClient client;

    public GoogleSignInHelper(Activity activity) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.google_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();

        client = GoogleSignIn.getClient(activity, gso);
    }

    /** Trả về Intent để startActivityForResult */
    public Intent getSignInIntent() {
        return client.getSignInIntent();
    }

    /** Xử lý kết quả từ onActivityResult */
    public void handleResult(Intent data, Callback callback) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            String idToken = account.getIdToken();
            if (idToken != null) {
                Log.d(TAG, "Google Sign-In thành công | email=" + account.getEmail());
                callback.onSuccess(idToken);
            } else {
                Log.e(TAG, "ID Token null — kiểm tra Web Client ID và requestIdToken()");
                callback.onError("Không lấy được ID Token từ Google");
            }
        } catch (ApiException e) {
            Log.e(TAG, "Google Sign-In thất bại | statusCode=" + e.getStatusCode());
            callback.onError("Đăng nhập Google thất bại: " + e.getMessage());
        }
    }

    /** Đăng xuất khỏi Google (gọi khi user logout khỏi app) */
    public void signOut() {
        client.signOut();
    }

    public interface Callback {
        void onSuccess(String idToken);
        default void onError(String message) {
            Log.e("GoogleSignIn", message);
        }
    }
}
