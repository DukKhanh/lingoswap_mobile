package com.lingoswap.presentation.friends;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lingoswap.data.model.ApiResponse;
import com.lingoswap.data.model.Friend;
import com.lingoswap.data.model.FriendRequest;
import com.lingoswap.data.model.SearchUserResponse;
import com.lingoswap.data.repository.RepositoryCallback;
import com.lingoswap.domain.repository.FriendRepository;
import com.lingoswap.domain.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class FriendsViewModel extends ViewModel {

    private final FriendRepository repository;
    private final UserRepository   userRepository;

    public final MutableLiveData<List<Friend>>                    friends        = new MutableLiveData<>();
    public final MutableLiveData<List<FriendRequest>>             friendRequests = new MutableLiveData<>();
    public final MutableLiveData<List<SearchUserResponse.SearchUser>> searchResults  = new MutableLiveData<>();
    public final MutableLiveData<Boolean>                         isLoading      = new MutableLiveData<>(false);
    public final MutableLiveData<String>                          error          = new MutableLiveData<>();
    public final MutableLiveData<String>                          successMessage = new MutableLiveData<>();

    private List<Friend>        allFriends  = new ArrayList<>();
    private List<FriendRequest> allRequests = new ArrayList<>();

    @Inject
    public FriendsViewModel(FriendRepository repository, UserRepository userRepository) {
        this.repository     = repository;
        this.userRepository = userRepository;
        loadAll();
    }

    // ── Load ─────────────────────────────────────────────────────────────────

    public void loadAll() {
        loadFriends();
        loadFriendRequests();
    }

    public void loadFriends() {
        isLoading.setValue(true);
        repository.getFriends(new FriendRepository.Callback<List<Friend>>() {
            @Override public void onSuccess(List<Friend> data) {
                allFriends = data != null ? data : new ArrayList<>();
                friends.postValue(allFriends);
                isLoading.postValue(false);
            }
            @Override public void onError(String message) {
                error.postValue(message);
                isLoading.postValue(false);
            }
        });
    }

    public void loadFriendRequests() {
        repository.getFriendRequests(new FriendRepository.Callback<List<FriendRequest>>() {
            @Override public void onSuccess(List<FriendRequest> data) {
                allRequests = data != null ? data : new ArrayList<>();
                friendRequests.postValue(allRequests);
            }
            @Override public void onError(String message) {
                error.postValue(message);
            }
        });
    }

    // ── Search ────────────────────────────────────────────────────────────────

    public void searchNewUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            searchResults.setValue(new ArrayList<>());
            return;
        }
        isLoading.setValue(true);

        // FIX: dùng SearchUserResponse thay vì Map<String, Object>
        userRepository.searchUsers(query.trim(), 1, 20,
                new RepositoryCallback<SearchUserResponse>() {
                    @Override
                    public void onSuccess(SearchUserResponse data) {
                        List<SearchUserResponse.SearchUser> list =
                                data != null && data.getResults() != null
                                        ? data.getResults()
                                        : new ArrayList<>();
                        searchResults.postValue(list);
                        isLoading.postValue(false);
                    }
                    @Override
                    public void onError(String message) {
                        error.postValue(message);
                        isLoading.postValue(false);
                    }
                });
    }

    public void sendFriendRequest(String userId) {
        repository.sendFriendRequest(userId, new FriendRepository.Callback<ApiResponse>() {
            @Override public void onSuccess(ApiResponse data) {
                successMessage.postValue("Đã gửi lời mời kết bạn");
            }
            @Override public void onError(String message) {
                error.postValue(message);
            }
        });
    }

    // ── Filter ────────────────────────────────────────────────────────────────

    public void filterFriends(int tab, String query) {
        if (tab == 2) {
            List<Friend> requestList = new ArrayList<>();
            for (FriendRequest req : allRequests) {
                Friend f = Friend.fromRequest(req);
                f.id = req.id;
                if (matchesQuery(f.fullName, query)) requestList.add(f);
            }
            friends.setValue(requestList);
            return;
        }

        List<Friend> filtered = new ArrayList<>();
        for (Friend f : allFriends) {
            boolean onlineMatch = tab != 1 || f.isOnline();
            boolean queryMatch  = matchesQuery(f.fullName, query);
            if (onlineMatch && queryMatch) filtered.add(f);
        }
        friends.setValue(filtered);
    }

    private boolean matchesQuery(String name, String query) {
        if (query == null || query.trim().isEmpty()) return true;
        if (name == null) return false;
        return name.toLowerCase().contains(query.toLowerCase().trim());
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public void acceptFriendRequest(String friendshipId) {
        repository.respondFriendRequest(friendshipId, "accept",
                new FriendRepository.Callback<ApiResponse>() {
                    @Override public void onSuccess(ApiResponse data) {
                        successMessage.postValue("Đã chấp nhận lời mời kết bạn");
                        loadAll();
                    }
                    @Override public void onError(String message) {
                        error.postValue(message);
                    }
                });
    }

    public void rejectFriendRequest(String friendshipId) {
        repository.respondFriendRequest(friendshipId, "reject",
                new FriendRepository.Callback<ApiResponse>() {
                    @Override public void onSuccess(ApiResponse data) {
                        successMessage.postValue("Đã từ chối lời mời kết bạn");
                        loadFriendRequests();
                    }
                    @Override public void onError(String message) {
                        error.postValue(message);
                    }
                });
    }

    public void removeFriend(String friendId) {
        repository.removeFriend(friendId, new FriendRepository.Callback<ApiResponse>() {
            @Override public void onSuccess(ApiResponse data) {
                successMessage.postValue("Đã hủy kết bạn");
                loadFriends();
            }
            @Override public void onError(String message) {
                error.postValue(message);
            }
        });
    }
}
