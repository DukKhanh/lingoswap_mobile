package com.lingoswap.presentation.friends;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lingoswap.R;
import com.lingoswap.activities.MatchingActivity;
import com.lingoswap.activities.VideoCallActivity;
import com.lingoswap.activities.HomeActivity;
import com.lingoswap.activities.ProfileActivity;
import com.lingoswap.presentation.chat.ChatActivity;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class FriendsActivity extends AppCompatActivity {

    private RecyclerView rvFriends;
    private LinearLayout layoutEmptyFriends;
    private EditText etSearchFriend;
    private TextView tabAll, tabOnline, tabRequests;
    private ImageView btnAddFriend;

    private FriendsViewModel viewModel;
    private FriendAdapter adapter;
    private int currentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        viewModel = new ViewModelProvider(this).get(FriendsViewModel.class);

        bindViews();
        setupRecyclerView();
        setupTabs();
        setupSearch();
        setupBottomNav();
        observeViewModel();
    }

    private void bindViews() {
        rvFriends = findViewById(R.id.rvFriends);
        layoutEmptyFriends = findViewById(R.id.layoutEmptyFriends);
        etSearchFriend = findViewById(R.id.etSearchFriend);
        tabAll = findViewById(R.id.tabFriendAll);
        tabOnline = findViewById(R.id.tabFriendOnline);
        tabRequests = findViewById(R.id.tabFriendRequests);
        btnAddFriend = findViewById(R.id.btnAddFriend);

        btnAddFriend.setOnClickListener(v ->
                Toast.makeText(this, "Tìm bạn bè mới – coming soon!", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnFindNow).setOnClickListener(v ->
                startActivity(new Intent(this, MatchingActivity.class)));
    }

    private void setupRecyclerView() {
        adapter = new FriendAdapter(new FriendAdapter.Listener() {
            @Override
            public void onChat(com.lingoswap.data.model.Friend friend) {
                Intent intent = new Intent(FriendsActivity.this, ChatActivity.class);
                intent.putExtra(ChatActivity.EXTRA_FRIEND_NAME, friend.name);
                intent.putExtra(ChatActivity.EXTRA_FRIEND_LANGS, friend.langs);
                intent.putExtra(ChatActivity.EXTRA_FRIEND_ONLINE, friend.isOnline);
                startActivity(intent);
            }

            @Override
            public void onCall(com.lingoswap.data.model.Friend friend) {
                Intent intent = new Intent(FriendsActivity.this, VideoCallActivity.class);
                intent.putExtra("partnerName", friend.name);
                startActivity(intent);
            }
        });

        rvFriends.setLayoutManager(new LinearLayoutManager(this));
        rvFriends.setAdapter(adapter);
    }

    private void setupTabs() {
        tabAll.setOnClickListener(v -> switchTab(0));
        tabOnline.setOnClickListener(v -> switchTab(1));
        tabRequests.setOnClickListener(v -> switchTab(2));
    }

    private void switchTab(int tab) {
        currentTab = tab;
        for (TextView t : new TextView[]{tabAll, tabOnline, tabRequests}) {
            t.setBackgroundResource(android.R.color.transparent);
            t.setTextColor(getResources().getColor(R.color.text_muted));
        }
        TextView active = tab == 0 ? tabAll : tab == 1 ? tabOnline : tabRequests;
        active.setBackgroundResource(R.drawable.bg_tab_active);
        active.setTextColor(getResources().getColor(R.color.white));

        viewModel.filterFriends(currentTab, etSearchFriend.getText().toString());
    }

    private void setupSearch() {
        etSearchFriend.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                viewModel.filterFriends(currentTab, s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void observeViewModel() {
        viewModel.friends.observe(this, friends -> {
            adapter.submitList(friends);
            boolean isEmpty = friends.isEmpty();
            rvFriends.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            layoutEmptyFriends.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        });
    }

    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
        findViewById(R.id.navMatch).setOnClickListener(v ->
                startActivity(new Intent(this, MatchingActivity.class)));
        findViewById(R.id.navChat).setOnClickListener(v ->
                startActivity(new Intent(this, ChatActivity.class)));
        findViewById(R.id.navProfile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }
}
