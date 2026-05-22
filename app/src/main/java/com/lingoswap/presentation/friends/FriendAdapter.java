package com.lingoswap.presentation.friends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.lingoswap.R;
import com.lingoswap.data.model.Friend;

public class FriendAdapter extends ListAdapter<Friend, FriendAdapter.VH> {

    public interface Listener {
        void onChat(Friend friend);
        void onCall(Friend friend);
        void onAccept(Friend friend);   // requestId stored in friend.id when isPending
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
                public boolean areItemsTheSame(@NonNull Friend oldItem, @NonNull Friend newItem) {
                    // Use id as stable identifier
                    if (oldItem.id == null || newItem.id == null) return false;
                    return oldItem.id.equals(newItem.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull Friend oldItem, @NonNull Friend newItem) {
                    boolean sameStatus = safeEquals(oldItem.status, newItem.status);
                    boolean samePending = oldItem.isPending == newItem.isPending;
                    boolean sameName = safeEquals(oldItem.fullName, newItem.fullName);
                    return sameStatus && samePending && sameName;
                }

                private boolean safeEquals(String a, String b) {
                    if (a == null && b == null) return true;
                    if (a == null || b == null) return false;
                    return a.equals(b);
                }
            };

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Friend f = getItem(position);
        h.bind(f, listener);
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvStatus;
        View viewOnlineDot;
        ImageView imgAvatar;
        ImageView btnChat;
        ImageView btnCall;
        ImageView btnRemove;
        View layoutActions;       // chat + call buttons row
        View layoutRequestActions; // accept + reject buttons row
        TextView btnAccept;
        TextView btnReject;
        TextView tvSentAt;

        VH(@NonNull View v) {
            super(v);
            tvName       = v.findViewById(R.id.tvFriendName);
            tvStatus     = v.findViewById(R.id.tvFriendStatus);
            viewOnlineDot = v.findViewById(R.id.viewOnlineDot);
            imgAvatar    = v.findViewById(R.id.imgFriendAvatar);
            btnChat      = v.findViewById(R.id.btnFriendChat);
            btnCall      = v.findViewById(R.id.btnFriendCall);
            btnRemove    = v.findViewById(R.id.btnFriendRemove);
            layoutActions        = v.findViewById(R.id.layoutFriendActions);
            layoutRequestActions = v.findViewById(R.id.layoutRequestActions);
            btnAccept    = v.findViewById(R.id.btnAccept);
            btnReject    = v.findViewById(R.id.btnReject);
            tvSentAt     = v.findViewById(R.id.tvSentAt);
        }

        void bind(Friend f, Listener listener) {
            // Name
            tvName.setText(f.fullName != null ? f.fullName :
                    (f.partner != null ? f.partner.username : ""));

            // Avatar
            if (imgAvatar != null) {
                String avatarUrl = f.avatar != null ? f.avatar :
                        (f.partner != null ? f.partner.avatar : null);
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    Glide.with(imgAvatar.getContext())
                            .load(avatarUrl)
                            .apply(RequestOptions.circleCropTransform())
                            .placeholder(R.drawable.ic_default_avatar)
                            .into(imgAvatar);
                } else {
                    imgAvatar.setImageResource(R.drawable.ic_default_avatar);
                }
            }

            if (f.isPending) {
                // ── Pending friend request mode ──
                tvStatus.setText(itemView.getContext().getString(R.string.friend_request_pending));
                if (viewOnlineDot != null) viewOnlineDot.setVisibility(View.GONE);

                // Show sentAt if available
                if (tvSentAt != null && f.sentAt != null) {
                    tvSentAt.setVisibility(View.VISIBLE);
                    tvSentAt.setText(f.sentAt.friendly);
                } else if (tvSentAt != null) {
                    tvSentAt.setVisibility(View.GONE);
                }

                // Show request action buttons, hide friend actions
                if (layoutActions != null) layoutActions.setVisibility(View.GONE);
                if (layoutRequestActions != null) layoutRequestActions.setVisibility(View.VISIBLE);

                if (btnAccept != null) btnAccept.setOnClickListener(v -> listener.onAccept(f));
                if (btnReject != null) btnReject.setOnClickListener(v -> listener.onReject(f));

            } else {
                // ── Regular friend mode ──
                boolean online = f.isOnline();
                tvStatus.setText(online
                        ? itemView.getContext().getString(R.string.status_online)
                        : itemView.getContext().getString(R.string.status_offline));
                if (viewOnlineDot != null)
                    viewOnlineDot.setVisibility(online ? View.VISIBLE : View.GONE);

                if (tvSentAt != null) tvSentAt.setVisibility(View.GONE);

                // Show friend actions, hide request actions
                if (layoutActions != null) layoutActions.setVisibility(View.VISIBLE);
                if (layoutRequestActions != null) layoutRequestActions.setVisibility(View.GONE);

                if (btnChat != null) btnChat.setOnClickListener(v -> listener.onChat(f));
                if (btnCall != null) btnCall.setOnClickListener(v -> listener.onCall(f));
                if (btnRemove != null) btnRemove.setOnClickListener(v -> listener.onRemove(f));
            }
        }
    }
}
