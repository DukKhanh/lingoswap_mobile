package com.lingoswap.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.lingoswap.R;
import com.lingoswap.utils.ImageUtils;
import com.lingoswap.data.local.UserPreferences;
import com.lingoswap.data.model.DashboardResponse;
import com.lingoswap.databinding.ActivityHomeBinding;
import com.lingoswap.presentation.base.BaseActivity;
import com.lingoswap.presentation.chat.ChatActivity;
import com.lingoswap.presentation.friends.FriendsActivity;
import com.lingoswap.presentation.home.HomeViewModel;
import com.lingoswap.utils.HeartbeatManager;
import com.lingoswap.utils.SocketManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Màn hình chính: nạp dashboard (GET /api/users/dashboard), lịch học Tuần/Tháng,
 * và danh sách bạn học gợi ý.
 */
@AndroidEntryPoint
public class HomeActivity extends BaseActivity<ActivityHomeBinding> {

    @Inject UserPreferences userPreferences;
    @Inject SocketManager   socketManager;
    @Inject HeartbeatManager heartbeatManager;
    @Inject com.lingoswap.data.api.NotificationApiService notificationApi;

    private HomeViewModel viewModel;
    private String pendingLanguage;

    static class Friend {
        String id, name, language, status, conversationId, avatar;
        int    sessions;
        Friend(String id, String name, String language, String status, int sessions, String convId, String avatar) {
            this.id = id; this.name = name; this.language = language;
            this.status = status; this.sessions = sessions; this.conversationId = convId;
            this.avatar = avatar;
        }
        boolean isOnline() { return "online".equals(status); }
    }

    private final List<Friend> allFriends = new ArrayList<>();

    private final Set<String> learnedDays = new HashSet<>();   // chuỗi "yyyy-MM-dd"
    private boolean calendarWeekMode = true;
    private final Calendar calendarCursor = Calendar.getInstance(); // kỳ đang xem

    @Override
    protected ActivityHomeBinding inflateBinding(LayoutInflater inflater) {
        return ActivityHomeBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        if (binding.tvDarkToggle != null) {
            binding.tvDarkToggle.setVisibility(View.GONE);
        }

        requestNotificationPermissionIfNeeded();

        renderWeekdayHeader();
        renderCalendar();          // render rỗng trước, cập nhật khi có dữ liệu

        renderOnlineFriends();

        binding.tabWeek.setOnClickListener(v -> {
            calendarWeekMode = true;
            calendarCursor.setTime(new java.util.Date(System.currentTimeMillis()));
            setTabActive(binding.tabWeek, binding.tabMonth);
            renderCalendar();
        });
        binding.tabMonth.setOnClickListener(v -> {
            calendarWeekMode = false;
            calendarCursor.setTime(new java.util.Date(System.currentTimeMillis()));
            setTabActive(binding.tabMonth, binding.tabWeek);
            renderCalendar();
        });

        binding.btnCalPrev.setOnClickListener(v -> shiftCalendar(-1));
        binding.btnCalNext.setOnClickListener(v -> shiftCalendar(1));

        binding.btnNotifications.setOnClickListener(v ->
                startActivity(new Intent(this,
                        com.lingoswap.presentation.notification.NotificationListActivity.class)));

        binding.btnFindPartner.setOnClickListener(v -> showLanguageChooser());

        binding.navHome.setOnClickListener(v -> { /* đang ở Home */ });
        binding.navFriends.setOnClickListener(v ->
                startActivity(new Intent(this, FriendsActivity.class)));
        binding.navMatch.setOnClickListener(v -> showLanguageChooser());
        binding.navChat.setOnClickListener(v ->
                startActivity(new Intent(this,
                        com.lingoswap.presentation.chat.ConversationListActivity.class)));
        binding.navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }

    @Override
    protected void observeViewModel() {
        viewModel.dashboard.observe(this, this::bindDashboard);
        viewModel.error.observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userPreferences.isLoggedIn()) {
            socketManager.connect();
            heartbeatManager.start();
        }
        viewModel.loadDashboard();
        loadUnreadBadge();
    }

    /** Tải số thông báo chưa đọc và hiển thị badge đỏ trên chuông. */
    private void loadUnreadBadge() {
        notificationApi.getUnreadCount().enqueue(new retrofit2.Callback<java.util.Map<String, Integer>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.Map<String, Integer>> call,
                                   retrofit2.Response<java.util.Map<String, Integer>> response) {
                int count = 0;
                if (response.body() != null) {
                    Integer c = response.body().get("unreadCount");
                    if (c != null) count = c;
                }
                if (binding.tvNotifBadge == null) return;
                if (count > 0) {
                    binding.tvNotifBadge.setText(count > 9 ? "9+" : String.valueOf(count));
                    binding.tvNotifBadge.setVisibility(View.VISIBLE);
                } else {
                    binding.tvNotifBadge.setVisibility(View.GONE);
                }
            }
            @Override
            public void onFailure(retrofit2.Call<java.util.Map<String, Integer>> call, Throwable t) { }
        });
    }

    /** Đổi số giờ (double, vd 1.5) → chuỗi "HH:MM" (vd "01:30"). */
    private String formatHoursMinutes(double totalHours) {
        int totalMinutes = (int) Math.round(totalHours * 60);
        int h = totalMinutes / 60;
        int m = totalMinutes % 60;
        return String.format(java.util.Locale.US, "%02d:%02d", h, m);
    }

    private void bindDashboard(DashboardResponse data) {
        if (data == null) return;

        if (data.getGreeting() != null && !data.getGreeting().isEmpty()) {
            binding.tvGreeting.setText(data.getGreeting());
        }

        if (data.getStats() != null) {
            binding.tvStreak.setText(getString(R.string.home_streak_value, data.getStats().getStreak()));
            binding.tvTotalHours.setText(formatHoursMinutes(data.getStats().getTotalHours()));
            binding.tvTotalSessions.setText(String.valueOf(data.getStats().getTotalSessions()));
        }

        learnedDays.clear();
        if (data.getLearningCalendar() != null) {
            for (String d : data.getLearningCalendar()) {
                if (d != null && d.length() >= 10) learnedDays.add(d.substring(0, 10));
            }
        }
        renderCalendar();

        allFriends.clear();
        if (data.getSuggestedPartners() != null) {
            for (DashboardResponse.SuggestedPartner p : data.getSuggestedPartners()) {
                allFriends.add(new Friend(
                        p.getId(),
                        p.getFullName() != null ? p.getFullName() : "User",
                        p.getCountry() != null ? p.getCountry() : "",
                        p.isOnline() ? "online" : "offline",
                        0, null,
                        p.getAvatar()));
            }
        }
        renderOnlineFriends();
    }

    private void renderOnlineFriends() {
        List<Friend> online = new ArrayList<>();
        for (Friend f : allFriends) if (f.isOnline()) online.add(f);
        renderFriendList(online);
    }

    private void renderWeekdayHeader() {
        binding.llWeekdayHeader.removeAllViews();
        String[] labels = {"S", "M", "T", "W", "T", "F", "S"};
        for (String l : labels) {
            TextView tv = new TextView(this);
            tv.setText(l);
            tv.setGravity(Gravity.CENTER);
            tv.setTextColor(getResources().getColor(R.color.text_light, getTheme()));
            tv.setTextSize(11);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            binding.llWeekdayHeader.addView(tv);
        }
    }

    private void shiftCalendar(int delta) {
        if (calendarWeekMode) {
            calendarCursor.add(Calendar.WEEK_OF_YEAR, delta);
        } else {
            calendarCursor.add(Calendar.MONTH, delta);
        }
        renderCalendar();
    }

    private void renderCalendar() {
        binding.llCalendarGrid.removeAllViews();

        Calendar today = Calendar.getInstance();
        java.text.SimpleDateFormat dayKeyFmt =
                new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String todayKey = dayKeyFmt.format(today.getTime());

        if (calendarWeekMode) {
            // Tuần đang xem: lùi về Chủ nhật đầu tuần
            Calendar c = (Calendar) calendarCursor.clone();
            c.add(Calendar.DAY_OF_MONTH, -(c.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY));

            java.text.SimpleDateFormat periodFmt =
                    new java.text.SimpleDateFormat("dd MMM", Locale.getDefault());
            Calendar end = (Calendar) c.clone();
            end.add(Calendar.DAY_OF_MONTH, 6);
            binding.tvCalendarPeriod.setText(periodFmt.format(c.getTime())
                    + " – " + periodFmt.format(end.getTime()));

            LinearLayout row = newCalendarRow();
            for (int i = 0; i < 7; i++) {
                String key = dayKeyFmt.format(c.getTime());
                row.addView(buildDayCell(String.valueOf(c.get(Calendar.DAY_OF_MONTH)),
                        learnedDays.contains(key), key.equals(todayKey)));
                c.add(Calendar.DAY_OF_MONTH, 1);
            }
            binding.llCalendarGrid.addView(row);
        } else {
            java.text.SimpleDateFormat monthFmt =
                    new java.text.SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            binding.tvCalendarPeriod.setText(monthFmt.format(calendarCursor.getTime()));

            Calendar c = (Calendar) calendarCursor.clone();
            c.set(Calendar.DAY_OF_MONTH, 1);
            int firstWeekday = c.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY; // 0..6
            int daysInMonth  = c.getActualMaximum(Calendar.DAY_OF_MONTH);

            LinearLayout row = newCalendarRow();
            int col = 0;
            for (; col < firstWeekday; col++) row.addView(buildEmptyCell());

            for (int day = 1; day <= daysInMonth; day++) {
                if (col == 7) {
                    binding.llCalendarGrid.addView(row);
                    row = newCalendarRow();
                    col = 0;
                }
                Calendar dc = (Calendar) c.clone();
                dc.set(Calendar.DAY_OF_MONTH, day);
                String key = dayKeyFmt.format(dc.getTime());
                row.addView(buildDayCell(String.valueOf(day),
                        learnedDays.contains(key), key.equals(todayKey)));
                col++;
            }
            while (col < 7 && col > 0) { row.addView(buildEmptyCell()); col++; }
            binding.llCalendarGrid.addView(row);
        }
    }

    private LinearLayout newCalendarRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private View buildEmptyCell() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(0,
                (int) (38 * getResources().getDisplayMetrics().density), 1f));
        return v;
    }

    private View buildDayCell(String dayText, boolean learned, boolean isToday) {
        float dp = getResources().getDisplayMetrics().density;

        FrameLayout cell = new FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, (int) (38 * dp), 1f);
        cell.setLayoutParams(lp);

        TextView tv = new TextView(this);
        int size = (int) (30 * dp);
        FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(size, size);
        tlp.gravity = Gravity.CENTER;
        tv.setLayoutParams(tlp);
        tv.setGravity(Gravity.CENTER);
        tv.setText(dayText);
        tv.setTextSize(12);

        if (learned) {
            tv.setBackgroundResource(R.drawable.bg_calendar_day_active);
            tv.setTextColor(getResources().getColor(R.color.white, getTheme()));
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
        } else if (isToday) {
            tv.setBackgroundResource(R.drawable.bg_calendar_day_today);
            tv.setTextColor(getResources().getColor(R.color.blue, getTheme()));
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            tv.setTextColor(getResources().getColor(R.color.text_mid, getTheme()));
        }
        cell.addView(tv);
        return cell;
    }

    private void requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 200);
            }
        }
    }

    public void startMatching(String language) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            pendingLanguage = language;
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 100);
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
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, (int)(12 * dp), 0, (int)(12 * dp));

        FrameLayout avatar = new FrameLayout(this);
        int avatarSize = (int)(48 * dp);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(avatarSize, avatarSize);
        avatarParams.setMarginEnd((int)(12 * dp));
        avatar.setLayoutParams(avatarParams);
        avatar.setBackgroundResource(R.drawable.bg_avatar);

        TextView avatarText = new TextView(this);
        avatarText.setText(friend.name.isEmpty() ? "?" : String.valueOf(friend.name.charAt(0)));
        avatarText.setTextColor(getResources().getColor(R.color.white, getTheme()));
        avatarText.setTextSize(18);
        avatarText.setGravity(Gravity.CENTER);
        avatarText.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        avatar.addView(avatarText);

        if (friend.avatar != null && !friend.avatar.isEmpty()) {
            ImageView avatarImg = new ImageView(this);
            avatarImg.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            avatarImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
            avatar.addView(avatarImg);
            Glide.with(this)
                    .load(ImageUtils.normalizeAvatar(friend.avatar))
                    .circleCrop()
                    .into(avatarImg);
        }

        View dot = new View(this);
        int dotSize = (int)(12 * dp);
        FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(dotSize, dotSize);
        dotParams.gravity = Gravity.BOTTOM | Gravity.END;
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
        tvLang.setText(friend.isOnline()
                ? getString(R.string.status_online)
                : (friend.language.isEmpty() ? getString(R.string.status_offline) : friend.language));
        tvLang.setTextColor(getResources().getColor(R.color.text_muted, getTheme()));
        tvLang.setTextSize(12);

        info.addView(tvName);
        info.addView(tvLang);
        row.addView(info);

        // Đồng bộ kiểu nút với danh sách Friends (item_friend.xml).
        int iconPad = (int)(8 * dp);
        android.widget.ImageButton btnChat = new android.widget.ImageButton(this);
        btnChat.setImageResource(R.drawable.ic_chat);
        btnChat.setColorFilter(getResources().getColor(R.color.blue, getTheme()));
        btnChat.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        btnChat.setPadding(iconPad, iconPad, iconPad, iconPad);
        btnChat.setBackgroundResource(R.drawable.bg_input);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                (int)(36 * dp), (int)(36 * dp));
        btnParams.setMarginEnd((int)(6 * dp));
        btnChat.setLayoutParams(btnParams);
        btnChat.setOnClickListener(v -> openChat(friend));
        row.addView(btnChat);

        android.widget.ImageButton btnCall = new android.widget.ImageButton(this);
        btnCall.setImageResource(R.drawable.ic_phone);
        btnCall.setColorFilter(getResources().getColor(R.color.white, getTheme()));
        btnCall.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        btnCall.setPadding(iconPad, iconPad, iconPad, iconPad);
        btnCall.setBackgroundResource(R.drawable.bg_btn_primary);
        btnCall.setLayoutParams(new LinearLayout.LayoutParams(
                (int)(36 * dp), (int)(36 * dp)));
        btnCall.setOnClickListener(v -> callFriend(friend));
        row.addView(btnCall);

        return row;
    }

    private void openChat(Friend friend) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_FRIEND_ID,   friend.id);
        intent.putExtra(ChatActivity.EXTRA_FRIEND_NAME, friend.name);
        intent.putExtra(ChatActivity.EXTRA_FRIEND_ONLINE, friend.isOnline());
        intent.putExtra(ChatActivity.EXTRA_FRIEND_AVATAR, friend.avatar);
        if (friend.conversationId != null) {
            intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, friend.conversationId);
        }
        startActivity(intent);
    }

    private void callFriend(Friend friend) {
        Intent intent = new Intent(this, OutgoingCallActivity.class);
        intent.putExtra(OutgoingCallActivity.EXTRA_TARGET_ID,   friend.id);
        intent.putExtra(OutgoingCallActivity.EXTRA_TARGET_NAME, friend.name);
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
