package com.lingoswap.presentation.friends;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lingoswap.data.model.ApiResponse;
import com.lingoswap.data.model.Friend;
import com.lingoswap.data.model.FriendRequest;
import com.lingoswap.domain.repository.FriendRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class FriendsViewModel extends ViewModel {

    private final FriendRepository repository;

    // Raw data from API
    private final List<Friend> allFriends = new ArrayList<>();
    private final List<FriendRequest> allRequests = new ArrayList<>();

    // Exposed to UI
    private final MutableLiveData<List<Friend>> _friends = new MutableLiveData<>();
    public final LiveData<List<Friend>> friends = _friends;

    private final MutableLiveData<List<FriendRequest>> _friendRequests = new MutableLiveData<>();
    public final LiveData<List<FriendRequest>> friendRequests = _friendRequests;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public final LiveData<String> error = _error;

    private final MutableLiveData<String> _successMessage = new MutableLiveData<>();
    public final LiveData<String> successMessage = _successMessage;

    // Tracks current tab (0=All, 1=Online, 2=Requests) and search query
    private int currentTab = 0;
    private String currentQuery = "";

    @Inject
    public FriendsViewModel(FriendRepository repository) {
        this.repository = repository;
        loadFriends();
        loadFriendRequests();
    }

    // ─── Load ────────────────────────────────────────────────────────────────

    public void loadFriends() {
        _isLoading.setValue(true);
        repository.getFriends(new FriendRepository.Callback<List<Friend>>() {
            @Override
            public void onSuccess(List<Friend> data) {
                allFriends.clear();
                allFriends.addAll(data);
                _isLoading.postValue(false);
                applyFilter();
            }
            @Override
            public void onError(String message) {
                _isLoading.postValue(false);
                _error.postValue(message);
            }
        });
    }

    public void loadFriendRequests() {
        repository.getFriendRequests(new FriendRepository.Callback<List<FriendRequest>>() {
            @Override
            public void onSuccess(List<FriendRequest> data) {
                allRequests.clear();
                allRequests.addAll(data);
                _friendRequests.postValue(new ArrayList<>(allRequests));
                // Update filter in case we're on tab 2
                applyFilter();
            }
            @Override
            public void onError(String message) {
                _error.postValue(message);
            }
        });
    }

    // ─── Filter ──────────────────────────────────────────────────────────────

    /**
     * Called from Activity when tab or search query changes.
     * Tab 0 = All friends, Tab 1 = Online only, Tab 2 = Pending requests
     */
    public void filterFriends(int tab, String query) {
        currentTab = tab;
        currentQuery = query;
        applyFilter();
    }

    private void applyFilter() {
        String q = currentQuery.trim().toLowerCase();

        if (currentTab == 2) {
            // Show pending friend requests
            List<Friend> requestsAsFriends = new ArrayList<>();
            for (FriendRequest req : allRequests) {
                Friend f = Friend.fromRequest(req);
                boolean searchOk = q.isEmpty()
                        || (f.fullName != null && f.fullName.toLowerCase().contains(q));
                if (searchOk) requestsAsFriends.add(f);
            }
            _friends.postValue(requestsAsFriends);
        } else {
            List<Friend> filtered = new ArrayList<>();
            for (Friend f : allFriends) {
                boolean tabOk = currentTab == 0 || (currentTab == 1 && f.isOnline());
                boolean searchOk = q.isEmpty()
                        || (f.fullName != null && f.fullName.toLowerCase().contains(q));
                if (tabOk && searchOk) filtered.add(f);
            }
            _friends.postValue(filtered);
        }
    }

    // ─── Actions ─────────────────────────────────────────────────────────────

    public void acceptFriendRequest(String requestId) {
        repository.respondFriendRequest(requestId, "accept", new FriendRepository.Callback<ApiResponse>() {
            @Override
            public void onSuccess(ApiResponse data) {
                _successMessage.postValue("Đã chấp nhận lời mời kết bạn");
                loadFriends();
                loadFriendRequests();
            }
            @Override
            public void onError(String message) {
                _error.postValue(message);
            }
        });
    }

    public void rejectFriendRequest(String requestId) {
        repository.respondFriendRequest(requestId, "reject", new FriendRepository.Callback<ApiResponse>() {
            @Override
            public void onSuccess(ApiResponse data) {
                _successMessage.postValue("Đã từ chối lời mời kết bạn");
                loadFriendRequests();
            }
            @Override
            public void onError(String message) {
                _error.postValue(message);
            }
        });
    }

    public void removeFriend(String friendId) {
        repository.removeFriend(friendId, new FriendRepository.Callback<ApiResponse>() {
            @Override
            public void onSuccess(ApiResponse data) {
                _successMessage.postValue("Đã hủy kết bạn");
                loadFriends();
            }
            @Override
            public void onError(String message) {
                _error.postValue(message);
            }
        });
    }

    public void sendFriendRequest(String recipientId) {
        repository.sendFriendRequest(recipientId, new FriendRepository.Callback<ApiResponse>() {
            @Override
            public void onSuccess(ApiResponse data) {
                _successMessage.postValue("Đã gửi yêu cầu kết bạn");
            }
            @Override
            public void onError(String message) {
                _error.postValue(message);
            }
        });
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    public int getCurrentTab() {
        return currentTab;
    }

    public int getPendingRequestCount() {
        return allRequests.size();
    }
}
