package com.lingoswap.presentation.chat;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.lingoswap.R;
import com.lingoswap.activities.VideoCallActivity;
import com.lingoswap.data.local.UserPreferences;
import com.lingoswap.databinding.ActivityChatBinding;
import com.lingoswap.presentation.base.BaseActivity;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * ChatActivity — màn hình nhắn tin 1-1.
 */
@AndroidEntryPoint
public class ChatActivity extends BaseActivity<ActivityChatBinding> {

    // ── Intent keys ───────────────────────────────────────────────────────────
    public static final String EXTRA_PARTNER_ID      = "partnerId";
    public static final String EXTRA_CONVERSATION_ID = "conversationId";
    public static final String EXTRA_PARTNER_NAME    = "partnerName";
    public static final String EXTRA_MATCH_SESSION_ID= "matchSessionId";
    
    // For compatibility with FriendsActivity
    public static final String EXTRA_FRIEND_ID       = "extra_friend_id";
    public static final String EXTRA_FRIEND_NAME     = EXTRA_PARTNER_NAME;
    public static final String EXTRA_FRIEND_AVATAR   = "extra_friend_avatar";
    public static final String EXTRA_FRIEND_ONLINE   = "extra_friend_online";

    // ── Fields ────────────────────────────────────────────────────────────────
    private ChatViewModel viewModel;
    private ChatAdapter   adapter;

    private String partnerId;
    private String conversationId;
    private String partnerName;
    private String matchSessionId;

    @Inject UserPreferences userPreferences;

    // ── BaseActivity ──────────────────────────────────────────────────────────

    @Override
    protected ActivityChatBinding inflateBinding(LayoutInflater inflater) {
        return ActivityChatBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        // Initialize viewModel here (after super.onCreate has been called in BaseActivity)
        if (viewModel == null) {
            viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        }
        
        readExtras();
        bindTopBar();
        setupRecyclerView();
        setupInput();

        if (conversationId != null) {
            viewModel.loadMessages(conversationId);
        }
    }

    @Override
    protected void observeViewModel() {
        // Ensure viewModel is initialized
        if (viewModel == null) {
            viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        }

        // Lịch sử tin nhắn (REST)
        viewModel.getMessages().observe(this, messages -> {
            if (messages == null) return;
            adapter.setMessages(messages);
            if (!messages.isEmpty()) {
                binding.rvMessages.scrollToPosition(messages.size() - 1);
            }
        });

        // Tin nhắn mới từ đối phương (socket receive_message)
        viewModel.getIncomingMessage().observe(this, msg -> {
            if (msg == null) return;
            adapter.addMessage(msg);
            scrollToBottom();
        });

        // Xác nhận tin nhắn mình gửi đã được server lưu (socket message_sent_success)
        viewModel.getSentConfirmed().observe(this, msg -> {
            if (msg == null) return;
            adapter.addMessage(msg); // thêm vào list với _id chính thức từ DB
            scrollToBottom();
        });

        // Lỗi
        viewModel.getError().observe(this, errMsg -> {
            if (errMsg == null) return;
            Toast.makeText(this, errMsg, Toast.LENGTH_SHORT).show();
        });

        // Upload ảnh đang chạy
        viewModel.getIsUploading().observe(this, uploading -> {
            // Optional: Show loading state
        });
    }

    // ── Setup helpers ─────────────────────────────────────────────────────────

    private void readExtras() {
        partnerId      = getIntent().getStringExtra(EXTRA_PARTNER_ID);
        if (partnerId == null) partnerId = getIntent().getStringExtra(EXTRA_FRIEND_ID);
        
        conversationId = getIntent().getStringExtra(EXTRA_CONVERSATION_ID);
        
        partnerName    = getIntent().getStringExtra(EXTRA_PARTNER_NAME);
        if (partnerName == null) partnerName = "LingoSwap User";
        
        matchSessionId = getIntent().getStringExtra(EXTRA_MATCH_SESSION_ID);
    }

    private void bindTopBar() {
        binding.tvPartnerName.setText(partnerName);

        // Online status từ Intent
        boolean isOnline = getIntent().getBooleanExtra(EXTRA_FRIEND_ONLINE, false);
        binding.viewOnlineDot.setVisibility(isOnline ? View.VISIBLE : View.GONE);
        binding.tvPartnerStatus.setText(isOnline
                ? getString(R.string.label_online)
                : getString(R.string.label_offline));

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnVideoCall.setOnClickListener(v -> {
            Intent intent = new Intent(this, VideoCallActivity.class);
            intent.putExtra("partnerName", partnerName);
            intent.putExtra("partnerId", partnerId);
            startActivity(intent);
        });

        binding.btnEmoji.setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.coming_soon), Toast.LENGTH_SHORT).show());

        binding.btnCloseTranslate.setOnClickListener(v ->
                binding.layoutTranslateTip.setVisibility(View.GONE));
    }

    private void setupRecyclerView() {
        adapter = new ChatAdapter(userPreferences.getUserId(), (msg, position) -> {
            // Long press → hiện translate bar với nội dung tin nhắn
            binding.layoutTranslateTip.setVisibility(View.VISIBLE);
            binding.tvTranslationResult.setText(
                    getString(R.string.chat_translating_msg, msg.getContent()));
        });

        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(llm);
        binding.rvMessages.setAdapter(adapter);
    }

    private void setupInput() {
        // Nút Send mờ khi chưa có text
        binding.btnSend.setAlpha(0.45f);

        binding.etMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                boolean hasText = !TextUtils.isEmpty(s.toString().trim());
                binding.btnSend.setAlpha(hasText ? 1f : 0.45f);
            }
        });

        binding.btnSend.setOnClickListener(v -> {
            String text = binding.etMessage.getText().toString().trim();
            if (text.isEmpty()) return;
            if (partnerId == null) {
                Toast.makeText(this, "Lỗi: không có partnerId", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.sendMessage(partnerId, text, matchSessionId);
            binding.etMessage.setText("");
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void scrollToBottom() {
        int count = adapter.getItemCount();
        if (count > 0) binding.rvMessages.smoothScrollToPosition(count - 1);
    }
}
