package com.lingoswap.data.model;

import com.google.gson.annotations.SerializedName;

public class FriendRequest {
    @SerializedName("_id")
    public String id;          // friendshipId

    // Backend có thể trả về 'partner', 'sender', 'from', hoặc 'requester'
    @SerializedName(value = "partner", alternate = {"sender", "from", "user", "requester"})
    public Friend.FriendPartner partner;

    @SerializedName("sentAt")
    public Friend.TimeInfo sentAt;

    public String getId() {
        return id;
    }
}
