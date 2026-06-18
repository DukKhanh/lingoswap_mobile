package com.lingoswap.presentation.notification;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.lingoswap.R;
import com.lingoswap.data.model.Notification;
import com.lingoswap.presentation.friends.FriendsActivity;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * NotificationListActivity — màn hình Thông báo (api-doc 7.1–7.4).
 */
@AndroidEntryPoint
public class NotificationListActivity extends AppCompatActivity {

    private NotificationViewModel viewModel;
    private NotificationAdapter   adapter;

    private ListView    lvNotifications;
    private ProgressBar progress;
    private TextView    tvEmpty;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_list);

        viewModel = new ViewModelProvider(this).get(NotificationViewModel.class);

        lvNotifications = findViewById(R.id.lvNotifications);
        progress        = findViewById(R.id.progress);
        tvEmpty         = findViewById(R.id.tvEmpty);

        adapter = new NotificationAdapter(this);
        lvNotifications.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnMarkAllRead).setOnClickListener(v -> viewModel.markAllRead());

        lvNotifications.setOnItemClickListener((parent, view, position, id) -> {
            Notification n = (Notification) adapter.getItem(position);
            if (n == null) return;
            if (!n.isRead()) viewModel.markRead(n.getId());
            routeByType(n);
        });

        observe();
        viewModel.loadNotifications();
    }

    private void observe() {
        viewModel.notifications.observe(this, list -> {
            adapter.setItems(list);
            boolean empty = list == null || list.isEmpty();
            tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        });
        viewModel.isLoading.observe(this, loading ->
                progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));
        viewModel.error.observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
        viewModel.message.observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
    }

    /** Điều hướng theo type của thông báo. */
    private void routeByType(Notification n) {
        String type = n.getType() != null ? n.getType() : "";
        switch (type) {
            case "friend_request": {
                // Lời mời đến → mở thẳng tab Requests
                Intent i = new Intent(this, FriendsActivity.class);
                i.putExtra(FriendsActivity.EXTRA_OPEN_TAB, 2);
                startActivity(i);
                break;
            }
            case "friend_accepted":
                // Đã được chấp nhận → mở danh sách bạn (tab All)
                startActivity(new Intent(this, FriendsActivity.class));
                break;
            default:
                // Thông báo chung: chỉ đánh dấu đã đọc, ở lại màn hình
                break;
        }
    }
}
