package com.lingoswap.utils;

import android.os.Handler;
import android.os.Looper;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class HeartbeatManager {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final long HEARTBEAT_INTERVAL = 30000;
    private boolean isRunning = false;

    private final SocketManager socketManager;

    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                socketManager.emitHeartbeat();
                handler.postDelayed(this, HEARTBEAT_INTERVAL);
            }
        }
    };

    @Inject
    public HeartbeatManager(SocketManager socketManager) {
        this.socketManager = socketManager;
    }

    public void start() {
        if (!isRunning) {
            isRunning = true;
            handler.post(heartbeatRunnable);
        }
    }

    public void stop() {
        isRunning = false;
        handler.removeCallbacks(heartbeatRunnable);
    }
}
