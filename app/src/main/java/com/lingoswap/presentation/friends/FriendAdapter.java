package com.lingoswap.presentation.friends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.lingoswap.R;
import com.lingoswap.data.model.Friend;

public class FriendAdapter extends ListAdapter<Friend, FriendAdapter.ViewHolder> {

    public interface Listener {
        void onChat(Friend friend);
        void onCall(Friend friend);
        void onAccept(Friend friend);   // friend.id = friendshipId
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

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView    tvName;
        private final TextView    tvStatus;
        private final TextView    tvInitial;
        private final View        statusDot;
        private final LinearLayout layoutActions;
        private final LinearLayout layoutRequestActions;
        private final ImageView   btnChat;
        private final ImageView   btnCall;
        private final Button      btnAccept;
        private final Button      btnReject;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName               = itemView.findViewById(R.id.tvFriendName);
            tvStatus             = itemView.findViewById(R.id.tvFriendStatus);
            tvInitial            = itemView.findViewById(R.id.tvAvatarInitial);
            statusDot            = itemView.findViewById(R.id.viewStatusDot);
            layoutActions        = itemView.findViewById(R.id.layoutActions);
            layoutRequestActions = itemView.findViewById(R.id.layoutRequestActions);
            btnChat              = itemView.findViewById(R.id.btnChat);
            btnCall              = itemView.findViewById(R.id.btnCall);
            btnAccept            = itemView.findViewById(R.id.btnAccept);
            btnReject            = itemView.findViewById(R.id.btnReject);
        }

        void bind(Friend friend, Listener listener) {
            // ── Tên ──────────────────────────────────────────────────
            String name = friend.fullName != null ? friend.fullName : "User";

            // Nếu isPending thì lấy tên từ partner nếu fullName null
            if (friend.isPending && (friend.fullName == null || friend.fullName.isEmpty())) {
                if (friend.partner != null && friend.partner.username != null) {
                    name = friend.partner.username;
                }
            }

            tvName.setText(name);
            tvInitial.setText(name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase());

            // ── Trạng thái ───────────────────────────────────────────
            if (friend.isPending) {
                // Tab Requests
                String sentTime = (friend.sentAt != null && friend.sentAt.friendly != null)
                        ? friend.sentAt.friendly : "";
                tvStatus.setText(sentTime.isEmpty()
                        ? itemView.getContext().getString(R.string.friend_request_pending)
                        : sentTime);

                statusDot.setVisibility(View.GONE);
                layoutActions.setVisibility(View.GONE);
                layoutRequestActions.setVisibility(View.VISIBLE);

                btnAccept.setOnClickListener(v -> listener.onAccept(friend));
                btnReject.setOnClickListener(v -> listener.onReject(friend));

            } else {
                // Tab All / Online
                boolean online = friend.isOnline();
                tvStatus.setText(online
                        ? itemView.getContext().getString(R.string.status_online)
                        : itemView.getContext().getString(R.string.status_offline));
                tvStatus.setTextColor(itemView.getContext().getResources().getColor(
                        online ? R.color.blue : R.color.text_muted,
                        itemView.getContext().getTheme()));

                statusDot.setVisibility(View.VISIBLE);
                statusDot.setBackgroundResource(online
                        ? R.drawable.status_dot_online
                        : R.drawable.status_dot_offline);

                layoutActions.setVisibility(View.VISIBLE);
                layoutRequestActions.setVisibility(View.GONE);

                btnChat.setOnClickListener(v -> listener.onChat(friend));
                btnCall.setOnClickListener(v -> listener.onCall(friend));

                // Long press → remove friend
                itemView.setOnLongClickListener(v -> {
                    listener.onRemove(friend);
                    return true;
                });
            }
        }
    }
}
