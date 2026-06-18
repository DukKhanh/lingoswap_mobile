package com.lingoswap.activities;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.lingoswap.R;
import com.lingoswap.data.api.UserApiService;
import com.lingoswap.data.model.User;
import com.lingoswap.utils.ImageUtils;
import com.lingoswap.utils.SocketManager;
import com.lingoswap.utils.WebRtcManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
import io.socket.emitter.Emitter;

/**
 * VideoCallActivity — màn hình video call với WebRTC thật.
 */
@AndroidEntryPoint
public class VideoCallActivity extends AppCompatActivity {

    private static final String TAG = "VideoCallActivity";
    private static final int REQ_MEDIA_PERMISSION = 301;
    private static final int MAX_OFFER_RETRIES = 10;
    private static final long OFFER_RETRY_INTERVAL_MS = 1_000L;

    @Inject SocketManager socketManager;
    @Inject UserApiService userApiService;

    private WebRtcManager webRtcManager;
    private EglBase eglBase;

    private String sessionId;
    private String partnerId;
    private String partnerName;
    private String language;
    private boolean isCaller = false;

    private boolean isMuted     = false;
    private boolean isCameraOff = false;
    private boolean isChatVisible = true;
    private volatile boolean remoteAttached = false;
    private boolean answerReceived = false;
    private boolean callStarted = false;
    private boolean endingCall = false;
    private boolean offerCreationScheduled = false;
    private boolean offerCreationStarted = false;
    private int offerCreateWaitAttempts = 0;
    private int offerRetryCount = 0;
    private String localOfferJson;
    private Runnable offerRetryRunnable;
    private Emitter.Listener webRtcOfferListener;
    private Emitter.Listener webRtcAnswerListener;
    private Emitter.Listener iceCandidateListener;
    private Emitter.Listener partnerDisconnectedListener;
    private Emitter.Listener receiveMessageListener;
    private ImageView tvMuteIcon, tvCameraIcon;
    private LinearLayout chatPanel, chatMessages;
    private ScrollView scrollChat;
    private EditText etChatInput;

    private TextView tvCallTimer;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private int elapsedSeconds = 0;
    private Runnable timerRunnable;

    private SurfaceViewRenderer localVideoView;
    private SurfaceViewRenderer remoteVideoView;
    private ImageView imgPartnerAvatar;
    private TextView tvPartnerName;

    private static final long HEARTBEAT_INTERVAL_MS = 30_000L; // 30s — dưới ngưỡng 90s của server
    private final Handler heartbeatHandler = new Handler(Looper.getMainLooper());
    private Runnable heartbeatRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        sessionId = getIntent().getStringExtra("sessionId");
        partnerId = getIntent().getStringExtra("partnerId");
        language    = getIntent().getStringExtra("language");
        isCaller    = getIntent().getBooleanExtra("isCaller", false);
        partnerName = getIntent().getStringExtra("partnerName");

        if (sessionId == null || partnerId == null) {
            Log.e(TAG, "Thiếu sessionId hoặc partnerId");
            finish();
            return;
        }

        Log.d(TAG, "VideoCall started: sessionId=" + sessionId + " partnerId=" + partnerId + " isCaller=" + isCaller);

        bindViews();
        if (hasMediaPermission()) {
            startCallWhenReady();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    REQ_MEDIA_PERMISSION);
        }
    }

    private boolean hasMediaPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startCallWhenReady() {
        if (callStarted || isFinishing()) return;
        callStarted = true;

        if (!socketManager.isConnected()) {
            socketManager.connect();
        }

        startCallTimer();
        initWebRtc();
        registerSocketListeners();
        joinCallRoom();
        startHeartbeat();

        if (isCaller) {
            scheduleOfferCreation(500);
        }
    }

    private void scheduleOfferCreation(long delayMs) {
        if (offerCreationScheduled || offerCreationStarted) return;
        offerCreationScheduled = true;
        timerHandler.postDelayed(this::tryCreateOfferWhenSocketReady, delayMs);
    }

    private void tryCreateOfferWhenSocketReady() {
        offerCreationScheduled = false;
        if (isFinishing() || webRtcManager == null || answerReceived || offerCreationStarted) return;

        if (!socketManager.isConnected()) {
            if (offerCreateWaitAttempts++ < 10) {
                socketManager.connect();
                scheduleOfferCreation(500);
                return;
            }
            Log.e(TAG, "Socket disconnected before creating offer");
            Toast.makeText(this, "Mất kết nối, thử lại", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        offerCreateWaitAttempts = 0;
        offerCreationStarted = true;
        Log.d(TAG, "Creating offer...");
        webRtcManager.createOffer();
    }

    private void joinCallRoom() {
        try {
            JSONObject joinData = new JSONObject();
            joinData.put("sessionId", sessionId);
            socketManager.emit("join_call_room", joinData);
            Log.d(TAG, "Emitted join_call_room: " + sessionId);
        } catch (Exception e) {
            Log.e(TAG, "join_call_room error: " + e.getMessage());
        }
    }

    private void bindViews() {
        localVideoView   = findViewById(R.id.localVideoView);
        remoteVideoView  = findViewById(R.id.remoteVideoView);
        imgPartnerAvatar = findViewById(R.id.imgPartnerAvatar);
        tvCallTimer      = findViewById(R.id.tvCallTimer);
        tvMuteIcon      = findViewById(R.id.tvMuteIcon);
        tvCameraIcon    = findViewById(R.id.tvCameraIcon);
        chatPanel       = findViewById(R.id.chatPanel);
        chatMessages    = findViewById(R.id.chatMessages);
        scrollChat      = findViewById(R.id.scrollChat);
        etChatInput     = findViewById(R.id.etChatInput);

        tvPartnerName = findViewById(R.id.tvPartnerName);
        if (tvPartnerName != null) tvPartnerName.setText(
                partnerName != null ? partnerName : getString(R.string.video_connecting));

        loadPartnerInfo();

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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_MEDIA_PERMISSION) return;

        boolean granted = grantResults.length > 0;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                granted = false;
                break;
            }
        }

        if (granted) {
            startCallWhenReady();
        } else {
            Toast.makeText(this, "Cần cấp quyền Camera và Mic để gọi video", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void loadPartnerInfo() {
        if (partnerId == null) return;
        userApiService.getPublicProfile(partnerId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (isFinishing() || response.body() == null || response.body().getProfile() == null) return;
                String name   = response.body().getProfile().getFullName();
                String avatar = response.body().getProfile().getAvatar();
                if (tvPartnerName != null && name != null && !name.isEmpty()) {
                    tvPartnerName.setText(name);
                }
                if (imgPartnerAvatar != null && avatar != null && !avatar.isEmpty()) {
                    Glide.with(VideoCallActivity.this)
                            .load(ImageUtils.normalizeAvatar(avatar))
                            .circleCrop()
                            .into(imgPartnerAvatar);
                }
            }
            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Log.w(TAG, "loadPartnerInfo failed: " + t.getMessage());
            }
        });
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
                        localOfferJson = sdpJson;
                        offerRetryCount = 0;
                        emitOfferAndScheduleRetry();
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
                    if (isFinishing() || remoteAttached || remoteVideoView == null || webRtcManager == null) return;
                    remoteAttached = true;
                    webRtcManager.attachRemoteView(videoTrack, remoteVideoView);
                    if (imgPartnerAvatar != null) imgPartnerAvatar.setVisibility(View.GONE);
                    Log.d(TAG, "Remote video attached");
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

    private void emitOfferAndScheduleRetry() {
        if (!isCaller || localOfferJson == null || answerReceived || isFinishing()) return;

        socketManager.sendWebRtcOffer(sessionId, localOfferJson);
        Log.d(TAG, "Offer sent" + (offerRetryCount > 0 ? " retry=" + offerRetryCount : ""));

        if (offerRetryRunnable != null) {
            timerHandler.removeCallbacks(offerRetryRunnable);
        }

        offerRetryRunnable = () -> {
            if (answerReceived || isFinishing() || localOfferJson == null) return;
            if (offerRetryCount >= MAX_OFFER_RETRIES) return;
            offerRetryCount++;
            emitOfferAndScheduleRetry();
        };
        timerHandler.postDelayed(offerRetryRunnable, OFFER_RETRY_INTERVAL_MS);
    }

    private void stopOfferRetry() {
        if (offerRetryRunnable != null) {
            timerHandler.removeCallbacks(offerRetryRunnable);
            offerRetryRunnable = null;
        }
    }

    private void registerSocketListeners() {
        webRtcOfferListener = args -> {
            if (isCaller || webRtcManager == null || isFinishing()) return;
            try {
                JSONObject data  = (JSONObject) args[0];
                if (!isCurrentSession(data)) return;
                String offerJson = data.getJSONObject("offer").toString();
                webRtcManager.setRemoteOffer(offerJson, () -> {
                    if (!isFinishing() && webRtcManager != null) {
                        webRtcManager.createAnswer();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "webrtc_offer error: " + e.getMessage());
            }
        };
        socketManager.onWebRtcOffer(webRtcOfferListener);

        webRtcAnswerListener = args -> {
            if (!isCaller || webRtcManager == null || isFinishing()) return;
            try {
                JSONObject data   = (JSONObject) args[0];
                if (!isCurrentSession(data)) return;
                String answerJson = data.getJSONObject("answer").toString();
                answerReceived = true;
                stopOfferRetry();
                webRtcManager.setRemoteAnswer(answerJson);
            } catch (Exception e) {
                Log.e(TAG, "webrtc_answer error: " + e.getMessage());
            }
        };
        socketManager.onWebRtcAnswer(webRtcAnswerListener);

        iceCandidateListener = args -> {
            if (webRtcManager == null || isFinishing()) return;
            try {
                JSONObject data      = (JSONObject) args[0];
                if (!isCurrentSession(data)) return;
                String candidateJson = data.getJSONObject("candidate").toString();

                // Remote SDP chưa set thì xếp hàng ICE để xử lý sau
                webRtcManager.addRemoteIceCandidate(candidateJson);
            } catch (Exception e) {
                Log.e(TAG, "webrtc_ice_candidate error: " + e.getMessage());
            }
        };
        socketManager.onIceCandidate(iceCandidateListener);

        partnerDisconnectedListener = args -> runOnUiThread(() -> {
            Toast.makeText(this, "Đối tác đã rời cuộc gọi.", Toast.LENGTH_SHORT).show();
            endCall();
        });
        socketManager.onPartnerDisconnected(partnerDisconnectedListener);

        receiveMessageListener = args -> runOnUiThread(() -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String content  = data.optString("content", "");
                if (!content.isEmpty()) {
                    addChatBubble("Đối tác", content, false);
                }
            } catch (Exception e) {
                Log.e(TAG, "receive_message error: " + e.getMessage());
            }
        });
        socketManager.onReceiveMessage(receiveMessageListener);
    }

    private void unregisterSocketListeners() {
        if (webRtcOfferListener != null) {
            socketManager.off("webrtc_offer", webRtcOfferListener);
            webRtcOfferListener = null;
        }
        if (webRtcAnswerListener != null) {
            socketManager.off("webrtc_answer", webRtcAnswerListener);
            webRtcAnswerListener = null;
        }
        if (iceCandidateListener != null) {
            socketManager.off("webrtc_ice_candidate", iceCandidateListener);
            iceCandidateListener = null;
        }
        if (partnerDisconnectedListener != null) {
            socketManager.off("partner_disconnected", partnerDisconnectedListener);
            partnerDisconnectedListener = null;
        }
        if (receiveMessageListener != null) {
            socketManager.off("receive_message", receiveMessageListener);
            receiveMessageListener = null;
        }
    }

    private boolean isCurrentSession(JSONObject data) {
        String payloadSessionId = data.optString("sessionId", "");
        if (payloadSessionId.isEmpty() || payloadSessionId.equals(sessionId)) {
            return true;
        }
        Log.d(TAG, "Ignored signaling for session=" + payloadSessionId + ", current=" + sessionId);
        return false;
    }

    private void toggleMic() {
        if (webRtcManager == null) return;
        isMuted = !isMuted;
        webRtcManager.setMicEnabled(!isMuted);
        if (tvMuteIcon != null) tvMuteIcon.setImageResource(isMuted ? R.drawable.ic_mic_off : R.drawable.ic_mic);
        LinearLayout btnMute = findViewById(R.id.btnMute);
        if (btnMute != null) btnMute.setBackgroundResource(
            isMuted ? R.drawable.bg_ctrl_btn_red : R.drawable.bg_ctrl_btn);
        Toast.makeText(this, isMuted ? "Đã tắt mic" : "Đã bật mic", Toast.LENGTH_SHORT).show();
    }

    private void toggleCamera() {
        if (webRtcManager == null) return;
        isCameraOff = !isCameraOff;
        webRtcManager.setCameraEnabled(!isCameraOff);
        if (tvCameraIcon != null) tvCameraIcon.setImageResource(isCameraOff ? R.drawable.ic_videocam_off : R.drawable.ic_videocam);
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
            : android.graphics.Color.BLACK);
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
        if (endingCall) return;
        endingCall = true;
        stopHeartbeat();
        stopOfferRetry();

        socketManager.leaveQueue();

        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        unregisterSocketListeners();

        Intent intent = new Intent(this, RatingActivity.class);
        intent.putExtra("callDuration", elapsedSeconds);
        intent.putExtra("sessionId",    sessionId);
        intent.putExtra("partnerId",    partnerId);
        intent.putExtra("partnerName",  partnerName);
        startActivity(intent);
        finish();
    }
    private void startHeartbeat() {
        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (socketManager.isConnected()) {
                    socketManager.emit("heartbeat", null);
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
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        unregisterSocketListeners();
        stopOfferRetry();

        if (localVideoView != null) {
            localVideoView.release();
            localVideoView = null;
        }
        if (remoteVideoView != null) {
            remoteVideoView.release();
            remoteVideoView = null;
        }
        if (webRtcManager != null) {
            webRtcManager.release();
            webRtcManager = null;
        }
    }
}
