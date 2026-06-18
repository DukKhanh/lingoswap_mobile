package com.lingoswap.presentation.home;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lingoswap.data.model.DashboardResponse;
import com.lingoswap.data.repository.RepositoryCallback;
import com.lingoswap.domain.repository.UserRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * HomeViewModel — nạp tổng quan Dashboard (greeting, streak, stats, lịch học,
 * gợi ý bạn học) từ GET /api/users/dashboard.
 */
@HiltViewModel
public class HomeViewModel extends ViewModel {

    private final UserRepository userRepository;

    public final MutableLiveData<DashboardResponse> dashboard = new MutableLiveData<>();
    public final MutableLiveData<Boolean>           isLoading = new MutableLiveData<>(false);
    public final MutableLiveData<String>            error     = new MutableLiveData<>();

    @Inject
    public HomeViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void loadDashboard() {
        isLoading.setValue(true);
        userRepository.getDashboard(new RepositoryCallback<DashboardResponse>() {
            @Override public void onSuccess(DashboardResponse data) {
                dashboard.postValue(data);
                isLoading.postValue(false);
            }
            @Override public void onError(String message) {
                error.postValue(message);
                isLoading.postValue(false);
            }
        });
    }
}
