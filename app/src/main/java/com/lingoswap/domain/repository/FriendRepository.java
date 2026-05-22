package com.lingoswap.domain.repository;
import com.lingoswap.data.model.*;
import java.util.List; import java.util.Map;

public interface FriendRepository {
    interface Callback<T> { void onSuccess(T data); void onError(String message); }

    void getFriends(Callback<List<Friend>> callback);
    void getFriendRequests(Callback<List<FriendRequest>> callback);
    void sendFriendRequest(String recipientId, Callback<ApiResponse> callback);
    void respondFriendRequest(String requestId, String status, Callback<ApiResponse> callback);
    void removeFriend(String friendId, Callback<ApiResponse> callback);
    void checkFriendStatus(String targetUserId, Callback<FriendStatusResponse> callback);
    void getOnlineFriends(Callback<Map<String, List<String>>> callback);
}
