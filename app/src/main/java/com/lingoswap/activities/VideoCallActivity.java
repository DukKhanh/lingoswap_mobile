package com.lingoswap.activities;
import java.util.ArrayList;
import java.util.List;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.lingoswap.R;
import com.lingoswap.utils.SocketManager;
import com.lingoswap.utils.WebRtcManager;

import org.json.JSONObject;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * VideoCallActivity — màn hình video call với WebRTC thật.
 */
@AndroidEntryPoint
public class VideoCallActivity extends AppCompatActivity {

    private static final String TAG = "VideoCallActivity";

    @Inject SocketManager socketManager;

    private WebRtcManager webRtcManager;
    private EglBase eglBase;

    // Intent data
    private String sessionId;
    private String partnerId;
    private String language;
    private boolean isCaller = false;

    // UI
    private boolean isMuted     = false;
    private boolean isCameraOff = false;
    private boolean isChatVisible = true;
    private final List<String> iceCandidateQueue = new ArrayList<>();
    private volatile boolean remoteDescriptionSet = false;
    private TextView tvMuteIcon, tvCameraIcon;
    private LinearLayout chatPanel, chatMessages;
    private ScrollView scrollChat;
    private EditText etChatInput;

    // Timer
    private TextView tvCallTimer;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private int elapsedSeconds = 0;
    private Runnable timerRunnable;

    // Video views
    private SurfaceViewRenderer localVideoView;
    private SurfaceViewRenderer remoteVideoView;

    // Heartbeat
    private static final long HEARTBEAT_INTERVAL_MS = 30_000L; // 30s — dưới ngưỡng 90s của server
    private final Handler heartbeatHandler = new Handler(Looper.getMainLooper());
    private Runnable heartbeatRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        sessionId = getIntent().getStringExtra("sessionId");
        partnerId = getIntent().getStringExtra("partnerId");
        language  = getIntent().getStringExtra("language");
        isCaller  = getIntent().getBooleanExtra("isCaller", false);

        if (sessionId == null || partnerId == null) {
            Log.e(TAG, "Thiếu sessionId hoặc partnerId");
            finish();
            return;
        }

        Log.d(TAG, "VideoCall started: sessionId=" + sessionId + " partnerId=" + partnerId + " isCaller=" + isCaller);

        bindViews();
        startCallTimer();
        initWebRtc();
        registerSocketListeners();
        // Báo server biết đã vào call, tránh bị presence timeout
        try {
            JSONObject joinData = new JSONObject();
            joinData.put("sessionId", sessionId);
            socketManager.emit("join_call_room", joinData);
            Log.d(TAG, "Emitted join_call_room: " + sessionId);
        } catch (Exception e) {
            Log.e(TAG, "join_call_room error: " + e.getMessage());
        }
        startHeartbeat();

        if (isCaller) {
            // ✅ Fix — tăng lên 2500ms và kiểm tra socket còn connected không
            timerHandler.postDelayed(() -> {
                if (!isFinishing() && webRtcManager != null) {
                    if (!socketManager.isConnected()) {
                        Log.e(TAG, "Socket mất kết nối trước khi tạo offer!");
                        Toast.makeText(this, "Mất kết nối, thử lại", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    Log.d(TAG, "Tạo offer...");
                    webRtcManager.createOffer();
                }
            }, 2500);
        }
    }

    private void bindViews() {
        localVideoView  = findViewById(R.id.localVideoView);
        remoteVideoView = findViewById(R.id.remoteVideoView);
        tvCallTimer     = findViewById(R.id.tvCallTimer);
        tvMuteIcon      = findViewById(R.id.tvMuteIcon);
        tvCameraIcon    = findViewById(R.id.tvCameraIcon);
        chatPanel       = findViewById(R.id.chatPanel);
        chatMessages    = findViewById(R.id.chatMessages);
        scrollChat      = findViewById(R.id.scrollChat);
        etChatInput     = findViewById(R.id.etChatInput);

        TextView tvPartnerName = findViewById(R.id.tvPartnerName);
        if (tvPartnerName != null) tvPartnerName.setText(partnerId);

        // Buttons
        LinearLayout btnMute       = findViewById(R.id.btnMute);
        LinearLayout btnStopVideo  = findViewById(R.id.btnStopVideo);
        LinearLayout btnToggleChat = findViewById(R.id.btnToggleChat);
        LinearLayout btnEndCall    = findViewById(R.id.btnEndCall);
        FrameLayout  btnSendMsg    = findViewById(R.id.btnSendMessage);

        if (btnMute != null) btnMute.setOnClickListener(v -> toggleMic());
        if (btnStopVideo != null) btnStopVideo.setOnClickListener(v -> toggleCamera());
        if (btnToggleChat != null) btnToggleChat.setOnClickListener(v -> toggleChat());
        if (btnEndCall != null) btnEndCall.setOnClickListener(v -> endCall());
        if (btnSendMsg != null) btnSendMsg.setOnClickListener(v -> sendChatMessage());

        if (etChatInput != null) {
            etChatInput.setOnEditorActionListener((tv, actionId, event) -> {
                sendChatMessage();
                return true;
            });
        }
    }

    private void initWebRtc() {
        eglBase = EglBase.create();
        webRtcManager = new WebRtcManager(new WebRtcManager.Callback() {

            @Override
            public void onLocalSdpReady(SessionDescription sdp) {
                try {
                    JSONObject sdpObj = new JSONObject();
                    sdpObj.put("type", sdp.type.canonicalForm());
                    sdpObj.put("sdp",  sdp.description);
                    String sdpJson = sdpObj.toString();

                    if (sdp.type == SessionDescription.Type.OFFER) {
                        socketManager.sendWebRtcOffer(sessionId, sdpJson);
                        Log.d(TAG, "Offer sent");
                    } else {
                        socketManager.sendWebRtcAnswer(sessionId, sdpJson);
                        Log.d(TAG, "Answer sent");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "onLocalSdpReady error: " + e.getMessage());
                }
            }

            @Override
            public void onIceCandidateReady(IceCandidate candidate) {
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("sdpMid",        candidate.sdpMid);
                    obj.put("sdpMLineIndex", candidate.sdpMLineIndex);
                    obj.put("candidate",     candidate.sdp);
                    socketManager.sendIceCandidate(sessionId, obj.toString());
                } catch (Exception e) {
                    Log.e(TAG, "onIceCandidateReady error: " + e.getMessage());
                }
            }

            @Override
            public void onRemoteVideoTrackReceived(VideoTrack videoTrack) {
                runOnUiThread(() -> {
                    if (remoteVideoView != null) {
                        webRtcManager.attachRemoteView(videoTrack, remoteVideoView);
                        Log.d(TAG, "Remote video attached");
                    }
                });
            }

            @Override
            public void onConnectionFailed(String reason) {
                runOnUiThread(() -> {
                    Toast.makeText(VideoCallActivity.this, reason, Toast.LENGTH_LONG).show();
                    endCall();
                });
            }
        });

        webRtcManager.init(this, eglBase);
        webRtcManager.startLocalStream(localVideoView);
    }

    private void registerSocketListeners() {
        socketManager.onWebRtcOffer(args -> {
            if (isCaller) return;
            try {
                JSONObject data  = (JSONObject) args[0];
                String offerJson = data.getJSONObject("offer").toString();
                webRtcManager.setRemoteOffer(offerJson);

                // ✅ FIX: Đánh dấu remote SDP đã set, flush queue trước khi createAnswer
                remoteDescriptionSet = true;
                flushIceCandidateQueue();

                webRtcManager.createAnswer();
                Log.d(TAG, "Received offer, created answer");
            } catch (Exception e) {
                Log.e(TAG, "webrtc_offer error: " + e.getMessage());
            }
        });

        socketManager.onWebRtcAnswer(args -> {
            if (!isCaller) return;
            try {
                JSONObject data   = (JSONObject) args[0];
                String answerJson = data.getJSONObject("answer").toString();
                webRtcManager.setRemoteAnswer(answerJson);

                // ✅ FIX: Đánh dấu remote SDP đã set, flush queue
                remoteDescriptionSet = true;
                flushIceCandidateQueue();

                Log.d(TAG, "Remote answer set");
            } catch (Exception e) {
                Log.e(TAG, "webrtc_answer error: " + e.getMessage());
            }
        });

        socketManager.onIceCandidate(args -> {
            try {
                JSONObject data      = (JSONObject) args[0];
                String candidateJson = data.getJSONObject("candidate").toString();

                // ✅ FIX: Nếu remote SDP chưa set → queue lại, xử lý sau
                if (remoteDescriptionSet) {
                    webRtcManager.addRemoteIceCandidate(candidateJson);
                } else {
                    iceCandidateQueue.add(candidateJson);
                    Log.d(TAG, "ICE candidate queued (remoteDesc chưa sẵn sàng), queue size=" + iceCandidateQueue.size());
                }
            } catch (Exception e) {
                Log.e(TAG, "webrtc_ice_candidate error: " + e.getMessage());
            }
        });

        socketManager.onPartnerDisconnected(args -> runOnUiThread(() -> {
            Toast.makeText(this, "Đối tác đã rời cuộc gọi.", Toast.LENGTH_SHORT).show();
            endCall();
        }));

        socketManager.onReceiveMessage(args -> runOnUiThread(() -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String content  = data.optString("content", "");
                if (!content.isEmpty()) {
                    addChatBubble("Đối tác", content, false);
                }
            } catch (Exception e) {
                Log.e(TAG, "receive_message error: " + e.getMessage());
            }
        }));
    }

    private void unregisterSocketListeners() {
        socketManager.off("webrtc_offer");
        socketManager.off("webrtc_answer");
        socketManager.off("webrtc_ice_candidate");
        socketManager.off("partner_disconnected");
        socketManager.off("receive_message");
    }

    private void flushIceCandidateQueue() {
        if (iceCandidateQueue.isEmpty()) return;
        Log.d(TAG, "Flushing " + iceCandidateQueue.size() + " queued ICE candidates");
        for (String candidateJson : iceCandidateQueue) {
            webRtcManager.addRemoteIceCandidate(candidateJson);
        }
        iceCandidateQueue.clear();
    }

    private void toggleMic() {
        isMuted = !isMuted;
        webRtcManager.setMicEnabled(!isMuted);
        if (tvMuteIcon != null) tvMuteIcon.setText(isMuted ? "🔇" : "🎤");
        LinearLayout btnMute = findViewById(R.id.btnMute);
        if (btnMute != null) btnMute.setBackgroundResource(
            isMuted ? R.drawable.bg_ctrl_btn_red : R.drawable.bg_ctrl_btn);
        Toast.makeText(this, isMuted ? "Đã tắt mic" : "Đã bật mic", Toast.LENGTH_SHORT).show();
    }

    private void toggleCamera() {
        isCameraOff = !isCameraOff;
        webRtcManager.setCameraEnabled(!isCameraOff);
        if (tvCameraIcon != null) tvCameraIcon.setText(isCameraOff ? "📵" : "📹");
        if (localVideoView != null) localVideoView.setVisibility(
            isCameraOff ? View.INVISIBLE : View.VISIBLE);
        LinearLayout btnStopVideo = findViewById(R.id.btnStopVideo);
        if (btnStopVideo != null) btnStopVideo.setBackgroundResource(
            isCameraOff ? R.drawable.bg_ctrl_btn_red : R.drawable.bg_ctrl_btn);
        Toast.makeText(this, isCameraOff ? "Đã tắt camera" : "Đã bật camera", Toast.LENGTH_SHORT).show();
    }

    private void toggleChat() {
        isChatVisible = !isChatVisible;
        if (chatPanel != null) chatPanel.setVisibility(
            isChatVisible ? View.VISIBLE : View.GONE);
    }

    private void sendChatMessage() {
        if (etChatInput == null) return;
        String msg = etChatInput.getText().toString().trim();
        if (msg.isEmpty()) return;

        socketManager.sendMessage(partnerId, msg, sessionId);
        addChatBubble("Bạn", msg, true);
        etChatInput.setText("");
    }

    private void addChatBubble(String sender, String message, boolean isSelf) {
        if (chatMessages == null) return;

        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(0, 4, 0, 4);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = isSelf
            ? android.view.Gravity.END
            : android.view.Gravity.START;
        bubble.setLayoutParams(params);

        float density = getResources().getDisplayMetrics().density;
        int pad = (int)(12 * density);

        TextView tvMsg = new TextView(this);
        tvMsg.setText(message);
        tvMsg.setTextColor(isSelf
            ? android.graphics.Color.WHITE
            : android.graphics.Color.BLACK); // Using standard colors or R.color if available
        tvMsg.setBackgroundResource(isSelf
            ? R.drawable.bg_btn_primary
            : R.drawable.bg_card);
        tvMsg.setPadding(pad, (int)(8 * density), pad, (int)(8 * density));
        tvMsg.setMaxWidth((int)(280 * density));

        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        TextView tvTime = new TextView(this);
        tvTime.setText(time);
        tvTime.setTextColor(0xFF888888);
        tvTime.setTextSize(10);
        tvTime.setGravity(isSelf
            ? android.view.Gravity.END
            : android.view.Gravity.START);

        bubble.addView(tvMsg);
        bubble.addView(tvTime);
        chatMessages.addView(bubble);

        if (scrollChat != null) {
            scrollChat.post(() -> scrollChat.fullScroll(View.FOCUS_DOWN));
        }
    }

    private void startCallTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                elapsedSeconds++;
                int mins = elapsedSeconds / 60;
                int secs = elapsedSeconds % 60;
                if (tvCallTimer != null) {
                    tvCallTimer.setText(String.format(Locale.getDefault(),
                        "%02d:%02d", mins, secs));
                }
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void endCall() {
        stopHeartbeat();

        socketManager.leaveQueue();

        timerHandler.removeCallbacks(timerRunnable);
        unregisterSocketListeners();

        Intent intent = new Intent(this, RatingActivity.class);
        intent.putExtra("callDuration", elapsedSeconds);
        intent.putExtra("sessionId",    sessionId);
        intent.putExtra("partnerId",    partnerId);
        startActivity(intent);
        finish();
    }
    private void startHeartbeat() {
        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (socketManager.isConnected()) {
                    socketManager.emit("heartbeat", null); // hoặc new JSONObject()
                    Log.d(TAG, "💓 heartbeat sent");
                }
                heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS);
            }
        };
        heartbeatHandler.post(heartbeatRunnable);
    }

    private void stopHeartbeat() {
        if (heartbeatRunnable != null) {
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
            heartbeatRunnable = null;
        }
    }
    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Nhấn 'Kết thúc' để rời cuộc gọi", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopHeartbeat();
        timerHandler.removeCallbacks(timerRunnable);
        unregisterSocketListeners();
        iceCandidateQueue.clear();
        remoteDescriptionSet = false;

        if (localVideoView != null) {
            localVideoView.release();
            localVideoView = null;
        }
        if (remoteVideoView != null) {
            remoteVideoView.release();
            remoteVideoView = null;
        }
        if (webRtcManager != null) {
            webRtcManager.release(); // EglBase được release bên trong này
            webRtcManager = null;
        }

        // ❌ XÓA HOÀN TOÀN đoạn này — webRtcManager.release() đã làm rồi
        // if (eglBase != null) {
        //     eglBase.release();
        //     eglBase = null;
        // }
    }
}
