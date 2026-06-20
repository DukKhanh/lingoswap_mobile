package com.lingoswap.presentation.chat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.lingoswap.R;
import com.lingoswap.activities.VideoCallActivity;
import com.lingoswap.data.api.UserApiService;
import com.lingoswap.data.local.UserPreferences;
import com.lingoswap.data.model.User;
import com.lingoswap.databinding.ActivityChatBinding;
import com.lingoswap.presentation.base.BaseActivity;
import com.lingoswap.utils.ImageUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** ChatActivity — màn hình nhắn tin 1-1. */
@AndroidEntryPoint
public class ChatActivity extends BaseActivity<ActivityChatBinding> {

    public static final String EXTRA_PARTNER_ID      = "partnerId";
    public static final String EXTRA_CONVERSATION_ID = "conversationId";
    public static final String EXTRA_PARTNER_NAME    = "partnerName";
    public static final String EXTRA_MATCH_SESSION_ID= "matchSessionId";
    
    // For compatibility with FriendsActivity
    public static final String EXTRA_FRIEND_ID       = "extra_friend_id";
    public static final String EXTRA_FRIEND_NAME     = EXTRA_PARTNER_NAME;
    public static final String EXTRA_FRIEND_AVATAR   = "extra_friend_avatar";
    public static final String EXTRA_FRIEND_ONLINE   = "extra_friend_online";

    private ChatViewModel viewModel;
    private ChatAdapter   adapter;

    private String partnerId;
    private String conversationId;
    private String partnerName;
    private String matchSessionId;
    private String partnerAvatarUrl;

    @Inject UserPreferences userPreferences;
    @Inject UserApiService  userApiService;

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) sendImage(uri);
            });

    @Override
    protected ActivityChatBinding inflateBinding(LayoutInflater inflater) {
        return ActivityChatBinding.inflate(inflater);
    }

    @Override
    protected boolean shouldTrackChatUnread() { return false; }

    @Override
    protected void onResume() {
        super.onResume();
        com.lingoswap.utils.ChatUnreadStore.clear();
    }

    @Override
    protected void setupViews() {
        if (viewModel == null) {
            viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        }
        
        readExtras();
        bindTopBar();
        setupRecyclerView();
        setupInput();

        if (conversationId != null) {
            viewModel.loadMessages(conversationId);
        } else if (partnerId != null) {
            // Mở từ Home/Friends (chưa có conversationId) → tự tìm theo partnerId.
            viewModel.loadMessagesByPartner(partnerId);
        }
    }

    @Override
    protected void observeViewModel() {
        if (viewModel == null) {
            viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        }

        viewModel.getMessages().observe(this, messages -> {
            if (messages == null) return;
            adapter.setMessages(messages);
            if (!messages.isEmpty()) {
                binding.rvMessages.scrollToPosition(messages.size() - 1);
            }
        });

        viewModel.getIncomingMessage().observe(this, msg -> {
            if (msg == null) return;
            adapter.addMessage(msg);
            scrollToBottom();
        });

        viewModel.getSentConfirmed().observe(this, msg -> {
            if (msg == null) return;
            adapter.addMessage(msg);
            scrollToBottom();
        });

        viewModel.getError().observe(this, errMsg -> {
            if (errMsg == null) return;
            Toast.makeText(this, errMsg, Toast.LENGTH_SHORT).show();
        });

        viewModel.getIsUploading().observe(this, uploading -> {
        });
    }

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

        // Avatar: ưu tiên ảnh truyền qua Intent, nếu không có thì fetch theo partnerId.
        String avatarExtra = getIntent().getStringExtra(EXTRA_FRIEND_AVATAR);
        if (hasRealAvatar(avatarExtra)) {
            loadAvatar(avatarExtra);
        } else if (partnerId != null) {
            fetchPartnerAvatar();
        }
        // Nếu không có avatar thật → giữ nguyên ảnh mặc định (ic_avatar_placeholder trong layout).

        boolean isOnline = getIntent().getBooleanExtra(EXTRA_FRIEND_ONLINE, false);
        binding.viewOnlineDot.setVisibility(isOnline ? View.VISIBLE : View.GONE);
        binding.tvPartnerStatus.setText(isOnline
                ? getString(R.string.label_online)
                : getString(R.string.label_offline));

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnVideoCall.setOnClickListener(v -> {
            if (partnerId == null) {
                Toast.makeText(this, "Không xác định được người dùng", Toast.LENGTH_SHORT).show();
                return;
            }
            // Gọi qua signaling thay vì mở thẳng VideoCall (sẽ thiếu sessionId).
            Intent intent = new Intent(this, com.lingoswap.activities.OutgoingCallActivity.class);
            intent.putExtra(com.lingoswap.activities.OutgoingCallActivity.EXTRA_TARGET_ID, partnerId);
            intent.putExtra(com.lingoswap.activities.OutgoingCallActivity.EXTRA_TARGET_NAME, partnerName);
            startActivity(intent);
        });

        // btnEmoji thực chất mở image picker để đính kèm ảnh.
        binding.btnEmoji.setOnClickListener(v -> {
            if (partnerId == null) {
                Toast.makeText(this, "Lỗi: không có partnerId", Toast.LENGTH_SHORT).show();
                return;
            }
            imagePicker.launch("image/*");
        });

        binding.btnReport.setOnClickListener(v -> {
            if (partnerId == null) {
                Toast.makeText(this, "Không xác định được người dùng", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, com.lingoswap.presentation.report.ReportActivity.class);
            intent.putExtra(com.lingoswap.presentation.report.ReportActivity.EXTRA_REPORTED_USER_ID, partnerId);
            if (conversationId != null) {
                intent.putExtra(com.lingoswap.presentation.report.ReportActivity.EXTRA_CONVERSATION_ID, conversationId);
            }
            startActivity(intent);
        });

        binding.btnCloseTranslate.setOnClickListener(v ->
                binding.layoutTranslateTip.setVisibility(View.GONE));
    }

    /** true nếu là avatar thật (không null/rỗng và không phải sentinel "default_avatar.png" của backend). */
    private boolean hasRealAvatar(String url) {
        return url != null && !url.isEmpty() && !url.equals("default_avatar.png");
    }

    private void loadAvatar(String url) {
        if (!hasRealAvatar(url)) return;
        partnerAvatarUrl = url;
        Glide.with(this)
                .load(ImageUtils.normalizeAvatar(url))
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .circleCrop()
                .into(binding.ivPartnerAvatar);
        // Avatar cạnh bong bóng tin nhắn đối phương.
        if (adapter != null) adapter.setPartnerAvatar(url);
    }

    /** Khi Intent không kèm avatar (vd mở từ danh sách hội thoại) → lấy từ public profile. */
    private void fetchPartnerAvatar() {
        userApiService.getPublicProfile(partnerId).enqueue(new Callback<User>() {
            @Override public void onResponse(Call<User> call, Response<User> response) {
                if (isFinishing() || response.body() == null
                        || response.body().getProfile() == null) return;
                String avatar = response.body().getProfile().getAvatar();
                String name   = response.body().getProfile().getFullName();
                if (hasRealAvatar(avatar)) loadAvatar(avatar);
                if (("LingoSwap User".equals(partnerName) || partnerName == null)
                        && name != null && !name.isEmpty()) {
                    binding.tvPartnerName.setText(name);
                }
            }
            @Override public void onFailure(Call<User> call, Throwable t) { }
        });
    }

    private void setupRecyclerView() {
        adapter = new ChatAdapter(userPreferences.getUserId(), (msg, position) -> {
            binding.layoutTranslateTip.setVisibility(View.VISIBLE);
            binding.tvTranslationResult.setText(
                    getString(R.string.chat_translating_msg, msg.getContent()));
        });

        adapter.setPartnerAvatar(partnerAvatarUrl);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(llm);
        binding.rvMessages.setAdapter(adapter);
    }

    private void setupInput() {
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

    private void scrollToBottom() {
        int count = adapter.getItemCount();
        if (count > 0) binding.rvMessages.smoothScrollToPosition(count - 1);
    }

    private void sendImage(Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) {
                Toast.makeText(this, "Không đọc được ảnh đã chọn", Toast.LENGTH_SHORT).show();
                return;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int n;
            while ((n = in.read(chunk)) != -1) buffer.write(chunk, 0, n);
            in.close();
            byte[] bytes = buffer.toByteArray();

            String mime = getContentResolver().getType(uri);
            if (mime == null) mime = "image/*";
            MediaType type = MediaType.parse(mime);

            RequestBody imageBody = RequestBody.create(type, bytes);
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", "chat.jpg", imageBody);

            MediaType textType = MediaType.parse("text/plain");
            RequestBody partnerBody = RequestBody.create(textType, partnerId);
            RequestBody sessionBody = RequestBody.create(textType,
                    matchSessionId == null ? "" : matchSessionId);

            viewModel.sendImage(imagePart, partnerBody, sessionBody);
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi gửi ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
