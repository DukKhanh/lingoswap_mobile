package com.lingoswap.presentation.notification;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.lingoswap.R;
import com.lingoswap.data.model.Notification;

import java.util.ArrayList;
import java.util.List;

/** ListView adapter cho danh sách thông báo. */
public class NotificationAdapter extends BaseAdapter {

    private final Context context;
    private final List<Notification> items = new ArrayList<>();

    public NotificationAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<Notification> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @Override public int getCount() { return items.size(); }
    @Override public Object getItem(int position) { return items.get(position); }
    @Override public long getItemId(int position) { return position; }

    static class Holder {
        View dot;
        TextView content, time;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        Holder h;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
            h = new Holder();
            h.dot     = convertView.findViewById(R.id.viewUnreadDot);
            h.content = convertView.findViewById(R.id.tvContent);
            h.time    = convertView.findViewById(R.id.tvTime);
            convertView.setTag(h);
        } else {
            h = (Holder) convertView.getTag();
        }

        Notification n = items.get(position);
        h.content.setText(n.getContent());
        h.time.setText(friendlyTime(n.getCreatedAt()));

        boolean unread = !n.isRead();
        h.dot.setVisibility(unread ? View.VISIBLE : View.INVISIBLE);
        h.content.setTypeface(null, unread ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        return convertView;
    }

    /** Đổi ISO UTC ("2026-06-18T14:28:58.987Z") → giờ thân thiện theo múi giờ thiết bị. */
    private String friendlyTime(String iso) {
        if (iso == null || iso.isEmpty()) return "";
        try {
            java.time.Instant t = java.time.Instant.parse(iso);
            long diffSec = (System.currentTimeMillis() - t.toEpochMilli()) / 1000;
            if (diffSec < 60)        return "Vừa xong";
            if (diffSec < 3600)      return (diffSec / 60) + " phút trước";
            if (diffSec < 86400)     return (diffSec / 3600) + " giờ trước";
            if (diffSec < 172800)    return "Hôm qua";
            java.time.ZonedDateTime z = t.atZone(java.time.ZoneId.systemDefault());
            return String.format(java.util.Locale.US, "%02d/%02d/%04d",
                    z.getDayOfMonth(), z.getMonthValue(), z.getYear());
        } catch (Exception e) {
            return iso;
        }
    }
}
