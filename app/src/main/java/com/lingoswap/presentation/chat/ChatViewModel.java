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

    private final MutableLiveData<List<Message>> messages   = new MutableLiveData<>();
    private final MutableLiveData<Message>  incomingMessage = new MutableLiveData<>();
    private final MutableLiveData<Message>  sentConfirmed   = new MutableLiveData<>();
    private final MutableLiveData<String>   error           = new MutableLiveData<>();
    private final MutableLiveData<Boolean>  isUploading     = new MutableLiveData<>(false);

    @Inject
    public ChatViewModel(ChatApiService chatApiService, SocketManager socketManager) {
        this.chatApiService = chatApiService;
        this.socketManager  = socketManager;
        listenSocket();
    }

    public LiveData<List<Message>> getMessages()        { return messages; }
    public LiveData<Message>       getIncomingMessage() { return incomingMessage; }
    public LiveData<Message>       getSentConfirmed()   { return sentConfirmed; }
    public LiveData<String>        getError()           { return error; }
    public LiveData<Boolean>       getIsUploading()     { return isUploading; }

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
     * Khi mở chat từ Home/Friends (chỉ có partnerId, chưa có conversationId):
     * tìm hội thoại khớp partnerId trong danh sách rồi tải tin nhắn.
     */
    public void loadMessagesByPartner(String partnerId) {
        if (partnerId == null) return;
        chatApiService.getAllConversations().enqueue(new Callback<java.util.List<com.lingoswap.data.model.Conversation>>() {
            @Override
            public void onResponse(Call<java.util.List<com.lingoswap.data.model.Conversation>> call,
                                   Response<java.util.List<com.lingoswap.data.model.Conversation>> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                for (com.lingoswap.data.model.Conversation c : response.body()) {
                    if (c.getPartner() != null && partnerId.equals(c.getPartner().getId())) {
                        loadMessages(c.getId());
                        return;
                    }
                }
                // Chưa từng chat với người này → chưa có hội thoại, để trống là đúng.
            }
            @Override
            public void onFailure(Call<java.util.List<com.lingoswap.data.model.Conversation>> call, Throwable t) { }
        });
    }

    /** Gửi text qua socket; xác nhận trả về qua sentConfirmed LiveData. */
    public void sendMessage(String partnerId, String content, String matchSessionId) {
        socketManager.sendMessage(partnerId, content, matchSessionId);
    }

    /** Upload ảnh qua REST (multipart); kết quả trả về qua sentConfirmed LiveData. */
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

    private io.socket.emitter.Emitter.Listener receiveListener;
    private io.socket.emitter.Emitter.Listener sentListener;

    private void listenSocket() {
        receiveListener = args -> {
            Message msg = parseSocketMessage(args);
            if (msg != null) incomingMessage.postValue(msg);
        };
        socketManager.onReceiveMessage(receiveListener);

        sentListener = args -> {
            Message msg = parseSocketMessage(args);
            if (msg != null) sentConfirmed.postValue(msg);
        };
        socketManager.onMessageSentSuccess(sentListener);
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

    @Override
    protected void onCleared() {
        super.onCleared();
        // Gỡ đúng listener của màn chat này (không đụng listener của VideoCall/màn khác).
        if (receiveListener != null) socketManager.off("receive_message", receiveListener);
        if (sentListener != null)    socketManager.off("message_sent_success", sentListener);
    }
}
