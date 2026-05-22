package com.lingoswap.data.api;

import com.lingoswap.data.model.ApiResponse;
import com.lingoswap.data.model.MatchSession;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MatchApiService {
    @GET("api/user/matches")
    Call<List<MatchSession>> getMatchHistory(
        @Query("page") int page,
        @Query("limit") int limit
    );

    @GET("api/user/matches/{sessionId}")
    Call<MatchSession> getMatchDetail(@Path("sessionId") String sessionId);

    @POST("api/user/matches/{sessionId}/review")
    Call<ApiResponse> reviewSession(
        @Path("sessionId") String sessionId,
        @Body Map<String, Object> body
    );
}
