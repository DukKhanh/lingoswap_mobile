package com.lingoswap.data.repository;

import com.lingoswap.data.api.FriendApiService;
import com.lingoswap.data.model.ApiResponse;
import com.lingoswap.data.model.Friend;
import com.lingoswap.data.model.FriendRequest;
import com.lingoswap.data.model.FriendStatusResponse;
import com.lingoswap.domain.repository.FriendRepository;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Response;

public class FriendRepositoryImpl implements FriendRepository {

    private final FriendApiService apiService;

    @Inject
    public FriendRepositoryImpl(FriendApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public void getFriends(Callback<List<Friend>> callback) {
        apiService.getFriends().enqueue(new retrofit2.Callback<List<Friend>>() {
            @Override
            public void onResponse(Call<List<Friend>> call, Response<List<Friend>> response) {
                if (response.isSuccessful() && response.body() != null)
                    callback.onSuccess(response.body());
                else
                    callback.onError(parseError(response));
            }
            @Override
            public void onFailure(Call<List<Friend>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    @Override
    public void getFriendRequests(Callback<List<FriendRequest>> callback) {
        apiService.getFriendRequests().enqueue(new retrofit2.Callback<List<FriendRequest>>() {
            @Override
            public void onResponse(Call<List<FriendRequest>> call, Response<List<FriendRequest>> response) {
                if (response.isSuccessful() && response.body() != null)
                    callback.onSuccess(response.body());
                else
                    callback.onError(parseError(response));
            }
            @Override
            public void onFailure(Call<List<FriendRequest>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    @Override
    public void sendFriendRequest(String recipientId, Callback<ApiResponse> callback) {
        apiService.sendFriendRequest(recipientId).enqueue(new retrofit2.Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful()) callback.onSuccess(response.body());
                else callback.onError(parseError(response));
            }
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    @Override
    public void respondFriendRequest(String requestId, String status, Callback<ApiResponse> callback) {
        Map<String, String> body = new HashMap<>();
        body.put("status", status);
        apiService.respondFriendRequest(requestId, body).enqueue(new retrofit2.Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful()) callback.onSuccess(response.body());
                else callback.onError(parseError(response));
            }
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    @Override
    public void removeFriend(String friendId, Callback<ApiResponse> callback) {
        apiService.removeFriend(friendId).enqueue(new retrofit2.Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful()) callback.onSuccess(response.body());
                else callback.onError(parseError(response));
            }
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    @Override
    public void checkFriendStatus(String targetUserId, Callback<FriendStatusResponse> callback) {
        apiService.checkFriendStatus(targetUserId).enqueue(new retrofit2.Callback<FriendStatusResponse>() {
            @Override
            public void onResponse(Call<FriendStatusResponse> call, Response<FriendStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null)
                    callback.onSuccess(response.body());
                else
                    callback.onError(parseError(response));
            }
            @Override
            public void onFailure(Call<FriendStatusResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    @Override
    public void getOnlineFriends(Callback<Map<String, List<String>>> callback) {
        apiService.getOnlineFriends().enqueue(new retrofit2.Callback<Map<String, List<String>>>() {
            @Override
            public void onResponse(Call<Map<String, List<String>>> call, Response<Map<String, List<String>>> response) {
                if (response.isSuccessful() && response.body() != null)
                    callback.onSuccess(response.body());
                else
                    callback.onError(parseError(response));
            }
            @Override
            public void onFailure(Call<Map<String, List<String>>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    private String parseError(Response<?> response) {
        try {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
            JSONObject json = new JSONObject(errorBody);
            return json.optString("error", "Lỗi không xác định");
        } catch (Exception e) {
            return "HTTP " + response.code();
        }
    }
}
