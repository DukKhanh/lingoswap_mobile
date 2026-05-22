package com.lingoswap.data.model;

import com.google.gson.annotations.SerializedName;

public class FriendRequest {
    @SerializedName("_id")
    public String id;          // friendshipId — used to accept/reject

    @SerializedName("partner")
    public Friend.FriendPartner partner;

    @SerializedName("sentAt")
    public Friend.TimeInfo sentAt;
}
