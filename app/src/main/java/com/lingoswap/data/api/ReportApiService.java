package com.lingoswap.data.api;

import com.lingoswap.data.model.ApiResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ReportApiService {
    @POST("api/user/reports")
    Call<ApiResponse> sendReport(@Body Map<String, Object> body);
}
