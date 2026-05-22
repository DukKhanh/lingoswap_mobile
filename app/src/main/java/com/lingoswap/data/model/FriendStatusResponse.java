package com.lingoswap.data.model;

import com.google.gson.annotations.SerializedName;

public class FriendStatusResponse {
    // "none" | "friends" | "request_sent" | "request_received"
    @SerializedName("status")
    public String status;

    @SerializedName("friendshipId")
    public String friendshipId;
}
