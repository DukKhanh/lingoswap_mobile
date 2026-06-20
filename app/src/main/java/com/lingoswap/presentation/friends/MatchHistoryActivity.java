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
import com.lingoswap.data.model.Friend;
import com.lingoswap.data.model.FriendRequest;
import com.lingoswap.data.model.FriendStatusResponse;
import com.lingoswap.data.model.MatchHistoryResponse;
import com.lingoswap.databinding.ActivityMatchHistoryBinding;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class MatchHistoryActivity extends AppCompatActivity {

    private ActivityMatchHistoryBinding binding;
    private MatchHistoryAdapter          adapter;

    private final Set<String> friendUserIds = new HashSet<>();
    private final Set<String> pendingUserIds = new HashSet<>();

    @Inject MatchApiService  matchApiService;
    @Inject FriendApiService friendApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMatchHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupViews();
        loadFriendsAndRequests();
    }

    private void setupViews() {
        binding.btnBack.setOnClickListener(v -> finish());

        adapter = new MatchHistoryAdapter(session -> sendFriendRequest(session));
        binding.rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSearchResults.setAdapter(adapter);
    }

    private void loadFriendsAndRequests() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvNoResults.setVisibility(View.GONE);
        friendUserIds.clear();
        pendingUserIds.clear();

        friendApiService.getFriends().enqueue(new Callback<List<Friend>>() {
            @Override
            public void onResponse(@NonNull Call<List<Friend>> call,
                                   @NonNull Response<List<Friend>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Friend f : response.body()) {
                        if (f.id != null) {
                            friendUserIds.add(f.id);
                        }
                    }
                }
                loadFriendRequests();
            }

            @Override
            public void onFailure(@NonNull Call<List<Friend>> call, @NonNull Throwable t) {
                loadFriendRequests();
            }
        });
    }

    private void loadFriendRequests() {
        friendApiService.getFriendRequests().enqueue(new Callback<List<FriendRequest>>() {
            @Override
            public void onResponse(@NonNull Call<List<FriendRequest>> call,
                                   @NonNull Response<List<FriendRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (FriendRequest req : response.body()) {
                        if (req.partner != null && req.partner.id != null) {
                            pendingUserIds.add(req.partner.id);
                        }
                    }
                }
                adapter.setFriendsAndRequests(friendUserIds, pendingUserIds);
                loadMatchHistory();
            }

            @Override
            public void onFailure(@NonNull Call<List<FriendRequest>> call, @NonNull Throwable t) {
                adapter.setFriendsAndRequests(friendUserIds, pendingUserIds);
                loadMatchHistory();
            }
        });
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
                            } else {
                                fetchStatusesForPartners(list);
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

    private void fetchStatusesForPartners(List<MatchHistoryResponse> sessions) {
        if (sessions == null || sessions.isEmpty()) return;
        Set<String> uniquePartnerIds = new HashSet<>();
        for (MatchHistoryResponse session : sessions) {
            if (session.partner != null && session.partner.id != null) {
                uniquePartnerIds.add(session.partner.id);
            }
        }

        for (String partnerId : uniquePartnerIds) {
            friendApiService.checkFriendStatus(partnerId).enqueue(new Callback<FriendStatusResponse>() {
                @Override
                public void onResponse(@NonNull Call<FriendStatusResponse> call,
                                       @NonNull Response<FriendStatusResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String status = response.body().status;
                        if ("friends".equals(status)) {
                            friendUserIds.add(partnerId);
                            pendingUserIds.remove(partnerId);
                        } else if ("request_sent".equals(status) || "request_received".equals(status)) {
                            pendingUserIds.add(partnerId);
                            friendUserIds.remove(partnerId);
                        } else {
                            friendUserIds.remove(partnerId);
                            pendingUserIds.remove(partnerId);
                        }
                        adapter.setFriendsAndRequests(friendUserIds, pendingUserIds);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<FriendStatusResponse> call, @NonNull Throwable t) {
                    // Ignore failure, keep loaded friends/requests state
                }
            });
        }
    }

    private void sendFriendRequest(MatchHistoryResponse session) {
        if (session.partner == null || session.partner.id == null) return;
        
        friendApiService.sendFriendRequest(session.partner.id)
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse> call,
                                           @NonNull Response<ApiResponse> response) {
                        if (response.isSuccessful()) {
                            pendingUserIds.add(session.partner.id);
                            adapter.setFriendsAndRequests(friendUserIds, pendingUserIds);
                            Toast.makeText(MatchHistoryActivity.this,
                                    "Đã gửi lời mời tới " + session.partner.getFullName(),
                                    Toast.LENGTH_SHORT).show();
                        } else if (response.code() == 400 || response.code() == 409) {
                            pendingUserIds.add(session.partner.id);
                            adapter.setFriendsAndRequests(friendUserIds, pendingUserIds);
                            Toast.makeText(MatchHistoryActivity.this,
                                    "Đã là bạn bè hoặc đã gửi lời mời trước đó",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MatchHistoryActivity.this,
                                    "Lỗi server (" + response.code() + "): Kiểm tra lại API Route",
                                    Toast.LENGTH_LONG).show();
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                        Toast.makeText(MatchHistoryActivity.this,
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
