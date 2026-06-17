package com.lingoswap.data.api;

import com.lingoswap.data.model.ApiResponse;
import com.lingoswap.data.model.Friend;
import com.lingoswap.data.model.FriendRequest;
import com.lingoswap.data.model.FriendStatusResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface FriendApiService {

    // GET /api/user/friends/friends - Lấy danh sách bạn bè
    @GET("api/user/friends/friends")
    Call<List<Friend>> getFriends();

    // GET /api/user/friends/friends/requests - Lấy danh sách yêu cầu kết bạn
    @GET("api/user/friends/friends/requests")
    Call<List<FriendRequest>> getFriendRequests();

    // POST /api/user/friends/friends/{recipientId}/request - Gửi yêu cầu kết bạn
    @POST("api/user/friends/friends/{recipientId}/request")
    Call<ApiResponse> sendFriendRequest(@Path("recipientId") String recipientId);

    // PATCH /api/user/friends/friends/{requestId}/response - Phản hồi yêu cầu kết bạn
    @PATCH("api/user/friends/friends/{requestId}/response")
    Call<ApiResponse> respondFriendRequest(
            @Path("requestId") String requestId,
            @Body Map<String, String> body
    );

    // DELETE /api/user/friends/friends/{friendId} - Hủy kết bạn
    @DELETE("api/user/friends/friends/{friendId}")
    Call<ApiResponse> removeFriend(@Path("friendId") String friendId);

    // GET /api/user/friends/friends/{targetUserId}/status - Kiểm tra trạng thái bạn bè
    @GET("api/user/friends/friends/{targetUserId}/status")
    Call<FriendStatusResponse> checkFriendStatus(@Path("targetUserId") String targetUserId);

    // GET /api/user/friends/online-friends - Lấy danh sách bạn bè đang online
    @GET("api/user/friends/online-friends")
    Call<Map<String, List<String>>> getOnlineFriends();
}
