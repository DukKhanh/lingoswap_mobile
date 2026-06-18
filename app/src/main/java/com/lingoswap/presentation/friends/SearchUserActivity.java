package com.lingoswap.presentation.friends;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.lingoswap.data.api.FriendApiService;
import com.lingoswap.data.api.MatchApiService;
import com.lingoswap.data.model.ApiResponse;
import com.lingoswap.data.model.MatchHistoryResponse;
import com.lingoswap.databinding.ActivitySearchUserBinding;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class SearchUserActivity extends AppCompatActivity {

    private ActivitySearchUserBinding binding;
    private MatchHistoryAdapter        adapter;

    @Inject MatchApiService  matchApiService;
    @Inject FriendApiService friendApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupViews();
        loadMatchHistory();
    }

    private void setupViews() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.etSearch.setVisibility(View.GONE);

        adapter = new MatchHistoryAdapter(session -> sendFriendRequest(session));
        binding.rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSearchResults.setAdapter(adapter);
    }

    private void loadMatchHistory() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvNoResults.setVisibility(View.GONE);

        matchApiService.getMatchHistory(1, 50).enqueue(
                new Callback<List<MatchHistoryResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<MatchHistoryResponse>> call,
                                           @NonNull Response<List<MatchHistoryResponse>> response) {
                        binding.progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            List<MatchHistoryResponse> list = response.body();
                            adapter.setItems(list);
                            if (list.isEmpty()) {
                                binding.tvNoResults.setVisibility(View.VISIBLE);
                                binding.tvNoResults.setText("Chưa có lịch sử matching.");
                            }
                        } else {
                            showError("Không thể tải lịch sử matching");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<MatchHistoryResponse>> call,
                                          @NonNull Throwable t) {
                        binding.progressBar.setVisibility(View.GONE);
                        showError("Lỗi mạng: " + t.getMessage());
                    }
                });
    }

    private void sendFriendRequest(MatchHistoryResponse session) {
        if (session.partner == null || session.partner.id == null) return;
        
        friendApiService.sendFriendRequest(session.partner.id)
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse> call,
                                           @NonNull Response<ApiResponse> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(SearchUserActivity.this,
                                    "Đã gửi lời mời tới " + session.partner.getFullName(),
                                    Toast.LENGTH_SHORT).show();
                        } else if (response.code() == 400 || response.code() == 409) {
                            Toast.makeText(SearchUserActivity.this,
                                    "Đã là bạn bè hoặc đã gửi lời mời trước đó",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SearchUserActivity.this,
                                    "Lỗi server (" + response.code() + "): Kiểm tra lại API Route",
                                    Toast.LENGTH_LONG).show();
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                        Toast.makeText(SearchUserActivity.this,
                                "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void showError(String msg) {
        binding.tvNoResults.setVisibility(View.VISIBLE);
        binding.tvNoResults.setText(msg);
    }
}
