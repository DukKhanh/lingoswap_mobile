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

    @GET("api/user/friends")
    Call<List<Friend>> getFriends();

    @GET("api/user/friends/requests")
    Call<List<FriendRequest>> getFriendRequests();

    @POST("api/user/friends/{recipientId}/requests")
    Call<ApiResponse> sendFriendRequest(@Path("recipientId") String recipientId);

    @PATCH("api/user/friends/requests/{requestId}")
    Call<ApiResponse> respondFriendRequest(
            @Path("requestId") String requestId,
            @Body Map<String, String> body
    );

    @DELETE("api/user/friends/{friendId}")
    Call<ApiResponse> removeFriend(@Path("friendId") String friendId);

    @GET("api/user/friends/{targetUserId}/status")
    Call<FriendStatusResponse> checkFriendStatus(@Path("targetUserId") String targetUserId);

    @GET("api/user/friends/online")
    Call<Map<String, List<String>>> getOnlineFriends();
}
