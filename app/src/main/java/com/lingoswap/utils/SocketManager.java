package com.lingoswap.utils;

import android.util.Log;

import com.lingoswap.data.local.UserPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * SocketManager — quản lý kết nối Socket.IO với backend LingoSwap.
 *
 * FIX: Bỏ forceNew=true để tránh tạo transport mới mỗi lần connect,
 *      gây mất event khi socket đang trong quá trình handshake lại.
 */
@Singleton
public class SocketManager {

    private static final String TAG        = "SocketManager";
    private static final String SOCKET_URL = "http://10.0.2.2:5000";

    private Socket socket;
    private final UserPreferences userPreferences;

    // ─── Callback dùng cho joinQueueWhenReady() ───────────────────────────────
    // Lưu lại để có thể off() chính xác sau khi dùng xong (tránh memory leak)
    private Emitter.Listener pendingConnectListener = null;

    @Inject
    public SocketManager(UserPreferences userPreferences) {
        this.userPreferences = userPreferences;
    }

    // ─── Connect / Disconnect ─────────────────────────────────────────────────

    public void connect() {
        if (socket != null && socket.connected()) {
            Log.d(TAG, "Socket đã connected, bỏ qua connect()");
            return;
        }

        // Nếu socket tồn tại nhưng đang connecting (chưa connected) → không tạo mới
        if (socket != null) {
            Log.d(TAG, "Socket đang trong quá trình kết nối, bỏ qua connect()");
            return;
        }

        String token = userPreferences.getAccessToken();
        if (token == null) {
            Log.w(TAG, "Không có token, bỏ qua connect()");
            return;
        }

        try {
            IO.Options options = new IO.Options();
            // FIX #1: Bỏ forceNew=true — tránh tạo TCP transport mới mỗi lần,
            //         vốn là nguyên nhân gây drop event khi reconnect giữa chừng.
            options.forceNew             = false;
            options.reconnection         = true;
            options.reconnectionAttempts = 5;
            options.reconnectionDelay    = 2000;
            options.auth                 = Collections.singletonMap("token", token);

            socket = IO.socket(SOCKET_URL, options);
            registerBaseListeners();
            socket.connect();
            Log.d(TAG, "Socket đang kết nối...");
        } catch (URISyntaxException e) {
            Log.e(TAG, "Lỗi URL socket: " + e.getMessage());
        }
    }

    public void reconnectWithNewToken() {
        disconnect();
        connect();
    }

    public void disconnect() {
        if (socket != null) {
            socket.off();
            socket.disconnect();
            socket = null;
            pendingConnectListener = null;
            Log.d(TAG, "Socket đã ngắt kết nối");
        }
    }

    public boolean isConnected() {
        return socket != null && socket.connected();
    }

    // ─── Base Listeners ───────────────────────────────────────────────────────

    private void registerBaseListeners() {
        socket.on(Socket.EVENT_CONNECT, args ->
                Log.d(TAG, "✅ Socket connected | id=" + socket.id()));

        socket.on(Socket.EVENT_DISCONNECT, args ->
                Log.d(TAG, "❌ Socket disconnected | reason=" +
                        (args.length > 0 ? args[0].toString() : "unknown")));

        socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
            if (args.length > 0) Log.e(TAG, "⚠️ Socket connect error: " + args[0].toString());
        });

        // DEBUG: log toàn bộ event nhận được — XÓA KHI RELEASE
        socket.onAnyIncoming(args -> {
            if (args != null && args.length > 0) {
                String eventName = String.valueOf(args[0]);
                Log.d(TAG, "📨 [IN] event='" + eventName + "' args=" + Arrays.toString(args));
            }
        });
    }

    // ─── Queue helpers ────────────────────────────────────────────────────────

    public void joinMatchQueue(String language) {
        if (!isConnected()) {
            Log.w(TAG, "joinMatchQueue() bị bỏ qua — socket chưa connected");
            return;
        }
        try {
            JSONObject data = new JSONObject();
            data.put("language", language);
            socket.emit("join_queue", data);
            Log.d(TAG, "📤 Emitted join_queue: " + language);
        } catch (JSONException e) {
            Log.e(TAG, "Lỗi JSON joinMatchQueue: " + e.getMessage());
        }
    }

    /**
     * FIX #2: Gọi hàm này thay cho joinMatchQueue() trực tiếp.
     * Nếu socket đã connected → join ngay.
     * Nếu chưa → đăng ký one-shot listener trên EVENT_CONNECT rồi join sau.
     *
     * @param language ngôn ngữ đã được normalize
     * @param onReady  callback chạy trên background thread của Socket.IO
     *                 sau khi join_queue được emit thành công (có thể null)
     */
    public void joinQueueWhenReady(String language, Runnable onReady) {
        // Dọn listener cũ nếu còn sót
        if (pendingConnectListener != null) {
            socket.off(Socket.EVENT_CONNECT, pendingConnectListener);
            pendingConnectListener = null;
        }

        if (isConnected()) {
            joinMatchQueue(language);
            if (onReady != null) onReady.run();
            return;
        }

        Log.w(TAG, "Socket chưa connected — đăng ký one-shot listener để join sau");
        pendingConnectListener = args -> {
            Log.d(TAG, "🔁 Socket vừa connected, join queue ngay: " + language);
            joinMatchQueue(language);
            if (onReady != null) onReady.run();
            // One-shot: tự off sau khi dùng
            if (socket != null) socket.off(Socket.EVENT_CONNECT, pendingConnectListener);
            pendingConnectListener = null;
        };
        socket.on(Socket.EVENT_CONNECT, pendingConnectListener);
    }

    /** Huỷ pending connect listener (gọi khi Activity bị destroy trước khi connect xong). */
    public void cancelPendingQueueJoin() {
        if (pendingConnectListener != null && socket != null) {
            socket.off(Socket.EVENT_CONNECT, pendingConnectListener);
            pendingConnectListener = null;
            Log.d(TAG, "Đã huỷ pending queue join");
        }
    }

    public void leaveQueue() {
        if (!isConnected()) return;
        socket.emit("leave_queue");
        Log.d(TAG, "📤 Emitted leave_queue");
    }

    // ─── Match events ─────────────────────────────────────────────────────────

    public void onMatchFound(Emitter.Listener listener)          { on("match_found",          listener); }
    public void onWaitingStatus(Emitter.Listener listener)       { on("waiting_status",        listener); }
    public void onQueueTimeout(Emitter.Listener listener)        { on("queue_timeout",         listener); }
    public void onPartnerDisconnected(Emitter.Listener listener) { on("partner_disconnected",  listener); }
    public void onMatchError(Emitter.Listener listener)          { on("error",                 listener); }

    // ─── WebRTC events ────────────────────────────────────────────────────────

    public void sendWebRtcOffer(String sessionId, String sdpJson) {
        if (!isConnected()) return;
        try {
            JSONObject data = new JSONObject();
            data.put("sessionId", sessionId);
            data.put("offer", new JSONObject(sdpJson));
            socket.emit("webrtc_offer", data);
        } catch (JSONException e) {
            Log.e(TAG, "Lỗi emit webrtc_offer: " + e.getMessage());
        }
    }

    public void sendWebRtcAnswer(String sessionId, String sdpJson) {
        if (!isConnected()) return;
        try {
            JSONObject data = new JSONObject();
            data.put("sessionId", sessionId);
            data.put("answer", new JSONObject(sdpJson));
            socket.emit("webrtc_answer", data);
        } catch (JSONException e) {
            Log.e(TAG, "Lỗi emit webrtc_answer: " + e.getMessage());
        }
    }

    public void sendIceCandidate(String sessionId, String candidateJson) {
        if (!isConnected()) return;
        try {
            JSONObject data = new JSONObject();
            data.put("sessionId", sessionId);
            data.put("candidate", new JSONObject(candidateJson));
            socket.emit("webrtc_ice_candidate", data);
        } catch (JSONException e) {
            Log.e(TAG, "Lỗi emit webrtc_ice_candidate: " + e.getMessage());
        }
    }

    public void onWebRtcOffer(Emitter.Listener listener)   { on("webrtc_offer",        listener); }
    public void onWebRtcAnswer(Emitter.Listener listener)  { on("webrtc_answer",       listener); }
    public void onIceCandidate(Emitter.Listener listener)  { on("webrtc_ice_candidate",listener); }

    // ─── Chat events ──────────────────────────────────────────────────────────

    /**
     * Gửi tin nhắn text — event name phải khớp chatHandler.js: "send_message"
     */
    public void sendMessage(String partnerId, String content, String matchSessionId) {
        if (!isConnected()) { Log.w(TAG, "Socket chưa kết nối"); return; }
        try {
            JSONObject data = new JSONObject();
            data.put("partnerId",      partnerId);
            data.put("receiverId",     partnerId);
            data.put("content",        content);
            data.put("type",           "text");
            if (matchSessionId != null) data.put("matchSessionId", matchSessionId);
            socket.emit("send_message", data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onReceiveMessage(Emitter.Listener listener)    { on("receive_message",       listener); }
    public void onMessageSentSuccess(Emitter.Listener listener){ on("message_sent_success",  listener); }
    public void onPresenceUpdate(Emitter.Listener listener)    { on("presence_update",       listener); }

    // ─── Generic on/off ───────────────────────────────────────────────────────
    // ─── Generic emit/on/off ──────────────────────────────────────────────────

    public void emit(String event, Object data) {

        if (!isConnected()) {
            Log.w(TAG, "emit('" + event + "') bỏ qua — socket chưa connected");
            return;
        }

        if (data == null) {
            socket.emit(event);
        } else {
            socket.emit(event, data);
        }

        Log.d(TAG, "📤 emit: " + event);
    }

    public void on(String event, Emitter.Listener listener) {
        if (socket != null) socket.on(event, listener);
        else Log.w(TAG, "on('" + event + "') bị bỏ qua — socket null");
    }

    public void off(String event) {
        if (socket != null) socket.off(event);
    }

    public void offAll() {
        if (socket != null) {
            socket.off();
            // off() đã cover tất cả, dòng dưới dư nhưng giữ cho an toàn
            socket.off("message_sent_success");
        }
    }
}
