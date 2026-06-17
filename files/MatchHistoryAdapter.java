package com.lingoswap.presentation.friends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.lingoswap.R;
import com.lingoswap.data.model.MatchHistoryResponse;

import java.util.ArrayList;
import java.util.List;

public class MatchHistoryAdapter
        extends RecyclerView.Adapter<MatchHistoryAdapter.ViewHolder> {

    public interface Listener {
        void onAddFriend(MatchHistoryResponse session);
    }

    private List<MatchHistoryResponse> items = new ArrayList<>();
    private final Listener listener;

    public MatchHistoryAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<MatchHistoryResponse> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_match_history, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView  tvInitial, tvPartnerName, tvLanguage, tvDuration;
        Button    btnAddFriend;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar      = itemView.findViewById(R.id.ivAvatar);
            tvInitial     = itemView.findViewById(R.id.tvInitial);
            tvPartnerName = itemView.findViewById(R.id.tvPartnerName);
            tvLanguage    = itemView.findViewById(R.id.tvLanguage);
            tvDuration    = itemView.findViewById(R.id.tvDuration);
            btnAddFriend  = itemView.findViewById(R.id.btnAddFriend);
        }

        void bind(MatchHistoryResponse session, Listener listener) {
            MatchHistoryResponse.Partner partner = session.partner;
            String name = partner != null ? partner.getFullName() : "User";
            String avatar = partner != null ? partner.getAvatar() : null;

            tvPartnerName.setText(name);
            tvInitial.setText(name.isEmpty() ? "?" :
                    String.valueOf(name.charAt(0)).toUpperCase());
            tvLanguage.setText(session.language != null ? session.language : "");
            tvDuration.setText("⏱ " + session.getFormattedDuration());

            // Avatar
            if (avatar != null && !avatar.isEmpty()
                    && !avatar.equals("default_avatar.png")) {
                tvInitial.setVisibility(View.GONE);
                ivAvatar.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(avatar)
                        .circleCrop()
                        .placeholder(R.drawable.bg_avatar)
                        .into(ivAvatar);
            } else {
                ivAvatar.setVisibility(View.GONE);
                tvInitial.setVisibility(View.VISIBLE);
            }

            // Nút kết bạn — disable nếu partner null
            if (partner == null || partner.id == null) {
                btnAddFriend.setVisibility(View.GONE);
                return;
            }

            btnAddFriend.setVisibility(View.VISIBLE);
            btnAddFriend.setEnabled(true);
            btnAddFriend.setText("+ Kết bạn");
            btnAddFriend.setAlpha(1f);
            btnAddFriend.setOnClickListener(v -> {
                btnAddFriend.setEnabled(false);
                btnAddFriend.setText("Đang gửi...");
                listener.onAddFriend(session);
            });
        }

        /** Gọi sau khi gửi lời mời thành công */
        public void markAsSent() {
            btnAddFriend.setText("✓ Đã gửi");
            btnAddFriend.setEnabled(false);
            btnAddFriend.setAlpha(0.6f);
        }
    }
}
