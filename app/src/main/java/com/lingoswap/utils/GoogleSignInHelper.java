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

/** Tiện ích đăng nhập Google: lấy idToken để gửi lên backend xác thực. */
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

    public Intent getSignInIntent() {
        return client.getSignInIntent();
    }

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
