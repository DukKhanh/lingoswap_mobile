package com.lingoswap.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.lingoswap.databinding.ActivityHomeBinding;
import com.lingoswap.presentation.base.BaseActivity;
import com.lingoswap.presentation.chat.ChatActivity;
import com.lingoswap.presentation.friends.FriendsActivity;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeActivity extends BaseActivity<ActivityHomeBinding> {

    private String pendingLanguage;

    // ── Mô hình dữ liệu người bạn ──────────────────────────────────
    static class Friend {
        String name, language, status; // "online" | "offline" | "away"
        int    sessions;
        Friend(String name, String language, String status, int sessions) {
            this.name = name; this.language = language;
            this.status = status; this.sessions = sessions;
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
        // ── Dữ liệu mẫu (thay bằng API call sau) ──────────────────
        allFriends.add(new Friend("Mia Chen",    "English",    "online",  12));
        allFriends.add(new Friend("Ryo Tanaka",  "Japanese",   "offline",  8));
        allFriends.add(new Friend("Léa Moreau",  "French",     "online",   5));
        allFriends.add(new Friend("Carlos Ruiz", "Spanish",    "away",     3));
        allFriends.add(new Friend("Ji-woo Kim",  "Korean",     "online",  20));

        // ── Hiển thị mặc định: tất cả ─────────────────────────────
        renderFriendList(allFriends);

        // ── Tab filter ─────────────────────────────────────────────
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

        // ── Find partner ───────────────────────────────────────────
        binding.btnFindPartner.setOnClickListener(v -> showLanguageChooser());

        // ── Bottom nav ─────────────────────────────────────────────
        binding.navHome.setOnClickListener(v -> { /* đang ở Home */ });
        binding.navFriends.setOnClickListener(v ->
            startActivity(new Intent(this, FriendsActivity.class))
        );
        binding.navMatch.setOnClickListener(v -> showLanguageChooser());
        binding.navChat.setOnClickListener(v ->
            startActivity(new Intent(this, ChatActivity.class))
        );
        binding.navProfile.setOnClickListener(v ->
            startActivity(new Intent(this, ProfileActivity.class))
        );
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
                    if (r != PackageManager.PERMISSION_GRANTED) {
                        ok = false;
                        break;
                    }
                }
            } else {
                ok = false;
            }
            if (ok) launchMatchingActivity(pendingLanguage);
            else Toast.makeText(this, "Cần cấp quyền Camera và Mic để gọi video", Toast.LENGTH_LONG).show();
        }
    }

    private void launchMatchingActivity(String language) {
        Intent intent = new Intent(this, MatchingActivity.class);
        intent.putExtra("language", language);
        startActivity(intent);
    }

    /** Chuyển trạng thái tab active / inactive */
    private void setTabActive(TextView active, TextView inactive) {
        active.setBackgroundResource(R.drawable.bg_tab_active);
        active.setTextColor(getResources().getColor(R.color.white, getTheme()));
        active.setTypeface(null, android.graphics.Typeface.BOLD);

        inactive.setBackgroundResource(android.R.color.transparent);
        inactive.setTextColor(getResources().getColor(R.color.text_muted, getTheme()));
        inactive.setTypeface(null, android.graphics.Typeface.NORMAL);
    }

    /** Render danh sách bạn bè vào container */
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
            View item = buildFriendItem(friend);
            binding.llFriendList.addView(item);
        }
    }

    /** Tạo view cho từng bạn bè */
    private View buildFriendItem(Friend friend) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, 12, 0, 12);

        // Avatar
        FrameLayout avatar = new FrameLayout(this);
        LinearLayout.LayoutParams avatarParams =
                new LinearLayout.LayoutParams(48 * (int)getResources().getDisplayMetrics().density, 48 * (int)getResources().getDisplayMetrics().density);
        avatarParams.setMarginEnd(12 * (int)getResources().getDisplayMetrics().density);
        avatar.setLayoutParams(avatarParams);
        avatar.setBackgroundResource(R.drawable.bg_avatar);

        TextView avatarText = new TextView(this);
        avatarText.setText(String.valueOf(friend.name.charAt(0)));
        avatarText.setTextColor(getResources().getColor(R.color.white, getTheme()));
        avatarText.setTextSize(18);
        avatarText.setGravity(android.view.Gravity.CENTER);
        FrameLayout.LayoutParams tvParams =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT);
        avatarText.setLayoutParams(tvParams);
        avatar.addView(avatarText);

        // Status dot
        View dot = new View(this);
        int dotSize = (int) (12 * getResources().getDisplayMetrics().density);
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

        // Nút gọi video
        Button btnCall = new Button(this);
        btnCall.setText("📹");
        btnCall.setBackgroundResource(R.drawable.bg_btn_outline);
        btnCall.setOnClickListener(v -> {
            startMatching(friend.language);
        });
        row.addView(btnCall);

        return row;
    }

    private void showLanguageChooser() {
        try {
            LanguageChooserDialog.newInstance().show(getSupportFragmentManager(), "LanguageChooserDialog");
        } catch (Exception e) {
            Toast.makeText(this, R.string.error_open_lang_chooser, Toast.LENGTH_SHORT).show();
        }
    }
}
