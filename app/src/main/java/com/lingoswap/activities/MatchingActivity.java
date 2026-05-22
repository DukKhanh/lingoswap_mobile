package com.lingoswap.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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

/**
 * MatchingActivity — màn hình chờ ghép cặp.
 *
 * FIX: Thay joinMatchQueue() trực tiếp bằng joinQueueWhenReady() để tránh
 *      race condition khi socket chưa connected mà backend đã match ngay.
 *
 *      Thứ tự đúng:
 *        1. registerSocketListeners()  → đảm bảo match_found được lắng nghe
 *        2. joinQueueWhenReady()       → join khi socket đã sẵn sàng
 *        3. startConnectTimeout()      → fallback nếu socket không connect được
 */
@AndroidEntryPoint
public class MatchingActivity extends AppCompatActivity {

    private static final String TAG                 = "MatchingActivity";
    private static final long   CONNECT_TIMEOUT_MS  = 6_000L; // 6s chờ socket connect

    @Inject SocketManager    socketManager;
    @Inject UserPreferences  userPreferences;

    private int      waitSeconds = 0;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private TextView tvWaitTime;
    private String   language;

    // Guard chống navigate nhiều lần (race condition từ nhiều event)
    private volatile boolean matchFound = false;

    // Handler + Runnable cho timeout kết nối
    private final Handler connectTimeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable connectTimeoutRunnable;

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matching);

        language = normalizeLanguage(getIntent().getStringExtra("language"));
        Log.d(TAG, "onCreate | language=" + language
                + " | socketConnected=" + socketManager.isConnected());

        tvWaitTime = findViewById(R.id.tvWaitTime);
        Button btnCancelSearch = findViewById(R.id.btnCancelSearch);

        animateRadar();
        startTimer();

        // FIX: Đăng ký listener TRƯỚC rồi mới join queue
        registerSocketListeners();
        joinQueueWhenReady();

        btnCancelSearch.setOnClickListener(v -> cancelSearch());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy | matchFound=" + matchFound);

        // Dọn timer
        timerHandler.removeCallbacks(timerRunnable);

        // Dọn connect-timeout
        if (connectTimeoutRunnable != null) {
            connectTimeoutHandler.removeCallbacks(connectTimeoutRunnable);
        }

        // FIX: Huỷ pending join nếu Activity bị destroy trước khi socket connect
        socketManager.cancelPendingQueueJoin();

        // Off listener trước, sau đó mới leave queue
        unregisterSocketListeners();
        if (!matchFound) {
            socketManager.leaveQueue();
        }
    }

    // ─── Queue ───────────────────────────────────────────────────────────────

    /**
     * FIX: Dùng joinQueueWhenReady() thay vì gọi joinMatchQueue() trực tiếp.
     * - Nếu socket đã connected → join ngay lập tức.
     * - Nếu chưa → đợi EVENT_CONNECT (one-shot) rồi join, đồng thời
     *   bật timeout 6s để tránh treo màn hình vô thời hạn.
     */
    private void joinQueueWhenReady() {
        if (socketManager.isConnected()) {
            Log.d(TAG, "Socket đã connected → join queue ngay");
            socketManager.joinMatchQueue(language);
        } else {
            Log.w(TAG, "Socket chưa connected → đăng ký one-shot và bật timeout");
            startConnectTimeout();
            socketManager.joinQueueWhenReady(language, () ->
                    Log.d(TAG, "✅ join_queue đã được emit sau khi socket connected"));
        }
    }

    /**
     * Nếu socket vẫn không connect được sau CONNECT_TIMEOUT_MS → thông báo + finish.
     */
    private void startConnectTimeout() {
        connectTimeoutRunnable = () -> {
            if (!matchFound && !isFinishing() && !socketManager.isConnected()) {
                Log.e(TAG, "⏱ Connect timeout — không thể kết nối socket");
                Toast.makeText(this,
                        "Không thể kết nối máy chủ. Vui lòng thử lại.",
                        Toast.LENGTH_LONG).show();
                socketManager.cancelPendingQueueJoin();
                finish();
            }
        };
        connectTimeoutHandler.postDelayed(connectTimeoutRunnable, CONNECT_TIMEOUT_MS);
    }

    // ─── Socket Listeners ─────────────────────────────────────────────────────

    private void registerSocketListeners() {
        socketManager.onMatchFound(args -> runOnUiThread(() -> {
            Log.d(TAG, "🎯 match_found received | matchFound=" + matchFound
                    + " | finishing=" + isFinishing());

            // Double-check guard để tránh navigate 2 lần
            if (matchFound || isFinishing()) return;
            matchFound = true;

            // Huỷ connect-timeout vì đã match thành công
            if (connectTimeoutRunnable != null) {
                connectTimeoutHandler.removeCallbacks(connectTimeoutRunnable);
            }

            try {
                JSONObject data  = (JSONObject) args[0];
                String sessionId = data.getString("sessionId");
                String partnerId = data.getString("partnerId");

                Log.d(TAG, "✅ Match! sessionId=" + sessionId
                        + " | partnerId=" + partnerId);

                boolean isCaller = data.optBoolean("isCaller", false);

                Intent intent = new Intent(MatchingActivity.this, VideoCallActivity.class);
                intent.putExtra("sessionId", sessionId);
                intent.putExtra("partnerId", partnerId);
                intent.putExtra("language",  language);
                intent.putExtra("isCaller",  isCaller);
                startActivity(intent);
                finish();

            } catch (Exception e) {
                Log.e(TAG, "❌ Lỗi parse match_found: " + e.getMessage());
                // Reset guard để user có thể thử lại nếu parse lỗi
                matchFound = false;
            }
        }));

        socketManager.onWaitingStatus(args -> runOnUiThread(() -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String msg = data.optString("message", "Đang tìm kiếm...");
                Log.d(TAG, "waiting_status: " + msg);
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Lỗi parse waiting_status: " + e.getMessage());
            }
        }));

        socketManager.onQueueTimeout(args -> runOnUiThread(() -> {
            if (isFinishing()) return;
            String msg = "Không tìm được đối tác. Vui lòng thử lại.";
            try {
                JSONObject data = (JSONObject) args[0];
                msg = data.optString("message", msg);
            } catch (Exception ignored) {}
            Log.w(TAG, "queue_timeout: " + msg);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            finish();
        }));

        socketManager.onMatchError(args -> runOnUiThread(() -> {
            if (isFinishing()) return;
            String msg = (args.length > 0) ? args[0].toString() : "Lỗi hệ thống";
            Log.e(TAG, "match error: " + msg);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            finish();
        }));
    }

    // ✅ Fix — nếu đã match thì giữ nguyên socket listeners, chỉ off các event không cần thiết
    private void unregisterSocketListeners() {
        // Nếu đã match rồi → VideoCallActivity sẽ tự quản lý socket
        // Chỉ off các event liên quan đến queue
        socketManager.off("waiting_status");
        socketManager.off("queue_timeout");
        if (!matchFound) {
            // Chỉ off match_found nếu chưa match (user hủy)
            socketManager.off("match_found");
            socketManager.off("error");
        }
        Log.d(TAG, "unregisterSocketListeners | matchFound=" + matchFound);
    }

    // ─── UI helpers ───────────────────────────────────────────────────────────

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
        Log.d(TAG, "User huỷ tìm kiếm");
        socketManager.cancelPendingQueueJoin();
        socketManager.leaveQueue();
        finish();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static String normalizeLanguage(String raw) {
        if (raw == null || raw.isEmpty()) return "english";
        switch (raw.toLowerCase().trim()) {
            case "english":
            case "japanese":
            case "korean":
            case "chinese":
            case "french":
                return raw.toLowerCase().trim();
            default:
                return "english";
        }
    }
}