package com.lingoswap.presentation.chat;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lingoswap.R;
import com.lingoswap.activities.VideoCallActivity;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_FRIEND_NAME = "friend_name";
    public static final String EXTRA_FRIEND_LANGS = "friend_langs";
    public static final String EXTRA_FRIEND_ONLINE = "friend_online";

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageView btnSend, btnBack, btnVideoCall, btnEmoji;
    private TextView tvPartnerName, tvPartnerStatus, tvLangTag;
    private LinearLayout layoutTranslateTip;
    private TextView tvTranslationResult;

    private ChatViewModel viewModel;
    private ChatAdapter adapter;

    private String partnerName;
    private String partnerLangs;
    private boolean partnerOnline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        readExtras();
        bindViews();
        setupRecyclerView();
        setupInput();
        observeViewModel();
    }

    private void readExtras() {
        partnerName = getIntent().getStringExtra(EXTRA_FRIEND_NAME);
        partnerLangs = getIntent().getStringExtra(EXTRA_FRIEND_LANGS);
        partnerOnline = getIntent().getBooleanExtra(EXTRA_FRIEND_ONLINE, false);

        if (partnerName == null) partnerName = "LingoSwap User";
        if (partnerLangs == null) partnerLangs = "VI → EN";
    }

    private void bindViews() {
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        btnVideoCall = findViewById(R.id.btnVideoCall);
        btnEmoji = findViewById(R.id.btnEmoji);
        tvPartnerName = findViewById(R.id.tvPartnerName);
        tvPartnerStatus = findViewById(R.id.tvPartnerStatus);
        tvLangTag = findViewById(R.id.tvLangTag);
        layoutTranslateTip = findViewById(R.id.layoutTranslateTip);
        tvTranslationResult = findViewById(R.id.tvTranslationResult);

        tvPartnerName.setText(partnerName);
        tvPartnerStatus.setText(partnerOnline ? "Online" : "Offline");
        tvLangTag.setText(partnerLangs);

        btnBack.setOnClickListener(v -> finish());

        btnVideoCall.setOnClickListener(v -> {
            Intent intent = new Intent(this, VideoCallActivity.class);
            intent.putExtra("partnerName", partnerName);
            startActivity(intent);
        });

        btnEmoji.setOnClickListener(v ->
                Toast.makeText(this, "Emoji – coming soon!", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnCloseTranslate).setOnClickListener(v ->
                layoutTranslateTip.setVisibility(View.GONE));
    }

    private void setupRecyclerView() {
        adapter = new ChatAdapter(new ChatAdapter.Listener() {
            @Override
            public void onLongPress(com.lingoswap.data.model.Message msg, int position) {
                layoutTranslateTip.setVisibility(View.VISIBLE);
                tvTranslationResult.setText("Đang dịch: \"" + msg.text + "\"");
            }
        });

        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        rvMessages.setLayoutManager(llm);
        rvMessages.setAdapter(adapter);
    }

    private void setupInput() {
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                boolean hasText = !TextUtils.isEmpty(s.toString().trim());
                btnSend.setAlpha(hasText ? 1f : 0.45f);
            }
        });

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                viewModel.sendMessage(text);
                etMessage.setText("");
            }
        });
        btnSend.setAlpha(0.45f);
    }

    private void observeViewModel() {
        viewModel.messages.observe(this, messages -> {
            adapter.submitList(messages);
            rvMessages.smoothScrollToPosition(messages.size() - 1);
        });
    }
}
