package com.lingoswap.presentation.notification;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lingoswap.data.api.NotificationApiService;
import com.lingoswap.data.model.ApiResponse;
import com.lingoswap.data.model.Notification;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class NotificationViewModel extends ViewModel {

    private final NotificationApiService api;

    public final MutableLiveData<List<Notification>> notifications = new MutableLiveData<>();
    public final MutableLiveData<Integer>            unreadCount   = new MutableLiveData<>(0);
    public final MutableLiveData<Boolean>            isLoading     = new MutableLiveData<>(false);
    public final MutableLiveData<String>             error         = new MutableLiveData<>();
    public final MutableLiveData<String>             message       = new MutableLiveData<>();

    @Inject
    public NotificationViewModel(NotificationApiService api) {
        this.api = api;
    }

    public void loadNotifications() {
        isLoading.setValue(true);
        api.getNotifications(1, 50).enqueue(new Callback<List<Notification>>() {
            @Override public void onResponse(Call<List<Notification>> call, Response<List<Notification>> r) {
                isLoading.postValue(false);
                if (r.isSuccessful() && r.body() != null) notifications.postValue(r.body());
                else error.postValue("Không tải được thông báo (HTTP " + r.code() + ")");
            }
            @Override public void onFailure(Call<List<Notification>> call, Throwable t) {
                isLoading.postValue(false);
                error.postValue("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    public void loadUnreadCount() {
        api.getUnreadCount().enqueue(new Callback<Map<String, Integer>>() {
            @Override public void onResponse(Call<Map<String, Integer>> call, Response<Map<String, Integer>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    Integer c = r.body().get("unreadCount");
                    unreadCount.postValue(c != null ? c : 0);
                }
            }
            @Override public void onFailure(Call<Map<String, Integer>> call, Throwable t) { }
        });
    }

    public void markRead(String id) {
        api.markAsRead(id).enqueue(new Callback<ApiResponse>() {
            @Override public void onResponse(Call<ApiResponse> call, Response<ApiResponse> r) {
                Integer cur = unreadCount.getValue();
                if (cur != null && cur > 0) unreadCount.postValue(cur - 1);
            }
            @Override public void onFailure(Call<ApiResponse> call, Throwable t) { }
        });
    }

    public void markAllRead() {
        api.markAllAsRead().enqueue(new Callback<ApiResponse>() {
            @Override public void onResponse(Call<ApiResponse> call, Response<ApiResponse> r) {
                if (r.isSuccessful()) {
                    message.postValue("Đã đánh dấu tất cả đã đọc");
                    unreadCount.postValue(0);
                    loadNotifications();
                } else error.postValue("Không thực hiện được (HTTP " + r.code() + ")");
            }
            @Override public void onFailure(Call<ApiResponse> call, Throwable t) {
                error.postValue("Lỗi kết nối: " + t.getMessage());
            }
        });
    }
}
