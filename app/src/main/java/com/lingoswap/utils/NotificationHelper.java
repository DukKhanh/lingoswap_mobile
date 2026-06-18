package com.lingoswap.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.lingoswap.R;

/** Tạo NotificationChannel và đẩy System Notification khi nhận sự kiện realtime. */
public class NotificationHelper {

    public static final String CHANNEL_MESSAGE = "channel_message";
    public static final String CHANNEL_FRIEND  = "channel_friend";
    public static final String CHANNEL_CALL    = "channel_call";
    public static final String CHANNEL_GENERAL = "channel_general";

    private static boolean channelsCreated = false;

    public static void createChannels(Context context) {
        if (channelsCreated) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm == null) return;
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL_MESSAGE, "Tin nhắn", NotificationManager.IMPORTANCE_HIGH));
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL_FRIEND, "Kết bạn", NotificationManager.IMPORTANCE_DEFAULT));
            NotificationChannel call = new NotificationChannel(
                    CHANNEL_CALL, "Cuộc gọi đến", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(call);
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL_GENERAL, "Thông báo chung", NotificationManager.IMPORTANCE_DEFAULT));
        }
        channelsCreated = true;
    }

    public static String channelForType(String type) {
        if (type == null) return CHANNEL_GENERAL;
        switch (type) {
            case "friend_request":
            case "friend_accepted":
                return CHANNEL_FRIEND;
            case "new_message":
            case "receive_message":
                return CHANNEL_MESSAGE;
            case "incoming_call":
            case "direct_match_offer":
                return CHANNEL_CALL;
            default:
                return CHANNEL_GENERAL;
        }
    }

    public static void show(Context context, String channelId, String title,
                            String content, Intent contentIntent, int id) {
        createChannels(context);

        PendingIntent pi = null;
        if (contentIntent != null) {
            contentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
            pi = PendingIntent.getActivity(context, id, contentIntent, flags);
        }

        NotificationCompat.Builder b = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle(title != null ? title : "LingoSwap")
                .setContentText(content)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        if (pi != null) b.setContentIntent(pi);

        try {
            NotificationManagerCompat.from(context).notify(id, b.build());
        } catch (SecurityException ignored) {
            // Thiếu quyền POST_NOTIFICATIONS (Android 13+) — bỏ qua, sẽ xin quyền ở Home.
        }
    }
}
