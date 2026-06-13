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
import com.lingoswap.data.model.SearchUserResponse;

import java.util.ArrayList;
import java.util.List;

public class SearchUserAdapter extends RecyclerView.Adapter<SearchUserAdapter.ViewHolder> {

    public interface OnUserClickListener {
        void onAddFriend(SearchUserResponse.SearchUser user);
    }

    private List<SearchUserResponse.SearchUser> users = new ArrayList<>();
    private final OnUserClickListener listener;

    public SearchUserAdapter(OnUserClickListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<SearchUserResponse.SearchUser> users) {
        this.users = users != null ? users : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(users.get(position), listener);
    }

    @Override
    public int getItemCount() { return users.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView  tvName, tvEmail;
        Button    btnAddFriend;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar     = itemView.findViewById(R.id.ivAvatar);
            tvName       = itemView.findViewById(R.id.tvName);
            tvEmail      = itemView.findViewById(R.id.tvEmail);
            btnAddFriend = itemView.findViewById(R.id.btnAddFriend);
        }

        void bind(SearchUserResponse.SearchUser user, OnUserClickListener listener) {
            // Tên
            String name = user.fullName != null ? user.fullName : "User";
            tvName.setText(name);

            // Country thay email (backend không trả email trong search)
            String sub = user.country != null ? "🌍 " + user.country : "";
            tvEmail.setText(sub);

            // Avatar
            if (user.avatar != null && !user.avatar.isEmpty()
                    && !user.avatar.equals("default_avatar.png")) {
                Glide.with(itemView.getContext())
                        .load(user.avatar)
                        .placeholder(R.drawable.bg_avatar)
                        .circleCrop()
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
            }

            // Nút kết bạn — đổi label nếu đã là bạn
            if (user.isFriend) {
                btnAddFriend.setText("Bạn bè");
                btnAddFriend.setEnabled(false);
                btnAddFriend.setAlpha(0.5f);
            } else {
                btnAddFriend.setText("Kết bạn");
                btnAddFriend.setEnabled(true);
                btnAddFriend.setAlpha(1f);
                btnAddFriend.setOnClickListener(v -> listener.onAddFriend(user));
            }
        }
    }
}
