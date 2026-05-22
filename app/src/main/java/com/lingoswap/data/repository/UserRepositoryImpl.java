package com.lingoswap.data.repository;

import com.lingoswap.data.api.UserApiService;
import com.lingoswap.data.model.DashboardResponse;
import com.lingoswap.data.model.User;
import com.lingoswap.domain.repository.UserRepository;

import org.json.JSONObject;

import java.util.Map;

import javax.inject.Inject;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepositoryImpl implements UserRepository {
    private final UserApiService apiService;

    @Inject
    public UserRepositoryImpl(UserApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public void getProfile(RepositoryCallback<User> callback) {
        apiService.getProfile().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful()) callback.onSuccess(response.body());
                else callback.onError(parseError(response));
            }
            @Override
            public void onFailure(Call<User> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    @Override
    public void updateProfile(Map<String, Object> body, RepositoryCallback<Map<String, Object>> callback) {
        apiService.updateProfile(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) callback.onSuccess(response.body());
                else callback.onError(parseError(response));
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    @Override
    public void uploadAvatar(MultipartBody.Part avatar, RepositoryCallback<Map<String, String>> callback) {
        apiService.uploadAvatar(avatar).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful()) callback.onSuccess(response.body());
                else callback.onError(parseError(response));
            }
            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    @Override
    public void getDashboard(RepositoryCallback<DashboardResponse> callback) {
        apiService.getDashboard().enqueue(new Callback<DashboardResponse>() {
            @Override
            public void onResponse(Call<DashboardResponse> call, Response<DashboardResponse> response) {
                if (response.isSuccessful()) callback.onSuccess(response.body());
                else callback.onError(parseError(response));
            }
            @Override
            public void onFailure(Call<DashboardResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    @Override
    public void searchUsers(String query, int page, int limit, RepositoryCallback<Map<String, Object>> callback) {
        apiService.searchUsers(query, page, limit).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) callback.onSuccess(response.body());
                else callback.onError(parseError(response));
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    @Override
    public void getPublicProfile(String userId, RepositoryCallback<User> callback) {
        apiService.getPublicProfile(userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful()) callback.onSuccess(response.body());
                else callback.onError(parseError(response));
            }
            @Override
            public void onFailure(Call<User> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    private String parseError(Response<?> response) {
        try {
            JSONObject json = new JSONObject(response.errorBody().string());
            return json.optString("error", "Lỗi không xác định");
        } catch (Exception e) {
            return "HTTP " + response.code();
        }
    }
}
