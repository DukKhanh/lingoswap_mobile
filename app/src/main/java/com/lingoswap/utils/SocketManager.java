package com.lingoswap.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.lingoswap.activities.HomeActivity;
import com.lingoswap.data.local.UserPreferences;
import com.lingoswap.utils.NotificationHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

@Singleton
public class SocketManager {

    private static final String TAG        = "SocketManager";
    private static final String SOCKET_URL = "http://10.0.2.2:5000/";

    private Socket socket;
    private String connectedToken;
    private final UserPreferences userPreferences;
    private final Context appContext;
    private int notifId = 1000;

    private Emitter.Listener pendingConnectListener = null;
    private Runnable onReconnectAction = null;

    @Inject
    public SocketManager(@ApplicationContext Context context, UserPreferences userPreferences) {
        this.appContext      = context;
        this.userPreferences = userPreferences;
    }

    public void connect() {
        String token = userPreferences.getAccessToken();
        if (token == null) {
            return;
        }

        // Token đã đổi (đăng nhập tài khoản khác) → bỏ socket cũ để tạo lại theo token mới.
        if (socket != null && !token.equals(connectedToken)) {
            disconnect();
        }

        if (socket != null && socket.connected()) {
            return;
        }

        if (socket != null) {
            return;
        }

        try {
            IO.Options options = new IO.Options();
            // Chỉ dùng WebSocket, bỏ qua long-polling → kết nối nhanh và ổn định hơn.
            options.transports           = new String[]{ "websocket" };
            options.forceNew             = false;
            options.upgrade              = false;
            options.reconnection         = true;
            options.reconnectionAttempts = Integer.MAX_VALUE;
            options.reconnectionDelay    = 1000;
            options.reconnectionDelayMax = 5000;
            options.randomizationFactor  = 0.5;
            options.timeout              = 20000;
            options.auth                 = Collections.singletonMap("token", token);

            socket = IO.socket(SOCKET_URL, options);
            connectedToken = token;
            registerBaseListeners();
            socket.connect();
            Log.d(TAG, "Socket connecting to production...");
        } catch (URISyntaxException e) {
            Log.e(TAG, "Socket URL error: " + e.getMessage());
        }
    }

    public void reconnectWithNewToken() {
        disconnect();
        connect();
    }

    public void disconnect() {
        if (socket != null) {
            socket.io().off();
            socket.off();
            socket.disconnect();
            socket = null;
            connectedToken = null;
            pendingConnectListener = null;
        }
    }

    public boolean isConnected() {
        return socket != null && socket.connected();
    }

    private void registerBaseListeners() {
        socket.on(Socket.EVENT_CONNECT, args -> {
            Log.d(TAG, "✅ Socket connected | id=" + socket.id());
            emitHeartbeat();
        });

        socket.on(Socket.EVENT_DISCONNECT, args ->
                Log.d(TAG, "❌ Socket disconnected | reason=" +
                        (args.length > 0 ? args[0].toString() : "unknown")));

        socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
            if (args.length > 0) Log.e(TAG, "⚠️ Socket connect error: " + args[0].toString());
        });

        // Sự kiện reconnect nằm ở tầng Manager (socket.io()), không phải trên socket.
        socket.io().on(io.socket.client.Manager.EVENT_RECONNECT, args -> {
            Log.d(TAG, "🔄 Socket reconnected after drop");
            if (onReconnectAction != null) onReconnectAction.run();
        });
        socket.io().on(io.socket.client.Manager.EVENT_RECONNECT_ATTEMPT, args ->
                Log.d(TAG, "… reconnecting"));

        socket.on("new_notification", args -> handleNewNotification(args));
        socket.on("direct_match_offer", args -> handleIncomingCall(args));
    }

    private void handleIncomingCall(Object[] args) {
        try {
            if (args == null || args.length == 0) return;
            JSONObject json = (JSONObject) args[0];
            String callerId = json.optString("callerId", "");
            String message  = json.optString("message", "Bạn có một cuộc gọi đến.");
            if (callerId.isEmpty()) return;

            Intent intent = new Intent(appContext,
                    com.lingoswap.activities.IncomingCallActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("callerId", callerId);
            intent.putExtra("message",  message);
            appContext.startActivity(intent);

            NotificationHelper.show(appContext, NotificationHelper.CHANNEL_CALL,
                    "Cuộc gọi đến", message, intent, notifId++);
        } catch (Exception e) {
            Log.e(TAG, "handleIncomingCall error: " + e.getMessage());
        }
    }

    public void requestDirectMatch(String targetUserId) {
        if (!isConnected()) connect();
        if (!isConnected()) return;
        try {
            JSONObject data = new JSONObject();
            data.put("targetUserId", targetUserId);
            socket.emit("direct_match_request", data);
        } catch (JSONException e) {
            Log.e(TAG, "requestDirectMatch error: " + e.getMessage());
        }
    }

    public void respondDirectMatch(String callerId, boolean accept) {
        if (!isConnected()) return;
        try {
            JSONObject data = new JSONObject();
            data.put("callerId", callerId);
            data.put("accept", accept);
            socket.emit("direct_match_response", data);
        } catch (JSONException e) {
            Log.e(TAG, "respondDirectMatch error: " + e.getMessage());
        }
    }

    public void onDirectMatchOffer(Emitter.Listener listener)    { on("direct_match_offer",    listener); }
    public void onDirectMatchRejected(Emitter.Listener listener) { on("direct_match_rejected", listener); }
    public void onDirectMatchError(Emitter.Listener listener)    { on("direct_match_error",    listener); }

    private void handleNewNotification(Object[] args) {
        try {
            if (args == null || args.length == 0) return;
            JSONObject json = (JSONObject) args[0];
            String type    = json.optString("type", "");
            String content = json.optString("content", "Bạn có thông báo mới");
            String title   = "LingoSwap";
            JSONObject sender = json.optJSONObject("senderId");
            if (sender != null) {
                JSONObject profile = sender.optJSONObject("profile");
                if (profile != null) title = profile.optString("fullName", title);
            }
            Intent intent = new Intent(appContext, HomeActivity.class);
            NotificationHelper.show(appContext, NotificationHelper.channelForType(type),
                    title, content, intent, notifId++);
        } catch (Exception e) {
            Log.e(TAG, "handleNewNotification error: " + e.getMessage());
        }
    }

    public void onNewNotification(Emitter.Listener listener) { on("new_notification", listener); }

    /** Emit heartbeat 30s/lần để giữ trạng thái online (api-doc 8.6). */
    public void emitHeartbeat() {
        if (isConnected()) socket.emit("heartbeat");
    }

    public void joinMatchQueue(String language) {
        if (!isConnected()) return;
        try {
            JSONObject data = new JSONObject();
            data.put("language", language);
            socket.emit("join_queue", data);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error joinMatchQueue: " + e.getMessage());
        }
    }

    public void joinQueueWhenReady(String language, Runnable onReady) {
        // Bảo đảm socket tồn tại (tránh NPE khi vào Matching mà chưa connect).
        if (socket == null) {
            connect();
        }
        if (socket == null) {
            Log.w(TAG, "joinQueueWhenReady: socket vẫn null (thiếu token?) — bỏ qua");
            return;
        }

        if (pendingConnectListener != null) {
            socket.off(Socket.EVENT_CONNECT, pendingConnectListener);
            pendingConnectListener = null;
        }

        if (isConnected()) {
            joinMatchQueue(language);
            if (onReady != null) onReady.run();
            return;
        }

        pendingConnectListener = args -> {
            joinMatchQueue(language);
            if (onReady != null) onReady.run();
            if (socket != null) socket.off(Socket.EVENT_CONNECT, pendingConnectListener);
            pendingConnectListener = null;
        };
        socket.on(Socket.EVENT_CONNECT, pendingConnectListener);
    }

    /** Hành động chạy lại mỗi khi socket reconnect (vd: vào lại queue). */
    public void setOnReconnect(Runnable action) {
        this.onReconnectAction = action;
    }

    public void cancelPendingQueueJoin() {
        if (pendingConnectListener != null && socket != null) {
            socket.off(Socket.EVENT_CONNECT, pendingConnectListener);
            pendingConnectListener = null;
        }
    }

    public void leaveQueue() {
        if (!isConnected()) return;
        socket.emit("leave_queue");
    }

    public void onMatchFound(Emitter.Listener listener)          { on("match_found",          listener); }
    public void onWaitingStatus(Emitter.Listener listener)       { on("waiting_status",        listener); }
    public void onQueueTimeout(Emitter.Listener listener)        { on("queue_timeout",         listener); }
    public void onPartnerDisconnected(Emitter.Listener listener) { on("partner_disconnected",  listener); }
    public void onMatchError(Emitter.Listener listener)          { on("error",                 listener); }

    public void sendWebRtcOffer(String sessionId, String sdpJson) {
        if (!isConnected()) return;
        try {
            JSONObject data = new JSONObject();
            data.put("sessionId", sessionId);
            data.put("offer", new JSONObject(sdpJson));
            socket.emit("webrtc_offer", data);
        } catch (JSONException e) {
            Log.e(TAG, "emit webrtc_offer error: " + e.getMessage());
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
            Log.e(TAG, "emit webrtc_answer error: " + e.getMessage());
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
            Log.e(TAG, "emit webrtc_ice_candidate error: " + e.getMessage());
        }
    }

    public void onWebRtcOffer(Emitter.Listener listener)   { on("webrtc_offer",        listener); }
    public void onWebRtcAnswer(Emitter.Listener listener)  { on("webrtc_answer",       listener); }
    public void onIceCandidate(Emitter.Listener listener)  { on("webrtc_ice_candidate",listener); }

    public void sendMessage(String partnerId, String content, String matchSessionId) {
        if (!isConnected()) return;
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

    public void emit(String event, Object data) {
        if (!isConnected()) return;
        if (data == null) socket.emit(event);
        else socket.emit(event, data);
    }

    public void on(String event, Emitter.Listener listener) {
        if (socket != null) socket.on(event, listener);
    }

    public void off(String event) {
        if (socket != null) socket.off(event);
    }

    /** Gỡ đúng MỘT listener của event (tránh xoá listener của màn hình khác dùng chung socket). */
    public void off(String event, Emitter.Listener listener) {
        if (socket != null) socket.off(event, listener);
    }

    /** Unregisters all listeners from the socket. */
    public void offAll() {
        if (socket != null) {
            socket.off();
        }
    }
}
