package com.lingoswap.presentation.friends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lingoswap.R;
import com.lingoswap.data.model.Friend;

import java.util.ArrayList;
import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.VH> {

    public interface Listener {
        void onChat(Friend friend);
        void onCall(Friend friend);
    }

    private List<Friend> list = new ArrayList<>();
    private final Listener listener;

    public FriendAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<Friend> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
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

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvLangs, tvStatus;
        View viewOnlineDot;
        ImageView btnChat, btnCall;

        VH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvFriendName);
            tvLangs = v.findViewById(R.id.tvFriendLangs);
            tvStatus = v.findViewById(R.id.tvFriendStatus);
            viewOnlineDot = v.findViewById(R.id.viewOnlineDot);
            btnChat = v.findViewById(R.id.btnFriendChat);
            btnCall = v.findViewById(R.id.btnFriendCall);
        }
    }
}
