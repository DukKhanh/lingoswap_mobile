package com.lingoswap.data.api;

import com.lingoswap.data.model.DashboardResponse;
import com.lingoswap.data.model.SearchUserResponse;
import com.lingoswap.data.model.User;

import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface UserApiService {
    @GET("api/users/me")
    Call<User> getProfile();

    @PUT("api/users/me")
    Call<Map<String, Object>> updateProfile(@Body Map<String, Object> body);

    @Multipart
    @PUT("api/users/me/avatar")
    Call<Map<String, String>> uploadAvatar(@Part MultipartBody.Part avatar);

    @GET("api/users/dashboard")
    Call<DashboardResponse> getDashboard();

    @GET("api/users")
    Call<SearchUserResponse> searchUsers(
            @Query("q") String query,
            @Query("page") int page,
            @Query("limit") int limit
    );

    @GET("api/users/{id}")
    Call<User> getPublicProfile(@Path("id") String userId);
}
