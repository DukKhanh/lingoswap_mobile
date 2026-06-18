package com.lingoswap.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.lingoswap.R;
import com.lingoswap.data.api.UserApiService;
import com.lingoswap.data.model.User;
import com.lingoswap.utils.SocketManager;

import org.json.JSONObject;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * IncomingCallActivity — màn hình nhận cuộc gọi trực tiếp (api-doc 8.2).
 * Mở khi nhận socket `direct_match_offer`.
 */
@AndroidEntryPoint
public class IncomingCallActivity extends AppCompatActivity {

    @Inject SocketManager socketManager;
    @Inject UserApiService userApiService;

    private String callerId;
    private String callerName;
    private boolean accepted = false;
    private boolean handled  = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hiển thị trên màn khoá
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_incoming_call);

        callerId   = getIntent().getStringExtra("callerId");
        callerName = getIntent().getStringExtra("callerName");
        if (callerName == null) callerName = "LingoSwap User";

        if (callerId == null) { finish(); return; }

        TextView tvName = findViewById(R.id.tvCallerName);
        TextView tvInitial = findViewById(R.id.tvCallerInitial);
        tvName.setText(callerName);
        tvInitial.setText(callerName.isEmpty()
                ? "?" : String.valueOf(callerName.charAt(0)).toUpperCase());

        loadCallerName(tvName, tvInitial);

        registerListeners();

        findViewById(R.id.btnAccept).setOnClickListener(v -> accept());
        findViewById(R.id.btnReject).setOnClickListener(v -> reject());
    }

    private void loadCallerName(TextView tvName, TextView tvInitial) {
        if (callerId == null) return;
        userApiService.getPublicProfile(callerId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (isFinishing() || response.body() == null || response.body().getProfile() == null) return;
                String name = response.body().getProfile().getFullName();
                if (name == null || name.isEmpty()) return;
                callerName = name;
                tvName.setText(name);
                tvInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
            }
            @Override
            public void onFailure(Call<User> call, Throwable t) { }
        });
    }

    private io.socket.emitter.Emitter.Listener matchFoundListener;

    private void registerListeners() {
        // Khi server xác nhận ghép phòng → vào VideoCall (mình là callee → isCaller=false)
        matchFoundListener = args -> runOnUiThread(() -> {
            if (!accepted || isFinishing()) return;
            try {
                JSONObject data = (JSONObject) args[0];
                String sessionId   = data.getString("sessionId");
                String partnerId   = data.optString("partnerId", callerId);
                String partnerName = data.optString("partnerName", callerName);

                Intent intent = new Intent(this, VideoCallActivity.class);
                intent.putExtra("sessionId",   sessionId);
                intent.putExtra("partnerId",   partnerId);
                intent.putExtra("partnerName", partnerName);
                intent.putExtra("isCaller",    false);
                startActivity(intent);
                finish();
            } catch (Exception ignored) {
                finish();
            }
        });
        socketManager.onMatchFound(matchFoundListener);
    }

    private void accept() {
        accepted = true;
        handled  = true;
        socketManager.respondDirectMatch(callerId, true);
        TextView status = findViewById(R.id.tvCallStatus);
        if (status != null) status.setText(getString(R.string.call_connecting));
    }

    private void reject() {
        handled = true;
        socketManager.respondDirectMatch(callerId, false);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!handled) socketManager.respondDirectMatch(callerId, false);
        if (matchFoundListener != null) socketManager.off("match_found", matchFoundListener);
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, getString(R.string.call_use_buttons), Toast.LENGTH_SHORT).show();
    }
}
