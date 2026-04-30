package com.example.lingoswap;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ChatActivity – Màn hình nhắn tin với một bạn bè
 * XML: activity_chat.xml  +  item_message.xml
 *
 * Extras mong đợi:
 *   EXTRA_FRIEND_NAME   (String)  – tên đối phương
 *   EXTRA_FRIEND_LANGS  (String)  – e.g. "VI → EN"
 *   EXTRA_FRIEND_ONLINE (boolean) – trạng thái online
 */
public class ChatActivity extends AppCompatActivity {

    // ── Intent extras ──────────────────────────────────────────────
    public static final String EXTRA_FRIEND_NAME   = "friend_name";
    public static final String EXTRA_FRIEND_LANGS  = "friend_langs";
    public static final String EXTRA_FRIEND_ONLINE = "friend_online";

    // ── Views ──────────────────────────────────────────────────────
    private RecyclerView  rvMessages;
    private EditText      etMessage;
    private ImageView     btnSend, btnBack, btnVideoCall, btnEmoji;
    private TextView      tvPartnerName, tvPartnerStatus, tvLangTag;
    private LinearLayout  layoutTranslateTip;
    private TextView      tvTranslationResult;

    // ── Data ───────────────────────────────────────────────────────
    private ChatAdapter       adapter;
    private final List<Message> messages = new ArrayList<>();

    private String partnerName;
    private String partnerLangs;
    private boolean partnerOnline;

    // ── Lifecycle ──────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        readExtras();
        bindViews();
        setupRecyclerView();
        setupInput();
        loadMockHistory();
    }

    // ── Extras ─────────────────────────────────────────────────────
    private void readExtras() {
        partnerName   = getIntent().getStringExtra(EXTRA_FRIEND_NAME);
        partnerLangs  = getIntent().getStringExtra(EXTRA_FRIEND_LANGS);
        partnerOnline = getIntent().getBooleanExtra(EXTRA_FRIEND_ONLINE, false);

        if (partnerName  == null) partnerName  = "LingoSwap User";
        if (partnerLangs == null) partnerLangs = "VI → EN";
    }

    // ── Bind ───────────────────────────────────────────────────────
    private void bindViews() {
        rvMessages          = findViewById(R.id.rvMessages);
        etMessage           = findViewById(R.id.etMessage);
        btnSend             = findViewById(R.id.btnSend);
        btnBack             = findViewById(R.id.btnBack);
        btnVideoCall        = findViewById(R.id.btnVideoCall);
        btnEmoji            = findViewById(R.id.btnEmoji);
        tvPartnerName       = findViewById(R.id.tvPartnerName);
        tvPartnerStatus     = findViewById(R.id.tvPartnerStatus);
        tvLangTag           = findViewById(R.id.tvLangTag);
        layoutTranslateTip  = findViewById(R.id.layoutTranslateTip);
        tvTranslationResult = findViewById(R.id.tvTranslationResult);

        tvPartnerName.setText(partnerName);
        tvPartnerStatus.setText(partnerOnline ? "Online" : "Offline");
        tvLangTag.setText(partnerLangs);

        btnBack.setOnClickListener(v -> finish());

        btnVideoCall.setOnClickListener(v -> {
            android.content.Intent intent =
                    new android.content.Intent(this, VideoCallActivity.class);
            intent.putExtra("partnerName", partnerName);
            startActivity(intent);
        });

        btnEmoji.setOnClickListener(v ->
                android.widget.Toast.makeText(this, "Emoji – coming soon!", android.widget.Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnCloseTranslate).setOnClickListener(v ->
                layoutTranslateTip.setVisibility(View.GONE));
    }

    // ── RecyclerView ───────────────────────────────────────────────
    private void setupRecyclerView() {
        adapter = new ChatAdapter(messages, new ChatAdapter.Listener() {
            @Override
            public void onLongPress(Message msg, int position) {
                // Giả lập dịch – thực tế gọi Translation API
                layoutTranslateTip.setVisibility(View.VISIBLE);
                tvTranslationResult.setText("Đang dịch: \"" + msg.text + "\"");
            }
        });

        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        rvMessages.setLayoutManager(llm);
        rvMessages.setAdapter(adapter);
    }

    // ── Input ──────────────────────────────────────────────────────
    private void setupInput() {
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                boolean hasText = !TextUtils.isEmpty(s.toString().trim());
                btnSend.setAlpha(hasText ? 1f : 0.45f);
            }
        });

        btnSend.setOnClickListener(v -> sendMessage());
        btnSend.setAlpha(0.45f);
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        messages.add(new Message(text, time, true));
        adapter.notifyItemInserted(messages.size() - 1);
        rvMessages.smoothScrollToPosition(messages.size() - 1);
        etMessage.setText("");

        // Giả lập đối phương trả lời sau 1.5 giây
        rvMessages.postDelayed(() -> simulateReply(), 1500);
    }

    private void simulateReply() {
        String[] replies = {
                "That's great! 👏",
                "Bạn nói rất tốt!",
                "Cảm ơn bạn nhé 😊",
                "Let's keep practicing!",
                "Tôi hiểu rồi, cảm ơn!"
        };
        String text = replies[(int) (Math.random() * replies.length)];
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        messages.add(new Message(text, time, false));
        adapter.notifyItemInserted(messages.size() - 1);
        rvMessages.smoothScrollToPosition(messages.size() - 1);
    }

    // ── Mock history ───────────────────────────────────────────────
    private void loadMockHistory() {
        messages.add(new Message("Xin chào! Bạn có muốn luyện tiếng Anh cùng mình không?", "10:20", false));
        messages.add(new Message("Chào bạn! Mình rất vui được luyện cùng bạn 😊", "10:21", true));
        messages.add(new Message("Great! Let's start. How long have you been learning English?", "10:21", false));
        messages.add(new Message("I've been learning for about 2 years. My speaking is still weak.", "10:22", true));
        messages.add(new Message("Đừng lo, luyện tập thường xuyên là bí quyết! 💪", "10:23", false));
        adapter.notifyDataSetChanged();
        rvMessages.scrollToPosition(messages.size() - 1);
    }

    // ══════════════════════════════════════════════════════════════
    //  Model
    // ══════════════════════════════════════════════════════════════
    public static class Message {
        public final String  text;
        public final String  time;
        public final boolean isMine;   // true = tin mình gửi

        public Message(String text, String time, boolean isMine) {
            this.text   = text;
            this.time   = time;
            this.isMine = isMine;
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  Adapter
    // ══════════════════════════════════════════════════════════════
    public static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {

        public interface Listener {
            void onLongPress(Message msg, int position);
        }

        private final List<Message> list;
        private final Listener      listener;

        public ChatAdapter(List<Message> list, Listener listener) {
            this.list     = list;
            this.listener = listener;
        }

        /** viewType 0 = tin của mình, 1 = tin đối phương */
        @Override public int getItemViewType(int pos) { return list.get(pos).isMine ? 0 : 1; }

        @NonNull @Override
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
                // Căn phải, màu xanh
                h.root.setGravity(Gravity.END);
                h.bubble.setBackgroundResource(R.drawable.bg_bubble_me);
                h.tvText.setTextColor(0xFFFFFFFF);
                h.tvTime.setTextColor(0x99FFFFFF);
                h.cvAvatar.setVisibility(View.GONE);
            } else {
                // Căn trái, màu trắng
                h.root.setGravity(Gravity.START);
                h.bubble.setBackgroundResource(R.drawable.bg_bubble_them);
                h.tvText.setTextColor(h.tvText.getContext()
                        .getResources().getColor(R.color.text_dark));
                h.tvTime.setTextColor(h.tvTime.getContext()
                        .getResources().getColor(R.color.text_muted));
                h.cvAvatar.setVisibility(View.VISIBLE);
            }

            h.itemView.setOnLongClickListener(v -> {
                listener.onLongPress(m, position);
                return true;
            });
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            LinearLayout root, bubble;
            TextView     tvText, tvTime;
            View         cvAvatar;

            VH(View v) {
                super(v);
                root     = v.findViewById(R.id.layoutMessageRoot);
                bubble   = v.findViewById(R.id.layoutBubble);
                tvText   = v.findViewById(R.id.tvMessageText);
                tvTime   = v.findViewById(R.id.tvMessageTime);
                cvAvatar = v.findViewById(R.id.cvPartnerAvatar);
            }
        }
    }
}
