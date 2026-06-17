package com.lingoswap.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FriendsResponse {
    @SerializedName("friends")
    private List<Friend> friends;

    @SerializedName("requests")
    private List<FriendRequest> requests;

    public List<Friend> getFriends() {
        return friends;
    }

    public List<FriendRequest> getRequests() {
        return requests;
    }
}
