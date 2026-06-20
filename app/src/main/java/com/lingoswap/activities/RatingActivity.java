package com.lingoswap.activities;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.lingoswap.R;
import com.lingoswap.data.api.FriendApiService;
import com.lingoswap.data.api.MatchApiService;
import com.lingoswap.data.model.ApiResponse;
import com.lingoswap.data.model.FriendStatusResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class RatingActivity extends AppCompatActivity {

    @Inject MatchApiService  matchApiService;
    @Inject FriendApiService friendApiService;

    private int ratingOverall = 0;
    private int ratingPartner = 0;
    private final List<String> selectedChips = new ArrayList<>();

    private String sessionId;
    private String partnerId;
    private String partnerName;

    interface StarCallback { void onRating(int rating); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);

        int callDuration = getIntent().getIntExtra("callDuration", 0);
        sessionId   = getIntent().getStringExtra("sessionId");
        partnerId   = getIntent().getStringExtra("partnerId");
        partnerName = getIntent().getStringExtra("partnerName");

        TextView tvSessionTime = findViewById(R.id.tvSessionTime);
        if (tvSessionTime != null) {
            int mins = callDuration / 60;
            int secs = callDuration % 60;
            tvSessionTime.setText(String.format("%02d:%02d", mins, secs));
        }

        TextView tvPartnerName = findViewById(R.id.tvPartnerName);
        if (tvPartnerName != null) {
            tvPartnerName.setText(partnerName != null ? partnerName : "Partner");
        }

        LinearLayout starsOverall = findViewById(R.id.starsOverall);
        LinearLayout starsPartner = findViewById(R.id.starsPartner);
        setupStars(starsOverall, rating -> ratingOverall = rating);
        setupStars(starsPartner, rating -> ratingPartner = rating);

        int[] chipIds = {
            R.id.chipFriendly, R.id.chipPatient, R.id.chipPronunciation,
            R.id.chipVocabulary, R.id.chipEasyToUnderstand, R.id.chipEnthusiastic
        };
        for (int chipId : chipIds) {
            TextView chip = findViewById(chipId);
            if (chip != null) chip.setOnClickListener(v -> toggleChip(chip));
        }

        Button btnAddFriend = findViewById(R.id.btnAddFriend);
        if (partnerId == null) {
            if (btnAddFriend != null) btnAddFriend.setVisibility(View.GONE);
        } else {
            if (btnAddFriend != null) {
                btnAddFriend.setVisibility(View.VISIBLE);
                btnAddFriend.setOnClickListener(v -> sendFriendRequest(btnAddFriend));
            }
            checkFriendStatus(btnAddFriend);
        }

        Button btnReport = findViewById(R.id.btnReport);
        if (btnReport != null) {
            if (partnerId == null) {
                btnReport.setVisibility(View.GONE);
            } else {
                btnReport.setVisibility(View.VISIBLE);
                btnReport.setOnClickListener(v -> {
                    Intent intent = new Intent(this, com.lingoswap.presentation.report.ReportActivity.class);
                    intent.putExtra(com.lingoswap.presentation.report.ReportActivity.EXTRA_REPORTED_USER_ID, partnerId);
                    if (sessionId != null) {
                        intent.putExtra(com.lingoswap.presentation.report.ReportActivity.EXTRA_MATCH_SESSION_ID, sessionId);
                    }
                    startActivity(intent);
                });
            }
        }

        Button btnSkip   = findViewById(R.id.btnSkip);
        Button btnSubmit = findViewById(R.id.btnSubmitReview);
        EditText etComments = findViewById(R.id.etComments);

        if (btnSkip != null) btnSkip.setOnClickListener(v -> goHome());
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                if (ratingOverall == 0) {
                    Toast.makeText(this, "Vui lòng đánh giá trải nghiệm tổng thể",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                submitReview(ratingOverall,
                        etComments != null ? etComments.getText().toString() : "");
            });
        }
    }

    private void sendFriendRequest(Button btn) {
        btn.setEnabled(false);
        btn.setText("Đang gửi...");

        friendApiService.sendFriendRequest(partnerId)
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse> call,
                                           @NonNull Response<ApiResponse> response) {
                        if (response.isSuccessful()) {
                            btn.setText("Đã gửi lời mời");
                            btn.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_check, 0, 0, 0);
                            btn.setCompoundDrawablePadding(8);
                            Toast.makeText(RatingActivity.this,
                                    "Đã gửi lời mời kết bạn tới " + partnerName,
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            btn.setEnabled(true);
                            btn.setText("Kết bạn");
                            btn.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_add, 0, 0, 0);
                            btn.setCompoundDrawablePadding(8);
                            // Nếu đã là bạn bè hoặc đã gửi rồi
                            Toast.makeText(RatingActivity.this,
                                    "Đã gửi lời mời trước đó hoặc đã là bạn bè",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                        btn.setEnabled(true);
                        btn.setText("Kết bạn");
                        btn.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_add, 0, 0, 0);
                        btn.setCompoundDrawablePadding(8);
                        Toast.makeText(RatingActivity.this,
                                "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void submitReview(int rating, String comment) {
        if (sessionId == null) { goHome(); return; }

        Map<String, Object> body = new HashMap<>();
        body.put("rating",  rating);
        body.put("comment", comment);

        matchApiService.reviewSession(sessionId, body).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call,
                                   @NonNull Response<ApiResponse> response) {
                Toast.makeText(RatingActivity.this,
                        "Cảm ơn bạn đã đánh giá! ⭐", Toast.LENGTH_SHORT).show();
                goHome();
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                Toast.makeText(RatingActivity.this,
                        "Lỗi khi gửi đánh giá", Toast.LENGTH_SHORT).show();
                goHome();
            }
        });
    }

    private void setupStars(LinearLayout container, StarCallback callback) {
        if (container == null) return;
        int count = container.getChildCount();
        TextView[] stars = new TextView[count];
        for (int i = 0; i < count; i++) {
            View child = container.getChildAt(i);
            if (child instanceof TextView) stars[i] = (TextView) child;
        }
        for (int i = 0; i < count; i++) {
            final int index = i;
            if (stars[i] != null) {
                stars[i].setOnClickListener(v -> {
                    callback.onRating(index + 1);
                    updateStarUI(stars, index + 1);
                });
            }
        }
    }

    private void updateStarUI(TextView[] stars, int rating) {
        for (int j = 0; j < stars.length; j++) {
            if (stars[j] != null) {
                stars[j].setTextColor(ContextCompat.getColor(this,
                        j < rating ? R.color.yellow : R.color.border));
            }
        }
    }

    private void toggleChip(TextView chip) {
        String label = chip.getText().toString();
        boolean selected = selectedChips.contains(label);
        if (selected) {
            selectedChips.remove(label);
            chip.setBackgroundResource(R.drawable.bg_chip);
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_mid));
            chip.setTypeface(null, Typeface.NORMAL);
        } else {
            selectedChips.add(label);
            chip.setBackgroundResource(R.drawable.bg_chip_selected);
            chip.setTextColor(ContextCompat.getColor(this, R.color.blue));
            chip.setTypeface(null, Typeface.BOLD);
        }
    }

    private void checkFriendStatus(Button btnAddFriend) {
        if (partnerId == null || btnAddFriend == null) return;

        friendApiService.checkFriendStatus(partnerId).enqueue(new Callback<FriendStatusResponse>() {
            @Override
            public void onResponse(@NonNull Call<FriendStatusResponse> call,
                                   @NonNull Response<FriendStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String status = response.body().status;
                    if ("friends".equals(status)) {
                        btnAddFriend.setText("Bạn bè");
                        btnAddFriend.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_check, 0, 0, 0);
                        btnAddFriend.setCompoundDrawablePadding(8);
                        btnAddFriend.setEnabled(false);
                        btnAddFriend.setAlpha(0.6f);
                    } else if ("request_sent".equals(status) || "request_received".equals(status)) {
                        btnAddFriend.setText("Đã gửi lời mời");
                        btnAddFriend.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_check, 0, 0, 0);
                        btnAddFriend.setCompoundDrawablePadding(8);
                        btnAddFriend.setEnabled(false);
                        btnAddFriend.setAlpha(0.6f);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<FriendStatusResponse> call, @NonNull Throwable t) {
                // Ignore error, keep original state
            }
        });
    }

    private void goHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
