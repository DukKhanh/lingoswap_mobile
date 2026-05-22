package com.lingoswap.data.model;

import com.google.gson.annotations.SerializedName;

public class Friend {
    @SerializedName("_id")   public String id;
    @SerializedName("conversationId") public String conversationId;
    @SerializedName("fullName")       public String fullName;
    @SerializedName("avatar")         public String avatar;
    @SerializedName("email")          public String email;
    @SerializedName("status")         public String status; // "online" | "offline"
    @SerializedName("partner")        public FriendPartner partner;
    @SerializedName("sentAt")         public TimeInfo sentAt;
    public boolean isPending = false;

    public boolean isOnline() { return "online".equals(status); }

    public static Friend fromRequest(FriendRequest request) {
        Friend f = new Friend();
        if (request.partner != null) {
            f.id = request.partner.id;
            f.fullName = request.partner.username;
            f.avatar = request.partner.avatar;
        }
        f.isPending = true;
        f.sentAt = request.sentAt;
        return f;
    }

    public static class FriendPartner {
        @SerializedName("_id")      public String id;
        @SerializedName("username") public String username;
        @SerializedName("avatar")   public String avatar;
        @SerializedName("email")    public String email;
    }

    public static class TimeInfo {
        @SerializedName("full")     public String full;
        @SerializedName("friendly") public String friendly;
    }
}
