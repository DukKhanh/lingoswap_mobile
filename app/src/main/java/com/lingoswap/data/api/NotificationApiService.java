package com.lingoswap.data.api;

import com.lingoswap.data.model.ApiResponse;
import com.lingoswap.data.model.Notification;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface NotificationApiService {
    @GET("api/user/notifications")
    Call<List<Notification>> getNotifications(
        @Query("page") int page,
        @Query("limit") int limit
    );

    @GET("api/user/notifications/unread-count")
    Call<Map<String, Integer>> getUnreadCount();

    @PATCH("api/user/notifications/{notificationId}/read")
    Call<ApiResponse> markAsRead(@Path("notificationId") String notifId);

    @PATCH("api/user/notifications/read-all")
    Call<ApiResponse> markAllAsRead();
}
