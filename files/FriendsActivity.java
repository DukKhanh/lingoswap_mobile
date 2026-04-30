package com.example.lingoswap;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * FriendsActivity – Màn hình danh sách bạn bè
 * XML: activity_friends.xml
 */
public class FriendsActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────
    private RecyclerView rvFriends;
    private LinearLayout layoutEmptyFriends;
    private EditText etSearchFriend;
    private TextView tabAll, tabOnline, tabRequests;
    private ImageView btnAddFriend;

    // ── Data ───────────────────────────────────────────────────────
    private FriendAdapter adapter;
    private final List<Friend> allFriends   = new ArrayList<>();
    private final List<Friend> shownFriends = new ArrayList<>();

    /** Trạng thái tab: 0 = Tất cả | 1 = Online | 2 = Lời mời */
    private int currentTab = 0;

    // ── Lifecycle ──────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        bindViews();
        setupRecyclerView();
        setupTabs();
        setupSearch();
        setupBottomNav();
        loadMockData();
    }

    // ── Bind ───────────────────────────────────────────────────────
    private void bindViews() {
        rvFriends          = findViewById(R.id.rvFriends);
        layoutEmptyFriends = findViewById(R.id.layoutEmptyFriends);
        etSearchFriend     = findViewById(R.id.etSearchFriend);
        tabAll             = findViewById(R.id.tabFriendAll);
        tabOnline          = findViewById(R.id.tabFriendOnline);
        tabRequests        = findViewById(R.id.tabFriendRequests);
        btnAddFriend       = findViewById(R.id.btnAddFriend);

        btnAddFriend.setOnClickListener(v ->
                Toast.makeText(this, "Tìm bạn bè mới – coming soon!", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnFindNow).setOnClickListener(v ->
                startActivity(new Intent(this, MatchingActivity.class)));
    }

    // ── RecyclerView ───────────────────────────────────────────────
    private void setupRecyclerView() {
        adapter = new FriendAdapter(shownFriends, new FriendAdapter.Listener() {
            @Override
            public void onChat(Friend friend) {
                Intent intent = new Intent(FriendsActivity.this, ChatActivity.class);
                intent.putExtra(ChatActivity.EXTRA_FRIEND_NAME,  friend.name);
                intent.putExtra(ChatActivity.EXTRA_FRIEND_LANGS, friend.langs);
                intent.putExtra(ChatActivity.EXTRA_FRIEND_ONLINE, friend.isOnline);
                startActivity(intent);
            }

            @Override
            public void onCall(Friend friend) {
                Intent intent = new Intent(FriendsActivity.this, VideoCallActivity.class);
                intent.putExtra("partnerName", friend.name);
                startActivity(intent);
            }
        });

        rvFriends.setLayoutManager(new LinearLayoutManager(this));
        rvFriends.setAdapter(adapter);
    }

    // ── Tabs ───────────────────────────────────────────────────────
    private void setupTabs() {
        tabAll.setOnClickListener(v      -> switchTab(0));
        tabOnline.setOnClickListener(v   -> switchTab(1));
        tabRequests.setOnClickListener(v -> switchTab(2));
    }

    private void switchTab(int tab) {
        currentTab = tab;

        // Reset visual state
        for (TextView t : new TextView[]{tabAll, tabOnline, tabRequests}) {
            t.setBackgroundResource(android.R.color.transparent);
            t.setTextColor(getResources().getColor(R.color.text_muted));
        }
        TextView active = tab == 0 ? tabAll : tab == 1 ? tabOnline : tabRequests;
        active.setBackgroundResource(R.drawable.bg_tab_active);
        active.setTextColor(getResources().getColor(R.color.white));

        filterAndShow(etSearchFriend.getText().toString());
    }

    // ── Search ─────────────────────────────────────────────────────
    private void setupSearch() {
        etSearchFriend.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                filterAndShow(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ── Filter ─────────────────────────────────────────────────────
    private void filterAndShow(String query) {
        shownFriends.clear();
        String q = query.trim().toLowerCase();

        for (Friend f : allFriends) {
            boolean tabOk = currentTab == 0
                    || (currentTab == 1 && f.isOnline)
                    || (currentTab == 2 && f.isPending);
            boolean searchOk = q.isEmpty() || f.name.toLowerCase().contains(q);
            if (tabOk && searchOk) shownFriends.add(f);
        }

        adapter.notifyDataSetChanged();

        boolean isEmpty = shownFriends.isEmpty();
        rvFriends.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        layoutEmptyFriends.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    // ── Mock data ──────────────────────────────────────────────────
    private void loadMockData() {
        allFriends.add(new Friend("Minh Tuan",  "VI → EN", true,  false));
        allFriends.add(new Friend("Sarah Lee",  "EN → VI", true,  false));
        allFriends.add(new Friend("Yuki Tanaka","JP → VI", false, false));
        allFriends.add(new Friend("Carlos R.",  "ES → EN", false, false));
        allFriends.add(new Friend("Linh Pham",  "VI → FR", true,  false));
        // Pending request
        allFriends.add(new Friend("Alex K.",    "EN → VI", false, true));

        filterAndShow("");
    }

    // ── Bottom Nav ─────────────────────────────────────────────────
    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
        // navFriends → current activity, no-op
        findViewById(R.id.navMatch).setOnClickListener(v ->
                startActivity(new Intent(this, MatchingActivity.class)));
        findViewById(R.id.navChat).setOnClickListener(v ->
                startActivity(new Intent(this, ChatActivity.class)));
        findViewById(R.id.navProfile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }

    // ══════════════════════════════════════════════════════════════
    //  Model
    // ══════════════════════════════════════════════════════════════
    public static class Friend {
        public final String  name;
        public final String  langs;
        public final boolean isOnline;
        public final boolean isPending;

        public Friend(String name, String langs, boolean isOnline, boolean isPending) {
            this.name      = name;
            this.langs     = langs;
            this.isOnline  = isOnline;
            this.isPending = isPending;
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  Adapter
    // ══════════════════════════════════════════════════════════════
    public static class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.VH> {

        public interface Listener {
            void onChat(Friend friend);
            void onCall(Friend friend);
        }

        private final List<Friend> list;
        private final Listener     listener;

        public FriendAdapter(List<Friend> list, Listener listener) {
            this.list     = list;
            this.listener = listener;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_friend, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Friend f = list.get(position);

            h.tvName.setText(f.name);
            h.tvLangs.setText(f.langs);
            h.tvStatus.setText(f.isPending ? "Lời mời kết bạn" : f.isOnline ? "Online" : "Offline");
            h.viewOnlineDot.setVisibility(f.isOnline ? View.VISIBLE : View.GONE);

            h.btnChat.setOnClickListener(v -> listener.onChat(f));
            h.btnCall.setOnClickListener(v -> listener.onCall(f));
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView  tvName, tvLangs, tvStatus;
            View      viewOnlineDot;
            ImageView btnChat, btnCall;

            VH(View v) {
                super(v);
                tvName       = v.findViewById(R.id.tvFriendName);
                tvLangs      = v.findViewById(R.id.tvFriendLangs);
                tvStatus     = v.findViewById(R.id.tvFriendStatus);
                viewOnlineDot= v.findViewById(R.id.viewOnlineDot);
                btnChat      = v.findViewById(R.id.btnFriendChat);
                btnCall      = v.findViewById(R.id.btnFriendCall);
            }
        }
    }
}
