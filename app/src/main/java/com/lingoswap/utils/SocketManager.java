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

@Singleton
public class SocketManager {

    private static final String TAG        = "SocketManager";
    // ISSUE 1 FIX: Use Production URL
    private static final String SOCKET_URL = "http://10.0.2.2:5000/";

    private Socket socket;
    private final UserPreferences userPreferences;

    private Emitter.Listener pendingConnectListener = null;

    @Inject
    public SocketManager(UserPreferences userPreferences) {
        this.userPreferences = userPreferences;
    }

    public void connect() {
        if (socket != null && socket.connected()) {
            return;
        }

        if (socket != null) {
            return;
        }

        String token = userPreferences.getAccessToken();
        if (token == null) {
            return;
        }

        try {
            IO.Options options = new IO.Options();
            options.forceNew             = false;
            options.reconnection         = true;
            options.reconnectionAttempts = 5;
            options.reconnectionDelay    = 2000;
            options.auth                 = Collections.singletonMap("token", token);

            socket = IO.socket(SOCKET_URL, options);
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
            socket.off();
            socket.disconnect();
            socket = null;
            pendingConnectListener = null;
        }
    }

    public boolean isConnected() {
        return socket != null && socket.connected();
    }

    private void registerBaseListeners() {
        socket.on(Socket.EVENT_CONNECT, args ->
                Log.d(TAG, "✅ Socket connected | id=" + socket.id()));

        socket.on(Socket.EVENT_DISCONNECT, args ->
                Log.d(TAG, "❌ Socket disconnected | reason=" +
                        (args.length > 0 ? args[0].toString() : "unknown")));

        socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
            if (args.length > 0) Log.e(TAG, "⚠️ Socket connect error: " + args[0].toString());
        });
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

    /** Unregisters all listeners from the socket. */
    public void offAll() {
        if (socket != null) {
            socket.off();
        }
    }
}
