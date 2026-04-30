package com.lingoswap.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.lingoswap.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VideoCallActivity extends AppCompatActivity {

    private boolean isMuted       = false;
    private boolean isCameraOff   = false;
    private boolean isChatVisible = true;

    // ── UI refs ────────────────────────────────────────────────────
    private TextView tvMuteIcon, tvCameraIcon;
    private LinearLayout chatPanel;
    private LinearLayout chatMessages;
    private ScrollView scrollChat;
    private EditText etChatInput;

    // ── Timer ──────────────────────────────────────────────────────
    private TextView tvCallTimer;
    private Handler  timerHandler = new Handler(Looper.getMainLooper());
    private int      elapsedSeconds = 0;
    private Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        String partnerName = getIntent().getStringExtra("partnerName");
        String language    = getIntent().getStringExtra("language");

        // ── Tên đối tác ────────────────────────────────────────────
        TextView tvPartnerName = findViewById(R.id.tvPartnerName);
        if (tvPartnerName != null && partnerName != null)
            tvPartnerName.setText(partnerName);

        // ── Timer cuộc gọi ─────────────────────────────────────────
        tvCallTimer = findViewById(R.id.tvCallTimer);
        startCallTimer();

        // ── Control buttons ────────────────────────────────────────
        LinearLayout btnMute        = findViewById(R.id.btnMute);
        LinearLayout btnStopVideo   = findViewById(R.id.btnStopVideo);
        LinearLayout btnToggleChat  = findViewById(R.id.btnToggleChat);
        LinearLayout btnEndCall     = findViewById(R.id.btnEndCall);

        tvMuteIcon   = (btnMute != null) ? btnMute.findViewById(R.id.tvMuteIcon) : null;
        tvCameraIcon = (btnStopVideo != null) ? btnStopVideo.findViewById(R.id.tvCameraIcon) : null;

        chatPanel    = findViewById(R.id.chatPanel);
        chatMessages = findViewById(R.id.chatMessages);
        scrollChat   = findViewById(R.id.scrollChat);
        etChatInput  = findViewById(R.id.etChatInput);
        FrameLayout btnSendMsg = findViewById(R.id.btnSendMessage);

        // ── Mute / Unmute ──────────────────────────────────────────
        if (btnMute != null) {
            btnMute.setOnClickListener(v -> {
                isMuted = !isMuted;
                if (tvMuteIcon != null)
                    tvMuteIcon.setText(isMuted ? "🔇" : "🎤");
                btnMute.setBackgroundResource(isMuted
                        ? R.drawable.bg_ctrl_btn_red
                        : R.drawable.bg_ctrl_btn);
                Toast.makeText(this, isMuted ? "Đã tắt mic" : "Đã bật mic", Toast.LENGTH_SHORT).show();
            });
        }

        // ── Bật / Tắt camera ───────────────────────────────────────
        if (btnStopVideo != null) {
            btnStopVideo.setOnClickListener(v -> {
                isCameraOff = !isCameraOff;
                if (tvCameraIcon != null)
                    tvCameraIcon.setText(isCameraOff ? "📵" : "📹");
                btnStopVideo.setBackgroundResource(isCameraOff
                        ? R.drawable.bg_ctrl_btn_red
                        : R.drawable.bg_ctrl_btn);
                Toast.makeText(this, isCameraOff ? "Đã tắt camera" : "Đã bật camera", Toast.LENGTH_SHORT).show();
            });
        }

        // ── Hiện / Ẩn chat ─────────────────────────────────────────
        if (btnToggleChat != null) {
            btnToggleChat.setOnClickListener(v -> {
                isChatVisible = !isChatVisible;
                if (chatPanel != null) chatPanel.setVisibility(isChatVisible ? View.VISIBLE : View.GONE);
            });
        }

        // ── Gửi tin nhắn ───────────────────────────────────────────
        if (btnSendMsg != null) btnSendMsg.setOnClickListener(v -> sendChatMessage());
        if (etChatInput != null) {
            etChatInput.setOnEditorActionListener((tv, actionId, event) -> {
                sendChatMessage();
                return true;
            });
        }

        // ── Kết thúc cuộc gọi ──────────────────────────────────────
        if (btnEndCall != null) btnEndCall.setOnClickListener(v -> endCall());
    }

    /** Gửi tin nhắn chat */
    private void sendChatMessage() {
        if (etChatInput == null) return;
        String msg = etChatInput.getText().toString().trim();
        if (msg.isEmpty()) return;

        // Thêm bubble tin nhắn
        addChatBubble("Bạn", msg, true);
        etChatInput.setText("");
    }

    /** Thêm bong bóng chat vào danh sách */
    private void addChatBubble(String sender, String message, boolean isSelf) {
        if (chatMessages == null) return;
        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(0, 4, 0, 4);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = isSelf ? android.view.Gravity.END : android.view.Gravity.START;
        bubble.setLayoutParams(params);

        TextView tvMsg = new TextView(this);
        tvMsg.setText(message);
        tvMsg.setTextColor(isSelf ? getColor(R.color.white) : getColor(R.color.text_dark));
        tvMsg.setBackgroundResource(isSelf ? R.drawable.bg_btn_primary : R.drawable.bg_card);
        tvMsg.setPadding((int)(16 * getResources().getDisplayMetrics().density), (int)(8 * getResources().getDisplayMetrics().density), (int)(16 * getResources().getDisplayMetrics().density), (int)(8 * getResources().getDisplayMetrics().density));
        tvMsg.setMaxWidth((int)(300 * getResources().getDisplayMetrics().density));

        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        TextView tvTime = new TextView(this);
        tvTime.setText(time);
        tvTime.setTextColor(getColor(R.color.text_muted));
        tvTime.setTextSize(10);
        tvTime.setGravity(isSelf ? android.view.Gravity.END : android.view.Gravity.START);

        bubble.addView(tvMsg);
        bubble.addView(tvTime);
        chatMessages.addView(bubble);

        // Scroll xuống cuối
        if (scrollChat != null) {
            scrollChat.post(() -> scrollChat.fullScroll(View.FOCUS_DOWN));
        }
    }

    /** Bộ đếm thời gian cuộc gọi */
    private void startCallTimer() {
        timerRunnable = new Runnable() {
            @Override public void run() {
                elapsedSeconds++;
                int mins = elapsedSeconds / 60;
                int secs = elapsedSeconds % 60;
                if (tvCallTimer != null)
                    tvCallTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs));
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    /** Kết thúc cuộc gọi và chuyển đến màn hình đánh giá */
    private void endCall() {
        timerHandler.removeCallbacks(timerRunnable);
        Intent intent = new Intent(this, RatingActivity.class);
        intent.putExtra("callDuration", elapsedSeconds);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Nhấn 'Kết thúc' để rời cuộc gọi", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
    }
}
