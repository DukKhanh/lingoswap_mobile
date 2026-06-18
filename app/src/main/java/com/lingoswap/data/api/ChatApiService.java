package com.lingoswap.data.api;

import com.lingoswap.data.model.Conversation;
import com.lingoswap.data.model.Message;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ChatApiService {
    @GET("api/user/conversations")
    Call<List<Conversation>> getAllConversations();

    @GET("api/user/conversations/{conversationId}")
    Call<List<Message>> getMessages(
        @Path("conversationId") String conversationId,
        @Query("page") int page,
        @Query("limit") int limit
    );

    @Multipart
    @POST("api/user/conversations/images")
    Call<Message> uploadImage(
        @Part MultipartBody.Part image,
        @Part("partnerId") RequestBody partnerId,
        @Part("matchSessionId") RequestBody matchSessionId // nullable
    );
}
