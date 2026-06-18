package com.lingoswap.domain.repository;

import com.lingoswap.data.model.DashboardResponse;
import com.lingoswap.data.model.SearchUserResponse;
import com.lingoswap.data.model.User;
import com.lingoswap.data.repository.RepositoryCallback;

import java.util.Map;

import okhttp3.MultipartBody;

public interface UserRepository {
    void getProfile(RepositoryCallback<User> callback);
    void updateProfile(Map<String, Object> body, RepositoryCallback<Map<String, Object>> callback);
    void uploadAvatar(MultipartBody.Part avatar, RepositoryCallback<Map<String, String>> callback);
    void getDashboard(RepositoryCallback<DashboardResponse> callback);

    void searchUsers(String query, int page, int limit, RepositoryCallback<SearchUserResponse> callback);

    void getPublicProfile(String userId, RepositoryCallback<User> callback);
}
