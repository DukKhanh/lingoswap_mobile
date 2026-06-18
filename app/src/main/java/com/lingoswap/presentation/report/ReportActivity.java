package com.lingoswap.presentation.report;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.lingoswap.R;
import com.lingoswap.data.api.ReportApiService;
import com.lingoswap.data.model.ApiResponse;
import com.lingoswap.utils.ErrorUtils;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ReportActivity — báo cáo vi phạm (api-doc 9.1). Dùng chung cho Chat / Call / Friends.
 *
 * Intent extras:
 *  - EXTRA_REPORTED_USER_ID (bắt buộc)
 *  - EXTRA_CONVERSATION_ID  (tuỳ chọn)
 *  - EXTRA_MATCH_SESSION_ID (tuỳ chọn)
 */
@AndroidEntryPoint
public class ReportActivity extends AppCompatActivity {

    public static final String EXTRA_REPORTED_USER_ID = "reportedUserId";
    public static final String EXTRA_CONVERSATION_ID  = "conversationId";
    public static final String EXTRA_MATCH_SESSION_ID = "matchSessionId";

    @Inject ReportApiService reportApi;

    private RadioGroup rgReasons;
    private EditText   etDescription;
    private Button     btnSubmit;

    private String reportedUserId;
    private String conversationId;
    private String matchSessionId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        reportedUserId = getIntent().getStringExtra(EXTRA_REPORTED_USER_ID);
        conversationId = getIntent().getStringExtra(EXTRA_CONVERSATION_ID);
        matchSessionId = getIntent().getStringExtra(EXTRA_MATCH_SESSION_ID);

        rgReasons     = findViewById(R.id.rgReasons);
        etDescription = findViewById(R.id.etDescription);
        btnSubmit     = findViewById(R.id.btnSubmitReport);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rgReasons.setOnCheckedChangeListener((group, checkedId) ->
                etDescription.setVisibility(checkedId == R.id.rbOther ? View.VISIBLE : View.GONE));

        btnSubmit.setOnClickListener(v -> submit());
    }

    private String selectedReason() {
        int id = rgReasons.getCheckedRadioButtonId();
        if (id == R.id.rbSpam)            return "Spam";
        if (id == R.id.rbHarass)          return "Quấy rối";
        if (id == R.id.rbInappropriate)   return "Ngôn từ không phù hợp";
        if (id == R.id.rbImpersonation)   return "Giả mạo danh tính";
        if (id == R.id.rbOther)           return etDescription.getText().toString().trim();
        return null;
    }

    private void submit() {
        if (TextUtils.isEmpty(reportedUserId)) {
            Toast.makeText(this, "Thiếu thông tin người bị báo cáo", Toast.LENGTH_SHORT).show();
            return;
        }
        String reason = selectedReason();
        if (TextUtils.isEmpty(reason)) {
            Toast.makeText(this, "Vui lòng chọn lý do báo cáo", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("reportedUserId", reportedUserId);
        body.put("reason", reason);
        if (conversationId != null) body.put("conversationId", conversationId);
        if (matchSessionId != null) body.put("matchSessionId", matchSessionId);

        btnSubmit.setEnabled(false);
        reportApi.sendReport(body).enqueue(new Callback<ApiResponse>() {
            @Override public void onResponse(Call<ApiResponse> call, Response<ApiResponse> r) {
                btnSubmit.setEnabled(true);
                if (r.isSuccessful()) {
                    Toast.makeText(ReportActivity.this,
                            "Báo cáo đã được ghi lại thành công", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(ReportActivity.this, ErrorUtils.parseError(r), Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(Call<ApiResponse> call, Throwable t) {
                btnSubmit.setEnabled(true);
                Toast.makeText(ReportActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
