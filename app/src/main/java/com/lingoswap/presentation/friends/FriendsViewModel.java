package com.lingoswap.presentation.friends;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lingoswap.data.model.Friend;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class FriendsViewModel extends ViewModel {

    private final MutableLiveData<List<Friend>> _friends = new MutableLiveData<>();
    public LiveData<List<Friend>> friends = _friends;

    private final List<Friend> allFriends = new ArrayList<>();

    @Inject
    public FriendsViewModel() {
        loadMockData();
    }

    private void loadMockData() {
        allFriends.add(new Friend("Minh Tuan", "VI → EN", true, false));
        allFriends.add(new Friend("Sarah Lee", "EN → VI", true, false));
        allFriends.add(new Friend("Yuki Tanaka", "JP → VI", false, false));
        allFriends.add(new Friend("Carlos R.", "ES → EN", false, false));
        allFriends.add(new Friend("Linh Pham", "VI → FR", true, false));
        allFriends.add(new Friend("Alex K.", "EN → VI", false, true));
        _friends.setValue(new ArrayList<>(allFriends));
    }

    public void filterFriends(int tab, String query) {
        List<Friend> filtered = new ArrayList<>();
        String q = query.trim().toLowerCase();

        for (Friend f : allFriends) {
            boolean tabOk = tab == 0
                    || (tab == 1 && f.isOnline)
                    || (tab == 2 && f.isPending);
            boolean searchOk = q.isEmpty() || f.name.toLowerCase().contains(q);
            if (tabOk && searchOk) filtered.add(f);
        }
        _friends.setValue(filtered);
    }
}
