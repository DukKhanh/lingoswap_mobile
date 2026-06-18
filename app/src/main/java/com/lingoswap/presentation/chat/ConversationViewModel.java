package com.lingoswap.presentation.chat;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lingoswap.data.model.Conversation;
import com.lingoswap.data.repository.RepositoryCallback;
import com.lingoswap.domain.repository.ChatRepository;
import com.lingoswap.utils.SocketManager;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.socket.emitter.Emitter;

@HiltViewModel
public class ConversationViewModel extends ViewModel {

    private final ChatRepository repository;
    private final SocketManager  socketManager;

    public final MutableLiveData<List<Conversation>> conversations = new MutableLiveData<>();
    public final MutableLiveData<Boolean>            isLoading     = new MutableLiveData<>(false);
    public final MutableLiveData<String>             error         = new MutableLiveData<>();

    private final Emitter.Listener receiveListener;

    @Inject
    public ConversationViewModel(ChatRepository repository, SocketManager socketManager) {
        this.repository    = repository;
        this.socketManager = socketManager;

        receiveListener = args -> loadConversations();
        socketManager.onReceiveMessage(receiveListener);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (receiveListener != null) socketManager.off("receive_message", receiveListener);
    }

    public void loadConversations() {
        // postValue: có thể được gọi từ thread socket (receive_message) lẫn main thread.
        isLoading.postValue(true);
        repository.getAllConversations(new RepositoryCallback<List<Conversation>>() {
            @Override public void onSuccess(List<Conversation> data) {
                isLoading.postValue(false);
                conversations.postValue(data);
            }
            @Override public void onError(String message) {
                isLoading.postValue(false);
                error.postValue(message);
            }
        });
    }
}
