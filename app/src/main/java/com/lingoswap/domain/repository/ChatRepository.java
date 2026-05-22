package com.lingoswap.domain.repository;

import com.lingoswap.data.model.Conversation;
import com.lingoswap.data.model.Message;
import com.lingoswap.data.repository.RepositoryCallback;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public interface ChatRepository {
    void getAllConversations(RepositoryCallback<List<Conversation>> callback);
    void getMessages(String conversationId, int page, int limit, RepositoryCallback<List<Message>> callback);
    void uploadImage(MultipartBody.Part image, RequestBody partnerId, RequestBody matchSessionId, RepositoryCallback<Message> callback);
}
