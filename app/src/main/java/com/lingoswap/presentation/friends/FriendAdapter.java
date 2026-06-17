package com.lingoswap.presentation.friends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.lingoswap.R;
import com.lingoswap.data.model.Friend;

public class FriendAdapter extends ListAdapter<Friend, FriendAdapter.ViewHolder> {

    public interface Listener {
        void onChat(Friend friend);
        void onCall(Friend friend);
        void onAccept(Friend friend);   
        void onReject(Friend friend);
        void onRemove(Friend friend);
    }

    private final Listener listener;

    public FriendAdapter(Listener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Friend> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Friend>() {
                @Override
                public boolean areItemsTheSame(@NonNull Friend a, @NonNull Friend b) {
                    return a.id != null && a.id.equals(b.id);
                }
                @Override
                public boolean areContentsTheSame(@NonNull Friend a, @NonNull Friend b) {
                    return a.id != null && a.id.equals(b.id)
                            && strEq(a.fullName, b.fullName)
                            && strEq(a.status, b.status)
                            && a.isPending == b.isPending;
                }
                private boolean strEq(String a, String b) {
                    return a == null ? b == null : a.equals(b);
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView    tvName;
        private final TextView    tvStatus;
        private final TextView    tvInitial;
        private final ImageView   ivAvatar;
        private final View        statusDot;
        private final LinearLayout layoutActions;
        private final LinearLayout layoutRequestActions;
        private final ImageView   btnChat;
        private final ImageView   btnCall;
        private final TextView    btnAccept; // Chuyển sang TextView
        private final TextView    btnReject; // Chuyển sang TextView

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName               = itemView.findViewById(R.id.tvFriendName);
            tvStatus             = itemView.findViewById(R.id.tvFriendStatus);
            tvInitial            = itemView.findViewById(R.id.tvAvatarInitial);
            ivAvatar             = itemView.findViewById(R.id.ivAvatar);
            statusDot            = itemView.findViewById(R.id.viewStatusDot);
            layoutActions        = itemView.findViewById(R.id.layoutActions);
            layoutRequestActions = itemView.findViewById(R.id.layoutRequestActions);
            btnChat              = itemView.findViewById(R.id.btnChat);
            btnCall              = itemView.findViewById(R.id.btnCall);
            btnAccept            = itemView.findViewById(R.id.btnAccept);
            btnReject            = itemView.findViewById(R.id.btnReject);
        }

        void bind(Friend friend, Listener listener) {
            String name = friend.fullName;
            if (name == null || name.isEmpty()) {
                if (friend.partner != null) {
                    name = friend.partner.getDisplayName();
                }
            }
            if (name == null || name.isEmpty()) name = "User";

            tvName.setText(name);
            tvInitial.setText(name.substring(0, 1).toUpperCase());

            String avatarUrl = (friend.avatar != null && !friend.avatar.isEmpty()) 
                    ? friend.avatar 
                    : (friend.partner != null ? friend.partner.avatar : null);

            if (avatarUrl != null && !avatarUrl.isEmpty() && !avatarUrl.equals("default_avatar.png")) {
                tvInitial.setVisibility(View.GONE);
                ivAvatar.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(avatarUrl)
                        .circleCrop()
                        .placeholder(R.drawable.bg_avatar)
                        .into(ivAvatar);
            } else {
                ivAvatar.setVisibility(View.GONE);
                tvInitial.setVisibility(View.VISIBLE);
            }

            if (friend.isPending) {
                String sentTime = (friend.sentAt != null && friend.sentAt.friendly != null)
                        ? friend.sentAt.friendly : "";
                tvStatus.setText(sentTime.isEmpty() ? "Yêu cầu kết bạn" : sentTime);
                
                if (statusDot != null) statusDot.setVisibility(View.GONE);
                layoutActions.setVisibility(View.GONE);
                layoutRequestActions.setVisibility(View.VISIBLE);

                btnAccept.setOnClickListener(v -> listener.onAccept(friend));
                btnReject.setOnClickListener(v -> listener.onReject(friend));
            } else {
                boolean online = friend.isOnline();
                tvStatus.setText(online ? "Đang hoạt động" : "Ngoại tuyến");
                
                if (statusDot != null) {
                    statusDot.setVisibility(View.VISIBLE);
                    statusDot.setBackgroundResource(online ? R.drawable.status_dot_online : R.drawable.status_dot_offline);
                }
                layoutActions.setVisibility(View.VISIBLE);
                layoutRequestActions.setVisibility(View.GONE);

                btnChat.setOnClickListener(v -> listener.onChat(friend));
                btnCall.setOnClickListener(v -> listener.onCall(friend));
            }
        }
    }
}
