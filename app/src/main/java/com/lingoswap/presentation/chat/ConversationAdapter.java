package com.lingoswap.presentation.chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.lingoswap.R;
import com.lingoswap.data.model.Conversation;
import com.lingoswap.utils.ImageUtils;

import java.util.ArrayList;
import java.util.List;

/** ListView adapter cho danh sách cuộc trò chuyện (api 4.1). */
public class ConversationAdapter extends BaseAdapter {

    private final Context context;
    private final List<Conversation> items = new ArrayList<>();

    public ConversationAdapter(Context context) { this.context = context; }

    public void setItems(List<Conversation> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @Override public int getCount() { return items.size(); }
    @Override public Object getItem(int position) { return items.get(position); }
    @Override public long getItemId(int position) { return position; }

    static class Holder {
        ImageView ivAvatar;
        TextView tvInitial, tvName, tvLastMessage, tvTime;
        View onlineDot;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        Holder h;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_conversation, parent, false);
            h = new Holder();
            h.ivAvatar      = convertView.findViewById(R.id.ivAvatar);
            h.tvInitial     = convertView.findViewById(R.id.tvInitial);
            h.tvName        = convertView.findViewById(R.id.tvName);
            h.tvLastMessage = convertView.findViewById(R.id.tvLastMessage);
            h.tvTime        = convertView.findViewById(R.id.tvTime);
            h.onlineDot     = convertView.findViewById(R.id.viewOnlineDot);
            convertView.setTag(h);
        } else {
            h = (Holder) convertView.getTag();
        }

        Conversation c = items.get(position);
        Conversation.Partner p = c.getPartner();

        String name = p != null ? p.getDisplayName() : "LingoSwap User";
        h.tvName.setText(name);
        h.tvInitial.setText(name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase());

        h.tvLastMessage.setText(c.getLastMessage() != null ? c.getLastMessage().getPreview() : "");
        h.tvTime.setText(c.getUpdatedAt() != null ? c.getUpdatedAt().getFriendly() : "");
        h.onlineDot.setVisibility(p != null && p.isOnline() ? View.VISIBLE : View.GONE);

        String avatar = p != null ? p.getAvatarUrl() : null;
        // "default_avatar.png" là sentinel backend cho "chưa set avatar" → hiển thị chữ cái đầu.
        boolean hasRealAvatar = avatar != null && !avatar.isEmpty()
                && !avatar.equals("default_avatar.png");
        if (hasRealAvatar) {
            h.tvInitial.setVisibility(View.GONE);
            h.ivAvatar.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(ImageUtils.normalizeAvatar(avatar))
                    .circleCrop()
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .error(R.drawable.ic_avatar_placeholder)
                    .into(h.ivAvatar);
        } else {
            h.ivAvatar.setVisibility(View.GONE);
            h.tvInitial.setVisibility(View.VISIBLE);
        }
        return convertView;
    }
}
