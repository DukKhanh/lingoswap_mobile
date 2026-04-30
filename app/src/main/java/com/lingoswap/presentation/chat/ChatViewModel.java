package com.lingoswap.presentation.chat;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lingoswap.data.model.Message;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ChatViewModel extends ViewModel {

    private final MutableLiveData<List<Message>> _messages = new MutableLiveData<>();
    public LiveData<List<Message>> messages = _messages;

    private final List<Message> messageList = new ArrayList<>();

    @Inject
    public ChatViewModel() {
        loadMockHistory();
    }

    private void loadMockHistory() {
        messageList.add(new Message("Xin chào! Bạn có muốn luyện tiếng Anh cùng mình không?", "10:20", false));
        messageList.add(new Message("Chào bạn! Mình rất vui được luyện cùng bạn 😊", "10:21", true));
        messageList.add(new Message("Great! Let's start. How long have you been learning English?", "10:21", false));
        messageList.add(new Message("I've been learning for about 2 years. My speaking is still weak.", "10:22", true));
        messageList.add(new Message("Đừng lo, luyện tập thường xuyên là bí quyết! 💪", "10:23", false));
        _messages.setValue(new ArrayList<>(messageList));
    }

    public void sendMessage(String text) {
        if (text == null || text.trim().isEmpty()) return;

        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        messageList.add(new Message(text, time, true));
        _messages.setValue(new ArrayList<>(messageList));

        // Simulate reply
        simulateReply();
    }

    private void simulateReply() {
        new android.os.Handler().postDelayed(() -> {
            String[] replies = {
                    "That's great! 👏",
                    "Bạn nói rất tốt!",
                    "Cảm ơn bạn nhé 😊",
                    "Let's keep practicing!",
                    "Tôi hiểu rồi, cảm ơn!"
            };
            String text = replies[(int) (Math.random() * replies.length)];
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            messageList.add(new Message(text, time, false));
            _messages.setValue(new ArrayList<>(messageList));
        }, 1500);
    }
}
