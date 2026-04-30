package com.lingoswap.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.lingoswap.R;
import com.lingoswap.presentation.friends.FriendsActivity;
import com.lingoswap.presentation.chat.ChatActivity;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

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
    private LinearLayout llFriendList;
    private TextView tabAll, tabOnline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // ── Dữ liệu mẫu (thay bằng API call sau) ──────────────────
        allFriends.add(new Friend("Mia Chen",    "English",    "online",  12));
        allFriends.add(new Friend("Ryo Tanaka",  "Japanese",   "offline",  8));
        allFriends.add(new Friend("Léa Moreau",  "French",     "online",   5));
        allFriends.add(new Friend("Carlos Ruiz", "Spanish",    "away",     3));
        allFriends.add(new Friend("Ji-woo Kim",  "Korean",     "online",  20));

        llFriendList = findViewById(R.id.llFriendList);
        tabAll       = findViewById(R.id.tabAll);
        tabOnline    = findViewById(R.id.tabOnline);

        Button   btnFindPartner = findViewById(R.id.btnFindPartner);
        LinearLayout navHome    = findViewById(R.id.navHome);
        LinearLayout navFriends = findViewById(R.id.navFriends);
        LinearLayout navMatch   = findViewById(R.id.navMatch);
        LinearLayout navChat    = findViewById(R.id.navChat);
        LinearLayout navProfile = findViewById(R.id.navProfile);

        // ── Hiển thị mặc định: tất cả ─────────────────────────────
        renderFriendList(allFriends);

        // ── Tab filter ─────────────────────────────────────────────
        if (tabAll != null) {
            tabAll.setOnClickListener(v -> {
                setTabActive(tabAll, tabOnline);
                renderFriendList(allFriends);
            });
        }
        if (tabOnline != null) {
            tabOnline.setOnClickListener(v -> {
                setTabActive(tabOnline, tabAll);
                List<Friend> online = new ArrayList<>();
                for (Friend f : allFriends) if (f.isOnline()) online.add(f);
                renderFriendList(online);
            });
        }

        // ── Find partner ───────────────────────────────────────────
        if (btnFindPartner != null) {
            btnFindPartner.setOnClickListener(v -> showLanguageChooser());
        }

        // ── Bottom nav ─────────────────────────────────────────────
        if (navHome != null) navHome.setOnClickListener(v -> { /* đang ở Home */ });
        if (navFriends != null) {
            navFriends.setOnClickListener(v ->
                startActivity(new Intent(this, FriendsActivity.class))
            );
        }
        if (navMatch != null) navMatch.setOnClickListener(v -> showLanguageChooser());
        if (navChat != null) {
            navChat.setOnClickListener(v ->
                startActivity(new Intent(this, ChatActivity.class))
            );
        }
        if (navProfile != null) {
            navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class))
            );
        }
    }

    /** Chuyển trạng thái tab active / inactive */
    private void setTabActive(TextView active, TextView inactive) {
        if (active != null) {
            active.setBackgroundResource(R.drawable.bg_tab_active);
            active.setTextColor(getColor(R.color.white));
            active.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        if (inactive != null) {
            inactive.setBackgroundResource(android.R.color.transparent);
            inactive.setTextColor(getColor(R.color.text_muted));
            inactive.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    /** Render danh sách bạn bè vào container */
    private void renderFriendList(List<Friend> friends) {
        if (llFriendList == null) return;
        llFriendList.removeAllViews();

        if (friends.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Không có bạn bè nào trong danh sách này");
            empty.setTextColor(getColor(R.color.text_muted));
            empty.setPadding(0, 24, 0, 24);
            llFriendList.addView(empty);
            return;
        }

        for (Friend friend : friends) {
            View item = buildFriendItem(friend);
            llFriendList.addView(item);
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
        avatarText.setTextColor(getColor(R.color.white));
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
        tvName.setTextColor(getColor(R.color.text_dark));
        tvName.setTextSize(14);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvLang = new TextView(this);
        tvLang.setText(friend.language + " · " + friend.sessions + " sessions");
        tvLang.setTextColor(getColor(R.color.text_muted));
        tvLang.setTextSize(12);

        info.addView(tvName);
        info.addView(tvLang);
        row.addView(info);

        // Nút gọi video
        Button btnCall = new Button(this);
        btnCall.setText("📹");
        btnCall.setBackgroundResource(R.drawable.bg_btn_outline);
        btnCall.setOnClickListener(v -> {
            Intent intent = new Intent(this, VideoCallActivity.class);
            intent.putExtra("language", friend.language);
            intent.putExtra("partnerName", friend.name);
            startActivity(intent);
        });
        row.addView(btnCall);

        return row;
    }

    private void showLanguageChooser() {
        try {
            androidx.fragment.app.DialogFragment dialog = (androidx.fragment.app.DialogFragment) 
                Class.forName("com.lingoswap.activities.LanguageChooserDialog").newInstance();
            dialog.show(getSupportFragmentManager(), "LanguageChooserDialog");
        } catch (Exception e) {
            Toast.makeText(this, "Không thể mở hộp thoại chọn ngôn ngữ", Toast.LENGTH_SHORT).show();
        }
    }
}
