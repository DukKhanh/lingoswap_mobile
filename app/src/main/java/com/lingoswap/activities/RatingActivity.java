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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.lingoswap.R;

import java.util.ArrayList;
import java.util.List;

public class RatingActivity extends AppCompatActivity {

    private int ratingOverall = 0;
    private int ratingPartner = 0;
    private final List<String> selectedChips = new ArrayList<>();

    // Callback interface để setupStars có thể gọi lại
    interface StarCallback { void onRating(int rating); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);

        int callDuration = getIntent().getIntExtra("callDuration", 0);

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

                // TODO: POST lên API review

                Toast.makeText(this, "Cảm ơn bạn đã đánh giá! ⭐", Toast.LENGTH_SHORT).show();
                goHome();
            });
        }
    }

    /** Thiết lập 5 ngôi sao có thể click */
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

    /** Cập nhật giao diện ngôi sao */
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

    /** Toggle chip: chọn / bỏ chọn */
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
