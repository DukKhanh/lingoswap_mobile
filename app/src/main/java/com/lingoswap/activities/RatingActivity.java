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
import com.lingoswap.data.api.MatchApiService;
import com.lingoswap.data.model.ApiResponse;

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

    @Inject MatchApiService matchApiService;

    private int ratingOverall = 0;
    private int ratingPartner = 0;
    private final List<String> selectedChips = new ArrayList<>();
    
    private String sessionId;
    private String partnerId;

    interface StarCallback { void onRating(int rating); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);

        int callDuration = getIntent().getIntExtra("callDuration", 0);
        sessionId = getIntent().getStringExtra("sessionId");
        partnerId = getIntent().getStringExtra("partnerId");

        // ── Hiển thị thời lượng phiên gọi ─────────────────────────
        TextView tvSessionTime = findViewById(R.id.tvSessionTime);
        if (tvSessionTime != null) {
            int mins = callDuration / 60;
            int secs = callDuration % 60;
            tvSessionTime.setText(String.format("%02d:%02d", mins, secs));
        }

        // ── Star rating panels ─────────────────────────────────────
        LinearLayout starsOverall = findViewById(R.id.starsOverall);
        LinearLayout starsPartner = findViewById(R.id.starsPartner);
        setupStars(starsOverall, rating -> ratingOverall = rating);
        setupStars(starsPartner, rating -> ratingPartner = rating);

        // ── Chips ──────────────────────────────────────────────────
        int[] chipIds = {
            R.id.chipFriendly, R.id.chipPatient, R.id.chipPronunciation,
            R.id.chipVocabulary, R.id.chipEasyToUnderstand, R.id.chipEnthusiastic
        };
        for (int chipId : chipIds) {
            TextView chip = findViewById(chipId);
            if (chip != null) {
                chip.setOnClickListener(v -> toggleChip(chip));
            }
        }

        // ── Buttons ────────────────────────────────────────────────
        Button btnSkip   = findViewById(R.id.btnSkip);
        Button btnSubmit = findViewById(R.id.btnSubmitReview);
        EditText etComments = findViewById(R.id.etComments);

        if (btnSkip != null) btnSkip.setOnClickListener(v -> goHome());

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                if (ratingOverall == 0) {
                    Toast.makeText(this, "Vui lòng đánh giá trải nghiệm tổng thể", Toast.LENGTH_SHORT).show();
                    return;
                }

                submitReview(ratingOverall, etComments != null ? etComments.getText().toString() : "");
            });
        }
    }

    private void submitReview(int rating, String comment) {
        if (sessionId == null) {
            goHome();
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("rating",  rating);
        body.put("comment", comment);
        
        // Optional: include partner rating or chips if backend supports it
        // body.put("partnerRating", ratingPartner);
        // body.put("tags", selectedChips);

        matchApiService.reviewSession(sessionId, body).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                Toast.makeText(RatingActivity.this, "Cảm ơn bạn đã đánh giá! ⭐", Toast.LENGTH_SHORT).show();
                goHome();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                Toast.makeText(RatingActivity.this, "Lỗi khi gửi đánh giá", Toast.LENGTH_SHORT).show();
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
            if (child instanceof TextView) {
                stars[i] = (TextView) child;
            }
        }

        for (int i = 0; i < count; i++) {
            final int index = i;
            if (stars[i] != null) {
                stars[i].setOnClickListener(v -> {
                    int rating = index + 1;
                    callback.onRating(rating);
                    updateStarUI(stars, rating);
                });
            }
        }
    }

    private void updateStarUI(TextView[] stars, int rating) {
        for (int j = 0; j < stars.length; j++) {
            if (stars[j] != null) {
                if (j < rating) {
                    stars[j].setTextColor(ContextCompat.getColor(this, R.color.yellow));
                } else {
                    stars[j].setTextColor(ContextCompat.getColor(this, R.color.border));
                }
            }
        }
    }

    private void toggleChip(TextView chip) {
        String label = chip.getText().toString();
        boolean isSelected = selectedChips.contains(label);

        if (isSelected) {
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

    private void goHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
