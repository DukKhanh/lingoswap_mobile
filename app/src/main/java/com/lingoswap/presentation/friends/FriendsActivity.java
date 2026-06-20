package com.lingoswap.presentation.friends;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.lingoswap.R;
import com.lingoswap.activities.HomeActivity;
import com.lingoswap.activities.MatchingActivity;
import com.lingoswap.activities.ProfileActivity;
import com.lingoswap.activities.VideoCallActivity;
import com.lingoswap.data.model.Friend;
import com.lingoswap.databinding.ActivityFriendsBinding;
import com.lingoswap.presentation.base.BaseActivity;
import com.lingoswap.presentation.chat.ChatActivity;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class FriendsActivity extends BaseActivity<ActivityFriendsBinding> {

    /** Truyền tab muốn mở sẵn: 0=All, 1=Online, 2=Requests. */
    public static final String EXTRA_OPEN_TAB = "open_tab";

    private FriendsViewModel viewModel;
    private FriendAdapter adapter;
    private int currentTab = 0;

    @Override
    protected ActivityFriendsBinding inflateBinding(LayoutInflater inflater) {
        return ActivityFriendsBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(FriendsViewModel.class);

        binding.btnFindNow.setOnClickListener(v ->
                com.lingoswap.activities.LanguageChooserDialog.newInstance()
                        .show(getSupportFragmentManager(), "LanguageChooserDialog"));

        setupRecyclerView();
        setupTabs();
        setupSearch();
        setupBottomNav();
    }

    private void setupRecyclerView() {
        adapter = new FriendAdapter(new FriendAdapter.Listener() {

            @Override
            public void onChat(Friend friend) {
                Intent intent = new Intent(FriendsActivity.this, ChatActivity.class);
                intent.putExtra(ChatActivity.EXTRA_FRIEND_ID, friend.id);
                intent.putExtra(ChatActivity.EXTRA_FRIEND_NAME, friend.fullName);
                intent.putExtra(ChatActivity.EXTRA_FRIEND_AVATAR, friend.avatar);
                intent.putExtra(ChatActivity.EXTRA_FRIEND_ONLINE, friend.isOnline());
                if (friend.conversationId != null) {
                    intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, friend.conversationId);
                }
                startActivity(intent);
            }

            @Override
            public void onCall(Friend friend) {
                // Gọi trực tiếp qua signaling (cần sessionId từ match_found),
                // KHÔNG mở thẳng VideoCallActivity (sẽ thiếu sessionId → đóng ngay).
                Intent intent = new Intent(FriendsActivity.this,
                        com.lingoswap.activities.OutgoingCallActivity.class);
                intent.putExtra(com.lingoswap.activities.OutgoingCallActivity.EXTRA_TARGET_ID, friend.id);
                intent.putExtra(com.lingoswap.activities.OutgoingCallActivity.EXTRA_TARGET_NAME, friend.fullName);
                startActivity(intent);
            }

            @Override
            public void onAccept(Friend friend) {
                // friend.id ở đây là friendshipId (đặt qua Friend.fromRequest), không phải userId.
                viewModel.acceptFriendRequest(friend.id);
            }

            @Override
            public void onReject(Friend friend) {
                viewModel.rejectFriendRequest(friend.id);
            }

            @Override
            public void onRemove(Friend friend) {
                viewModel.removeFriend(friend.id);
            }
        });

        binding.rvFriends.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFriends.setAdapter(adapter);
    }

    private void setupTabs() {
        binding.tabFriendAll.setOnClickListener(v -> switchTab(0));
        binding.tabFriendOnline.setOnClickListener(v -> switchTab(1));
        binding.tabFriendRequests.setOnClickListener(v -> switchTab(2));

        // Mở sẵn tab theo yêu cầu (vd từ thông báo lời mời kết bạn → tab Requests)
        int openTab = getIntent().getIntExtra(EXTRA_OPEN_TAB, 0);
        switchTab(openTab == 1 || openTab == 2 ? openTab : 0);
    }

    private void switchTab(int tab) {
        currentTab = tab;

        for (TextView t : new TextView[]{
                binding.tabFriendAll,
                binding.tabFriendOnline,
                binding.tabFriendRequests}) {
            t.setBackgroundResource(android.R.color.transparent);
            t.setTextColor(getResources().getColor(R.color.text_muted, getTheme()));
        }

        TextView active = tab == 0 ? binding.tabFriendAll
                : tab == 1 ? binding.tabFriendOnline
                : binding.tabFriendRequests;
        active.setBackgroundResource(R.drawable.bg_tab_active);
        active.setTextColor(getResources().getColor(R.color.white, getTheme()));

        viewModel.filterFriends(currentTab, binding.etSearchFriend.getText().toString());
    }

    private void setupSearch() {
        binding.etSearchFriend.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                viewModel.filterFriends(currentTab, s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void observeViewModel() {
        viewModel.friends.observe(this, friends -> {
            adapter.submitList(friends);
            boolean isEmpty = friends == null || friends.isEmpty();
            binding.rvFriends.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.layoutEmptyFriends.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        });

        viewModel.isLoading.observe(this, loading -> {
            if (binding.progressBar != null) {
                binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.error.observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.successMessage.observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.friendRequests.observe(this, requests -> {
            int count = requests != null ? requests.size() : 0;
            if (binding.tabFriendRequests != null && count > 0) {
                binding.tabFriendRequests.setText(
                        getString(R.string.tab_requests_with_count, count));
            } else if (binding.tabFriendRequests != null) {
                binding.tabFriendRequests.setText(getString(R.string.tab_requests));
            }
        });
    }

    private void setupBottomNav() {
        binding.navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
        binding.navMatch.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
        // Chat tab mở danh sách hội thoại, không phải ChatActivity rỗng.
        binding.navChat.setOnClickListener(v -> {
            startActivity(new Intent(this,
                    com.lingoswap.presentation.chat.ConversationListActivity.class));
            finish();
        });
        binding.navProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });
    }
}
