package com.lingoswap.data.repository;

import com.lingoswap.data.api.ChatApiService;
import com.lingoswap.data.model.Conversation;
import com.lingoswap.data.model.Message;
import com.lingoswap.domain.repository.ChatRepository;

import org.json.JSONObject;

import java.util.List;

import javax.inject.Inject;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatRepositoryImpl implements ChatRepository {
    private final ChatApiService apiService;

    @Inject
    public ChatRepositoryImpl(ChatApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public void getAllConversations(RepositoryCallback<List<Conversation>> callback) {
        apiService.getAllConversations().enqueue(new Callback<List<Conversation>>() {
            @Override
            public void onResponse(Call<List<Conversation>> call, Response<List<Conversation>> response) {
                if (response.isSuccessful()) callback.onSuccess(response.body());
                else callback.onError(parseError(response));
            }
            @Override
            public void onFailure(Call<List<Conversation>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    @Override
    public void getMessages(String conversationId, int page, int limit, RepositoryCallback<List<Message>> callback) {
        apiService.getMessages(conversationId, page, limit).enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                if (response.isSuccessful()) callback.onSuccess(response.body());
                else callback.onError(parseError(response));
            }
            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    @Override
    public void uploadImage(MultipartBody.Part image, RequestBody partnerId, RequestBody matchSessionId, RepositoryCallback<Message> callback) {
        apiService.uploadImage(image, partnerId, matchSessionId).enqueue(new Callback<Message>() {
            @Override
            public void onResponse(Call<Message> call, Response<Message> response) {
                if (response.isSuccessful()) callback.onSuccess(response.body());
                else callback.onError(parseError(response));
            }
            @Override
            public void onFailure(Call<Message> call, Throwable t) {
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
