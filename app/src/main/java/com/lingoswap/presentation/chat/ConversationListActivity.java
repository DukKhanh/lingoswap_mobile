package com.lingoswap.presentation.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.lingoswap.R;
import com.lingoswap.activities.HomeActivity;
import com.lingoswap.activities.ProfileActivity;
import com.lingoswap.data.model.Conversation;
import com.lingoswap.presentation.friends.FriendsActivity;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * ConversationListActivity — tab Chat: danh sách cuộc trò chuyện (api 4.1).
 * Tap 1 dòng → mở ChatActivity với conversationId + partnerId.
 */
@AndroidEntryPoint
public class ConversationListActivity extends AppCompatActivity {

    private ConversationViewModel viewModel;
    private ConversationAdapter   adapter;

    private ListView    lvConversations;
    private ProgressBar progress;
    private View        layoutEmpty;
    private boolean     loadedOnce = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_list);

        viewModel = new ViewModelProvider(this).get(ConversationViewModel.class);

        lvConversations = findViewById(R.id.lvConversations);
        progress        = findViewById(R.id.progress);
        layoutEmpty     = findViewById(R.id.layoutEmpty);

        adapter = new ConversationAdapter(this);
        lvConversations.setAdapter(adapter);

        lvConversations.setOnItemClickListener((parent, view, position, id) -> {
            Conversation c = (Conversation) adapter.getItem(position);
            openChat(c);
        });

        setupBottomNav();
        observe();
    }

    @Override
    protected void onResume() {
        super.onResume();
        com.lingoswap.utils.ChatUnreadStore.clear();
        viewModel.loadConversations();
    }

    private void observe() {
        viewModel.conversations.observe(this, list -> {
            loadedOnce = true;
            adapter.setItems(list);
            boolean empty = list == null || list.isEmpty();
            layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            lvConversations.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
        viewModel.isLoading.observe(this, loading -> {
            // Chỉ hiện spinner ở lần tải đầu để tránh nhấp nháy khi quay lại màn.
            progress.setVisibility(Boolean.TRUE.equals(loading) && !loadedOnce
                    ? View.VISIBLE : View.GONE);
        });
        viewModel.error.observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openChat(Conversation c) {
        if (c == null) return;
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, c.getId());
        if (c.getPartner() != null) {
            intent.putExtra(ChatActivity.EXTRA_PARTNER_ID, c.getPartner().getId());
            intent.putExtra(ChatActivity.EXTRA_PARTNER_NAME, c.getPartner().getDisplayName());
            intent.putExtra(ChatActivity.EXTRA_FRIEND_ONLINE, c.getPartner().isOnline());
        }
        startActivity(intent);
    }

    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> { goTo(HomeActivity.class); });
        findViewById(R.id.navFriends).setOnClickListener(v -> { goTo(FriendsActivity.class); });
        findViewById(R.id.navMatch).setOnClickListener(v -> { goTo(HomeActivity.class); });
        findViewById(R.id.navChat).setOnClickListener(v -> { /* đang ở Chat */ });
        findViewById(R.id.navProfile).setOnClickListener(v -> { goTo(ProfileActivity.class); });
    }

    private void goTo(Class<?> target) {
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        finish();
    }
}
