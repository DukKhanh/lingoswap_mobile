package com.lingoswap.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.lingoswap.R;
import com.lingoswap.data.local.UserPreferences;
import com.lingoswap.databinding.ActivityHomeBinding;
import com.lingoswap.presentation.base.BaseActivity;
import com.lingoswap.presentation.chat.ChatActivity;
import com.lingoswap.presentation.friends.FriendsActivity;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * HomeActivity — màn hình chính.
 *
 * Thay đổi:
 *  - Bỏ nút dark/light toggle (chỉ còn trong ProfileActivity)
 *  - Friend list: load từ API thật qua FriendsViewModel (hoặc giữ mock khi demo)
 *  - Nút chat mở FriendsActivity để chọn người nhắn tin
 */
@AndroidEntryPoint
public class HomeActivity extends BaseActivity<ActivityHomeBinding> {

    @Inject UserPreferences userPreferences;

    private String pendingLanguage;

    // ── Mô hình dữ liệu người bạn (giữ cho demo, sẽ thay bằng API) ─────────
    static class Friend {
        String id, name, language, status, conversationId;
        int    sessions;
        Friend(String id, String name, String language, String status, int sessions, String convId) {
            this.id = id; this.name = name; this.language = language;
            this.status = status; this.sessions = sessions; this.conversationId = convId;
        }
        boolean isOnline() { return "online".equals(status); }
    }

    private final List<Friend> allFriends = new ArrayList<>();

    @Override
    protected ActivityHomeBinding inflateBinding(LayoutInflater inflater) {
        return ActivityHomeBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        // ── Ẩn nút dark toggle (đã chuyển vào ProfileActivity) ────────────
        if (binding.tvDarkToggle != null) {
            binding.tvDarkToggle.setVisibility(View.GONE);
        }

        // ── Dữ liệu mẫu ────────────────────────────────────────────────────
        // TODO: Thay bằng gọi API GET /api/user/friends/friends
        allFriends.add(new Friend("uid_001", "Mia Chen",    "English",  "online",  12, "conv_001"));
        allFriends.add(new Friend("uid_002", "Ryo Tanaka",  "Japanese", "offline",  8, "conv_002"));
        allFriends.add(new Friend("uid_003", "Léa Moreau",  "French",   "online",   5, "conv_003"));
        allFriends.add(new Friend("uid_004", "Carlos Ruiz", "Spanish",  "away",     3, null));
        allFriends.add(new Friend("uid_005", "Ji-woo Kim",  "Korean",   "online",  20, "conv_005"));

        renderFriendList(allFriends);

        // ── Tab filter ─────────────────────────────────────────────────────
        binding.tabAll.setOnClickListener(v -> {
            setTabActive(binding.tabAll, binding.tabOnline);
            renderFriendList(allFriends);
        });

        binding.tabOnline.setOnClickListener(v -> {
            setTabActive(binding.tabOnline, binding.tabAll);
            List<Friend> online = new ArrayList<>();
            for (Friend f : allFriends) if (f.isOnline()) online.add(f);
            renderFriendList(online);
        });

        // ── Find partner ───────────────────────────────────────────────────
        binding.btnFindPartner.setOnClickListener(v -> showLanguageChooser());

        // ── Bottom nav ─────────────────────────────────────────────────────
        binding.navHome.setOnClickListener(v -> { /* đang ở Home */ });
        binding.navFriends.setOnClickListener(v ->
                startActivity(new Intent(this, FriendsActivity.class)));
        binding.navMatch.setOnClickListener(v -> showLanguageChooser());
        // ← Chat: mở FriendsActivity để chọn người nhắn tin
        binding.navChat.setOnClickListener(v ->
                startActivity(new Intent(this, FriendsActivity.class)));
        binding.navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }

    @Override
    protected void observeViewModel() { }

    public void startMatching(String language) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            pendingLanguage = language;
            ActivityCompat.requestPermissions(this,
                new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                }, 100);
        } else {
            launchMatchingActivity(language);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            boolean ok = true;
            if (grantResults.length > 0) {
                for (int r : grantResults) {
                    if (r != PackageManager.PERMISSION_GRANTED) { ok = false; break; }
                }
            } else { ok = false; }
            if (ok) launchMatchingActivity(pendingLanguage);
            else Toast.makeText(this, "Cần cấp quyền Camera và Mic để gọi video", Toast.LENGTH_LONG).show();
        }
    }

    private void launchMatchingActivity(String language) {
        Intent intent = new Intent(this, MatchingActivity.class);
        intent.putExtra("language", language);
        startActivity(intent);
    }

    private void setTabActive(TextView active, TextView inactive) {
        active.setBackgroundResource(R.drawable.bg_tab_active);
        active.setTextColor(getResources().getColor(R.color.white, getTheme()));
        active.setTypeface(null, android.graphics.Typeface.BOLD);

        inactive.setBackgroundResource(android.R.color.transparent);
        inactive.setTextColor(getResources().getColor(R.color.text_muted, getTheme()));
        inactive.setTypeface(null, android.graphics.Typeface.NORMAL);
    }

    private void renderFriendList(List<Friend> friends) {
        binding.llFriendList.removeAllViews();

        if (friends.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.home_empty_friends);
            empty.setTextColor(getResources().getColor(R.color.text_muted, getTheme()));
            empty.setPadding(0, 24, 0, 24);
            binding.llFriendList.addView(empty);
            return;
        }

        for (Friend friend : friends) {
            binding.llFriendList.addView(buildFriendItem(friend));
        }
    }

    private View buildFriendItem(Friend friend) {
        float dp = getResources().getDisplayMetrics().density;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, (int)(12 * dp), 0, (int)(12 * dp));

        // Avatar
        FrameLayout avatar = new FrameLayout(this);
        int avatarSize = (int)(48 * dp);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(avatarSize, avatarSize);
        avatarParams.setMarginEnd((int)(12 * dp));
        avatar.setLayoutParams(avatarParams);
        avatar.setBackgroundResource(R.drawable.bg_avatar);

        TextView avatarText = new TextView(this);
        avatarText.setText(String.valueOf(friend.name.charAt(0)));
        avatarText.setTextColor(getResources().getColor(R.color.white, getTheme()));
        avatarText.setTextSize(18);
        avatarText.setGravity(android.view.Gravity.CENTER);
        avatarText.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        avatar.addView(avatarText);

        // Status dot
        View dot = new View(this);
        int dotSize = (int)(12 * dp);
        FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(dotSize, dotSize);
        dotParams.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.END;
        dot.setLayoutParams(dotParams);
        int dotRes;
        switch (friend.status) {
            case "online":  dotRes = R.drawable.status_dot_online;  break;
            case "away":    dotRes = R.drawable.status_dot_away;    break;
            default:        dotRes = R.drawable.status_dot_offline; break;
        }
        dot.setBackgroundResource(dotRes);
        avatar.addView(dot);
        row.addView(avatar);

        // Tên + ngôn ngữ
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvName = new TextView(this);
        tvName.setText(friend.name);
        tvName.setTextColor(getResources().getColor(R.color.text_dark, getTheme()));
        tvName.setTextSize(14);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvLang = new TextView(this);
        tvLang.setText(getString(R.string.home_sessions_count, friend.language, friend.sessions));
        tvLang.setTextColor(getResources().getColor(R.color.text_muted, getTheme()));
        tvLang.setTextSize(12);

        info.addView(tvName);
        info.addView(tvLang);
        row.addView(info);

        // Nút chat 💬
        Button btnChat = new Button(this);
        btnChat.setText("💬");
        btnChat.setBackgroundResource(R.drawable.bg_btn_outline);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                (int)(40 * dp), (int)(40 * dp));
        btnParams.setMarginEnd((int)(6 * dp));
        btnChat.setLayoutParams(btnParams);
        btnChat.setOnClickListener(v -> openChat(friend));
        row.addView(btnChat);

        // Nút gọi video 📹
        Button btnCall = new Button(this);
        btnCall.setText("📹");
        btnCall.setBackgroundResource(R.drawable.bg_btn_outline);
        btnCall.setLayoutParams(new LinearLayout.LayoutParams(
                (int)(40 * dp), (int)(40 * dp)));
        btnCall.setOnClickListener(v -> startMatching(friend.language));
        row.addView(btnCall);

        return row;
    }

    /** Mở chat trực tiếp với bạn bè */
    private void openChat(Friend friend) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_FRIEND_ID,   friend.id);
        intent.putExtra(ChatActivity.EXTRA_FRIEND_NAME, friend.name);
        intent.putExtra(ChatActivity.EXTRA_FRIEND_ONLINE, friend.isOnline());
        if (friend.conversationId != null) {
            intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, friend.conversationId);
        }
        startActivity(intent);
    }

    private void showLanguageChooser() {
        try {
            LanguageChooserDialog.newInstance().show(getSupportFragmentManager(), "LanguageChooserDialog");
        } catch (Exception e) {
            Toast.makeText(this, R.string.error_open_lang_chooser, Toast.LENGTH_SHORT).show();
        }
    }
}
