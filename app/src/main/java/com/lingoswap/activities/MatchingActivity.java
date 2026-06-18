package com.lingoswap.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.lingoswap.R;
import com.lingoswap.data.local.UserPreferences;
import com.lingoswap.utils.SocketManager;

import org.json.JSONObject;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MatchingActivity extends AppCompatActivity {

    private static final long CONNECT_TIMEOUT_MS = 6_000L;

    @Inject SocketManager   socketManager;
    @Inject UserPreferences userPreferences;

    private int waitSeconds = 0;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private TextView tvWaitTime;
    private String language;

    private volatile boolean matchFound = false;

    private final Handler connectTimeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable connectTimeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matching);

        // Mã ngôn ngữ (en, vi, ja...) khớp với hàng đợi queue:<language> của backend.
        language = getIntent().getStringExtra("language");
        if (language == null || language.trim().isEmpty()) language = "en";

        tvWaitTime = findViewById(R.id.tvWaitTime);
        Button btnCancelSearch = findViewById(R.id.btnCancelSearch);

        animateRadar();
        startTimer();
        registerSocketListeners();
        joinQueueWhenReady();

        // Nếu socket rớt rồi nối lại khi đang chờ, tự vào lại queue (tránh bị loại khỏi hàng chờ).
        socketManager.setOnReconnect(() -> {
            if (!matchFound) socketManager.joinMatchQueue(language);
        });

        btnCancelSearch.setOnClickListener(v -> cancelSearch());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
        if (connectTimeoutRunnable != null) {
            connectTimeoutHandler.removeCallbacks(connectTimeoutRunnable);
        }
        socketManager.cancelPendingQueueJoin();
        socketManager.setOnReconnect(null);
        unregisterSocketListeners();
        if (!matchFound) socketManager.leaveQueue();
    }

    private void joinQueueWhenReady() {
        if (socketManager.isConnected()) {
            socketManager.joinMatchQueue(language);
        } else {
            startConnectTimeout();
            socketManager.joinQueueWhenReady(language, null);
        }
    }

    private void startConnectTimeout() {
        connectTimeoutRunnable = () -> {
            if (!matchFound && !isFinishing() && !socketManager.isConnected()) {
                Toast.makeText(this, "Không thể kết nối máy chủ. Vui lòng thử lại.",
                        Toast.LENGTH_LONG).show();
                socketManager.cancelPendingQueueJoin();
                finish();
            }
        };
        connectTimeoutHandler.postDelayed(connectTimeoutRunnable, CONNECT_TIMEOUT_MS);
    }

    private io.socket.emitter.Emitter.Listener matchFoundListener;

    private void registerSocketListeners() {
        matchFoundListener = args -> runOnUiThread(() -> {
            if (matchFound || isFinishing()) return;
            matchFound = true;
            if (connectTimeoutRunnable != null) {
                connectTimeoutHandler.removeCallbacks(connectTimeoutRunnable);
            }
            try {
                JSONObject data  = (JSONObject) args[0];
                String sessionId   = data.getString("sessionId");
                String partnerId   = data.getString("partnerId");
                String partnerName = data.optString("partnerName", "LingoSwap User");

                // Backend match_found không gửi isCaller → cả 2 máy đều false thì không
                // ai tạo offer (deadlock). Chọn caller xác định bằng cùng logic comparePeerIds
                // như web (MeetingPage): id lớn hơn làm caller → đúng 1 máy tạo offer,
                // đồng nhất kể cả khi web ↔ mobile ghép chéo.
                String myId = userPreferences.getUserId();
                boolean isCaller = data.has("isCaller")
                        ? data.optBoolean("isCaller", false)
                        : (myId != null && comparePeerIds(myId, partnerId) > 0);

                Intent intent = new Intent(this, VideoCallActivity.class);
                intent.putExtra("sessionId",   sessionId);
                intent.putExtra("partnerId",   partnerId);
                intent.putExtra("partnerName", partnerName);
                intent.putExtra("language",    language);
                intent.putExtra("isCaller",    isCaller);
                startActivity(intent);
                finish();
            } catch (Exception e) {
                matchFound = false;
            }
        });
        socketManager.onMatchFound(matchFoundListener);

        socketManager.onWaitingStatus(args -> runOnUiThread(() -> {
            try {
                JSONObject data = (JSONObject) args[0];
                Toast.makeText(this, data.optString("message", "Đang tìm kiếm..."),
                        Toast.LENGTH_SHORT).show();
            } catch (Exception ignored) {}
        }));

        socketManager.onQueueTimeout(args -> runOnUiThread(() -> {
            if (isFinishing()) return;
            String msg = "Không tìm được đối tác. Vui lòng thử lại.";
            try {
                msg = ((JSONObject) args[0]).optString("message", msg);
            } catch (Exception ignored) {}
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            finish();
        }));

        socketManager.onMatchError(args -> runOnUiThread(() -> {
            if (isFinishing()) return;
            String msg = (args.length > 0) ? args[0].toString() : "Lỗi hệ thống";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            finish();
        }));
    }

    /** Cùng logic với comparePeerIds bên web: id số thì so sánh số, còn lại so sánh chuỗi. */
    private static int comparePeerIds(String a, String b) {
        try {
            return Long.compare(Long.parseLong(a), Long.parseLong(b));
        } catch (NumberFormatException e) {
            return a.compareTo(b);
        }
    }

    private void unregisterSocketListeners() {
        socketManager.off("waiting_status");
        socketManager.off("queue_timeout");
        socketManager.off("error");
        // Gỡ đúng listener match_found của màn này (không đụng màn khác)
        if (matchFoundListener != null) {
            socketManager.off("match_found", matchFoundListener);
            matchFoundListener = null;
        }
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                waitSeconds++;
                int mins = waitSeconds / 60;
                int secs = waitSeconds % 60;
                if (tvWaitTime != null) {
                    tvWaitTime.setText(mins > 0
                            ? String.format("%d:%02d", mins, secs)
                            : String.format("Đang tìm... %ds", secs));
                }
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void animateRadar() {
        View ring1 = findViewById(R.id.radarRing1);
        View ring2 = findViewById(R.id.radarRing2);
        View ring3 = findViewById(R.id.radarRing3);
        if (ring1 != null) pulseRing(ring1, 0);
        if (ring2 != null) pulseRing(ring2, 400);
        if (ring3 != null) pulseRing(ring3, 800);
    }

    private void pulseRing(View ring, long delay) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(ring, "scaleX", 0.95f, 1.05f);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setRepeatMode(ValueAnimator.REVERSE);

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(ring, "scaleY", 0.95f, 1.05f);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatMode(ValueAnimator.REVERSE);

        ObjectAnimator alpha = ObjectAnimator.ofFloat(ring, "alpha", 0.8f, 0.1f);
        alpha.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setRepeatMode(ValueAnimator.REVERSE);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY, alpha);
        set.setDuration(2000);
        set.setStartDelay(delay);
        set.start();
    }

    private void cancelSearch() {
        socketManager.cancelPendingQueueJoin();
        socketManager.leaveQueue();
        finish();
    }
}
