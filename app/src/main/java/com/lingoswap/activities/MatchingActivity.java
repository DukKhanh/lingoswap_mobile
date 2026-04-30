package com.lingoswap.activities;

import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.lingoswap.R;

public class MatchingActivity extends AppCompatActivity {

    private int waitSeconds = 1;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private TextView tvWaitTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matching);

        String language = getIntent().getStringExtra("language");
        tvWaitTime = findViewById(R.id.tvWaitTime);
        Button btnCancelSearch = findViewById(R.id.btnCancelSearch);

        // Animate radar rings
        animateRadar();

        // Timer
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                tvWaitTime.setText("Waiting " + waitSeconds + "s");
                waitSeconds++;
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);

        // Simulate match found after 5s
        timerHandler.postDelayed(() -> {
            if (!isFinishing()) {
                Intent intent = new Intent(this, VideoCallActivity.class);
                intent.putExtra("language", language);
                startActivity(intent);
                finish();
            }
        }, 5000);

        btnCancelSearch.setOnClickListener(v -> finish());
    }

    private void animateRadar() {
        View ring1 = findViewById(R.id.radarRing1);
        View ring2 = findViewById(R.id.radarRing2);
        View ring3 = findViewById(R.id.radarRing3);

        pulseRing(ring1, 0);
        pulseRing(ring2, 400);
        pulseRing(ring3, 800);
    }

    private void pulseRing(View ring, long delay) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(ring, "scaleX", 0.95f, 1.05f);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(ring, "scaleY", 0.95f, 1.05f);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);

        ObjectAnimator alpha  = ObjectAnimator.ofFloat(ring, "alpha", 0.8f, 0.1f);
        alpha.setRepeatCount(ValueAnimator.INFINITE);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY, alpha);
        set.setDuration(2000);
        set.setStartDelay(delay);
        set.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
    }
}
