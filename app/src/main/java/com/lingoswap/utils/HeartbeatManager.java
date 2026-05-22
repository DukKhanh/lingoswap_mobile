package com.lingoswap.utils;

import android.os.Handler;
import android.os.Looper;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class HeartbeatManager {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final long HEARTBEAT_INTERVAL = 30000; // 30 seconds
    private boolean isRunning = false;

    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                // Heartbeat logic here (e.g., sending a ping to server via socket)
                handler.postDelayed(this, HEARTBEAT_INTERVAL);
            }
        }
    };

    @Inject
    public HeartbeatManager() {
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
