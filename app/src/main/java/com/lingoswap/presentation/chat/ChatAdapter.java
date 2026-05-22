package com.lingoswap.presentation.chat;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.lingoswap.R;
import com.lingoswap.data.model.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * ChatAdapter — hiển thị danh sách tin nhắn trong RecyclerView.
 *
 * ViewType:
 *   0 = tin nhắn của mình (gravity END, bg_bubble_me)
 *   1 = tin nhắn đối phương (gravity START, bg_bubble_them)
 *
 * Hỗ trợ message type: "text", "image"
 * Payload update: truyền String để chỉ update tvMessageTranslation (partial bind)
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {

    // ── Listener ──────────────────────────────────────────────────────────────

    public interface Listener {
        void onLongPress(Message msg, int position);
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private List<Message> list = new ArrayList<>();
    private final String   currentUserId;
    private final Listener listener;

    public ChatAdapter(String currentUserId, Listener listener) {
        this.currentUserId = currentUserId;
        this.listener      = listener;
    }

    // ── Data API ──────────────────────────────────────────────────────────────

    public void setMessages(List<Message> newList) {
        this.list = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    public void addMessage(Message message) {
        // Tránh duplicate theo _id
        for (Message m : list) {
            if (m.getId() != null && m.getId().equals(message.getId())) return;
        }
        list.add(message);
        notifyItemInserted(list.size() - 1);
    }

    /** Cập nhật status (sent → delivered → read) cho 1 tin nhắn */
    public void updateMessageStatus(String messageId, String newStatus) {
        for (int i = 0; i < list.size(); i++) {
            if (messageId.equals(list.get(i).getId())) {
                list.get(i).setStatus(newStatus);
                notifyItemChanged(i);
                return;
            }
        }
    }

    /** Hiển thị bản dịch inline — dùng payload để chỉ rebind phần dịch */
    public void showTranslation(int position, String translatedText) {
        if (position >= 0 && position < list.size()) {
            notifyItemChanged(position, translatedText);
        }
    }

    // ── RecyclerView overrides ────────────────────────────────────────────────

    @Override
    public int getItemViewType(int pos) {
        Message m = list.get(pos);
        return (m.getSenderId() != null && m.getSenderId().equals(currentUserId)) ? 0 : 1;
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
        Message m      = list.get(position);
        boolean isMine = getItemViewType(position) == 0;

        // ── Reset trạng thái dịch ──────────────────────────────────────────
        h.tvTranslation.setVisibility(View.GONE);
        h.dividerTranslation.setVisibility(View.GONE);

        // ── Nội dung theo type ─────────────────────────────────────────────
        if ("image".equals(m.getType())) {
            h.tvText.setVisibility(View.GONE);
            h.ivImage.setVisibility(View.VISIBLE);
            Glide.with(h.ivImage.getContext())
                    .load(m.getContent())
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_image_placeholder)
                            .transform(new RoundedCorners(12)))
                    .into(h.ivImage);
        } else {
            h.tvText.setVisibility(View.VISIBLE);
            h.ivImage.setVisibility(View.GONE);
            h.tvText.setText(m.getContent());
        }

        // ── Timestamp ──────────────────────────────────────────────────────
        if (m.getCreatedAt() != null) {
            h.tvTime.setText(m.getCreatedAt().getFriendly());
        } else {
            h.tvTime.setText("");
        }

        // ── Status tick (chỉ hiện với tin nhắn của mình) ───────────────────
        if (isMine) {
            h.tvStatus.setVisibility(View.VISIBLE);
            switch (m.getStatus() != null ? m.getStatus() : "sent") {
                case "read":      h.tvStatus.setText("✓✓"); h.tvStatus.setAlpha(1f);   break;
                case "delivered": h.tvStatus.setText("✓✓"); h.tvStatus.setAlpha(0.5f); break;
                default:          h.tvStatus.setText("✓");  h.tvStatus.setAlpha(0.5f); break;
            }
        } else {
            h.tvStatus.setVisibility(View.GONE);
        }

        // ── Bubble style ───────────────────────────────────────────────────
        if (isMine) {
            h.root.setGravity(Gravity.END);
            h.bubble.setBackgroundResource(R.drawable.bg_bubble_me);

            int white    = 0xFFFFFFFF;
            int whiteDim = 0x99FFFFFF;
            h.tvText.setTextColor(white);
            h.tvTime.setTextColor(whiteDim);
            h.tvStatus.setTextColor(whiteDim);
            h.tvTranslation.setTextColor(0xCCFFFFFF);
            h.cvAvatar.setVisibility(View.GONE);
        } else {
            h.root.setGravity(Gravity.START);
            h.bubble.setBackgroundResource(R.drawable.bg_bubble_them);

            int dark  = h.tvText.getContext().getResources().getColor(R.color.text_dark,  null);
            int muted = h.tvTime.getContext().getResources().getColor(R.color.text_muted, null);
            h.tvText.setTextColor(dark);
            h.tvTime.setTextColor(muted);
            h.tvTranslation.setTextColor(muted);
            h.cvAvatar.setVisibility(View.VISIBLE);
        }

        // ── Long press ─────────────────────────────────────────────────────
        h.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongPress(m, h.getAdapterPosition());
            return true;
        });
    }

    /** Partial bind — chỉ cập nhật bản dịch, không redraw toàn bộ item */
    @Override
    public void onBindViewHolder(@NonNull VH h, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && payloads.get(0) instanceof String) {
            String translated = (String) payloads.get(0);
            h.tvTranslation.setText(translated);
            h.tvTranslation.setVisibility(View.VISIBLE);
            h.dividerTranslation.setVisibility(View.VISIBLE);
            return;
        }
        super.onBindViewHolder(h, position, payloads);
    }

    @Override
    public int getItemCount() { return list.size(); }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout root, bubble;
        TextView     tvText, tvTime, tvTranslation, tvStatus;
        ImageView    ivImage;
        View         cvAvatar, dividerTranslation;

        VH(View v) {
            super(v);
            root              = v.findViewById(R.id.layoutMessageRoot);
            bubble            = v.findViewById(R.id.layoutBubble);
            tvText            = v.findViewById(R.id.tvMessageText);
            tvTime            = v.findViewById(R.id.tvMessageTime);
            tvTranslation     = v.findViewById(R.id.tvMessageTranslation);
            tvStatus          = v.findViewById(R.id.tvMessageStatus);
            ivImage           = v.findViewById(R.id.ivMsgImage);
            cvAvatar          = v.findViewById(R.id.cvPartnerAvatar);
            dividerTranslation= v.findViewById(R.id.dividerTranslation);
        }
    }
}
