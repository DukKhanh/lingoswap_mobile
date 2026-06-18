package com.lingoswap.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.lingoswap.R;
import com.lingoswap.utils.SocketManager;

import org.json.JSONObject;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * OutgoingCallActivity — màn hình "Đang gọi..." khi gọi trực tiếp một người bạn (api-doc 8.2).
 * Gửi `direct_match_request`, chờ `match_found` / `direct_match_rejected` / `direct_match_error`.
 */
@AndroidEntryPoint
public class OutgoingCallActivity extends AppCompatActivity {

    public static final String EXTRA_TARGET_ID   = "targetUserId";
    public static final String EXTRA_TARGET_NAME = "targetName";

    @Inject SocketManager socketManager;

    private String targetUserId;
    private String targetName;
    private boolean matched = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outgoing_call);

        targetUserId = getIntent().getStringExtra(EXTRA_TARGET_ID);
        targetName   = getIntent().getStringExtra(EXTRA_TARGET_NAME);
        if (targetName == null) targetName = "LingoSwap User";

        if (targetUserId == null) { finish(); return; }

        TextView tvName = findViewById(R.id.tvCalleeName);
        TextView tvInitial = findViewById(R.id.tvCalleeInitial);
        tvName.setText(targetName);
        tvInitial.setText(targetName.isEmpty()
                ? "?" : String.valueOf(targetName.charAt(0)).toUpperCase());

        registerListeners();

        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());

        socketManager.requestDirectMatch(targetUserId);
    }

    private io.socket.emitter.Emitter.Listener matchFoundListener;

    private void registerListeners() {
        matchFoundListener = args -> runOnUiThread(() -> {
            if (isFinishing()) return;
            try {
                JSONObject data = (JSONObject) args[0];
                String sessionId   = data.getString("sessionId");
                String partnerId   = data.optString("partnerId", targetUserId);
                String partnerName = data.optString("partnerName", targetName);
                matched = true;

                Intent intent = new Intent(this, VideoCallActivity.class);
                intent.putExtra("sessionId",   sessionId);
                intent.putExtra("partnerId",   partnerId);
                intent.putExtra("partnerName", partnerName);
                intent.putExtra("isCaller",    true);
                startActivity(intent);
                finish();
            } catch (Exception ignored) {
                finish();
            }
        });
        socketManager.onMatchFound(matchFoundListener);

        socketManager.onDirectMatchRejected(args -> runOnUiThread(() -> {
            Toast.makeText(this, getString(R.string.call_rejected), Toast.LENGTH_SHORT).show();
            finish();
        }));

        socketManager.onDirectMatchError(args -> runOnUiThread(() -> {
            String msg = getString(R.string.call_error_offline);
            try {
                JSONObject data = (JSONObject) args[0];
                msg = data.optString("message", msg);
            } catch (Exception ignored) {}
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            finish();
        }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (matchFoundListener != null) socketManager.off("match_found", matchFoundListener);
        socketManager.off("direct_match_rejected");
        socketManager.off("direct_match_error");
    }
}
