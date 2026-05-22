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

        binding.btnAddFriend.setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.coming_soon), Toast.LENGTH_SHORT).show());

        binding.btnFindNow.setOnClickListener(v ->
                startActivity(new Intent(this, MatchingActivity.class)));

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
                // Pass conversationId so ChatActivity can load existing messages directly
                if (friend.conversationId != null) {
                    intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, friend.conversationId);
                }
                startActivity(intent);
            }

            @Override
            public void onCall(Friend friend) {
                Intent intent = new Intent(FriendsActivity.this, VideoCallActivity.class);
                intent.putExtra("partnerName", friend.fullName);
                intent.putExtra("partnerId", friend.id);
                startActivity(intent);
            }

            @Override
            public void onAccept(Friend friend) {
                // friend.id here is the friendshipId (set via Friend.fromRequest)
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

        // Highlight default tab
        switchTab(0);
    }

    private void switchTab(int tab) {
        currentTab = tab;

        // Reset all tab styles
        for (TextView t : new TextView[]{
                binding.tabFriendAll,
                binding.tabFriendOnline,
                binding.tabFriendRequests}) {
            t.setBackgroundResource(android.R.color.transparent);
            t.setTextColor(getResources().getColor(R.color.text_muted, getTheme()));
        }

        // Highlight active tab
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
        // Main list (friends or requests depending on tab)
        viewModel.friends.observe(this, friends -> {
            adapter.submitList(friends);
            boolean isEmpty = friends == null || friends.isEmpty();
            binding.rvFriends.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.layoutEmptyFriends.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        });

        // Loading indicator
        viewModel.isLoading.observe(this, loading -> {
            if (binding.progressBar != null) {
                binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            }
        });

        // Error messages
        viewModel.error.observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        // Success messages (accept/reject/remove)
        viewModel.successMessage.observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        // Update badge on friend requests tab if needed
        viewModel.friendRequests.observe(this, requests -> {
            int count = requests != null ? requests.size() : 0;
            // Optional: show badge on tabFriendRequests
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
        binding.navMatch.setOnClickListener(v ->
                startActivity(new Intent(this, MatchingActivity.class)));
        binding.navChat.setOnClickListener(v ->
                startActivity(new Intent(this, ChatActivity.class)));
        binding.navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }
}
