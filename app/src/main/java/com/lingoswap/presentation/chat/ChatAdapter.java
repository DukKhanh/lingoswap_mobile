package com.lingoswap.presentation.chat;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lingoswap.R;
import com.lingoswap.data.model.Message;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {

    public interface Listener {
        void onLongPress(Message msg, int position);
    }

    private List<Message> list = new ArrayList<>();
    private final Listener listener;

    public ChatAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<Message> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int pos) {
        return list.get(pos).isMine ? 0 : 1;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Message m = list.get(position);

        h.tvText.setText(m.text);
        h.tvTime.setText(m.time);

        if (m.isMine) {
            h.root.setGravity(Gravity.END);
            h.bubble.setBackgroundResource(R.drawable.bg_bubble_me);
            h.tvText.setTextColor(0xFFFFFFFF);
            h.tvTime.setTextColor(0x99FFFFFF);
            h.cvAvatar.setVisibility(View.GONE);
        } else {
            h.root.setGravity(Gravity.START);
            h.bubble.setBackgroundResource(R.drawable.bg_bubble_them);
            h.tvText.setTextColor(h.tvText.getContext().getResources().getColor(R.color.text_dark));
            h.tvTime.setTextColor(h.tvTime.getContext().getResources().getColor(R.color.text_muted));
            h.cvAvatar.setVisibility(View.VISIBLE);
        }

        h.itemView.setOnLongClickListener(v -> {
            listener.onLongPress(m, position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout root, bubble;
        TextView tvText, tvTime;
        View cvAvatar;

        VH(View v) {
            super(v);
            root = v.findViewById(R.id.layoutMessageRoot);
            bubble = v.findViewById(R.id.layoutBubble);
            tvText = v.findViewById(R.id.tvMessageText);
            tvTime = v.findViewById(R.id.tvMessageTime);
            cvAvatar = v.findViewById(R.id.cvPartnerAvatar);
        }
    }
}
