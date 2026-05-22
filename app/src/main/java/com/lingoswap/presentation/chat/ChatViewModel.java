package com.lingoswap.presentation.chat;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lingoswap.data.api.ChatApiService;
import com.lingoswap.data.model.Message;
import com.lingoswap.data.remote.TimestampDeserializer;
import com.lingoswap.utils.SocketManager;

import org.json.JSONObject;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class ChatViewModel extends ViewModel {

    private final ChatApiService chatApiService;
    private final SocketManager  socketManager;

    // Gson nhận cả TimestampField dạng object lẫn string ISO
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Message.TimestampField.class, new TimestampDeserializer())
            .create();

    // ── LiveData ──────────────────────────────────────────────────────────────

    /** Danh sách tin nhắn lịch sử (load lần đầu) */
    private final MutableLiveData<List<Message>> messages   = new MutableLiveData<>();

    /** Tin nhắn mới đến từ đối phương (socket receive_message) */
    private final MutableLiveData<Message>  incomingMessage = new MutableLiveData<>();

    /** Xác nhận tin nhắn mình gửi đã được server lưu (socket message_sent_success) */
    private final MutableLiveData<Message>  sentConfirmed   = new MutableLiveData<>();

    /** Lỗi (network / socket) */
    private final MutableLiveData<String>   error           = new MutableLiveData<>();

    /** Trạng thái upload ảnh */
    private final MutableLiveData<Boolean>  isUploading     = new MutableLiveData<>(false);

    // ── Constructor ───────────────────────────────────────────────────────────

    @Inject
    public ChatViewModel(ChatApiService chatApiService, SocketManager socketManager) {
        this.chatApiService = chatApiService;
        this.socketManager  = socketManager;
        listenSocket();
    }

    // ── Public getters ────────────────────────────────────────────────────────

    public LiveData<List<Message>> getMessages()        { return messages; }
    public LiveData<Message>       getIncomingMessage() { return incomingMessage; }
    public LiveData<Message>       getSentConfirmed()   { return sentConfirmed; }
    public LiveData<String>        getError()           { return error; }
    public LiveData<Boolean>       getIsUploading()     { return isUploading; }

    // ── Actions ───────────────────────────────────────────────────────────────

    /**
     * Load lịch sử tin nhắn của 1 cuộc hội thoại qua REST API.
     * @param conversationId ID cuộc hội thoại
     */
    public void loadMessages(String conversationId) {
        chatApiService.getMessages(conversationId, 1, 50)
                .enqueue(new Callback<List<Message>>() {
                    @Override
                    public void onResponse(Call<List<Message>> call,
                                           Response<List<Message>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            messages.postValue(response.body());
                        } else {
                            error.postValue("Không tải được tin nhắn (HTTP " + response.code() + ")");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Message>> call, Throwable t) {
                        error.postValue("Lỗi kết nối: " + t.getMessage());
                    }
                });
    }

    /**
     * Gửi tin nhắn text qua Socket.IO.
     * Kết quả phản hồi sẽ đến qua sentConfirmed LiveData.
     */
    public void sendMessage(String partnerId, String content, String matchSessionId) {
        socketManager.sendMessage(partnerId, content, matchSessionId);
    }

    /**
     * Upload ảnh qua REST API (multipart).
     * Kết quả phản hồi đến qua sentConfirmed LiveData.
     */
    public void sendImage(MultipartBody.Part image,
                          RequestBody partnerId,
                          RequestBody matchSessionId) {
        isUploading.setValue(true);
        chatApiService.uploadImage(image, partnerId, matchSessionId)
                .enqueue(new Callback<Message>() {
                    @Override
                    public void onResponse(Call<Message> call, Response<Message> response) {
                        isUploading.postValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            sentConfirmed.postValue(response.body());
                        } else {
                            error.postValue("Gửi ảnh thất bại (HTTP " + response.code() + ")");
                        }
                    }

                    @Override
                    public void onFailure(Call<Message> call, Throwable t) {
                        isUploading.postValue(false);
                        error.postValue("Lỗi tải ảnh: " + t.getMessage());
                    }
                });
    }

    // ── Socket listeners ──────────────────────────────────────────────────────

    private void listenSocket() {
        // Tin nhắn đến từ đối phương
        socketManager.onReceiveMessage(args -> {
            Message msg = parseSocketMessage(args);
            if (msg != null) incomingMessage.postValue(msg);
        });

        // Xác nhận tin nhắn mình vừa gửi đã được lưu vào DB
        socketManager.onMessageSentSuccess(args -> {
            Message msg = parseSocketMessage(args);
            if (msg != null) sentConfirmed.postValue(msg);
        });
    }

    private Message parseSocketMessage(Object[] args) {
        try {
            if (args == null || args.length == 0) return null;
            JSONObject json = (JSONObject) args[0];
            return gson.fromJson(json.toString(), Message.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCleared() {
        super.onCleared();
        socketManager.offAll();
    }
}
