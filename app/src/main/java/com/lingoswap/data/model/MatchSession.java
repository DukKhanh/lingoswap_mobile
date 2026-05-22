package com.lingoswap.data.model;

import com.google.gson.annotations.SerializedName;

public class MatchSession {
    @SerializedName("_id")             private String id;
    @SerializedName("status")          private String status;
    @SerializedName("durationSeconds") private int durationSeconds;
    @SerializedName("conversationId")  private String conversationId;
    @SerializedName("partner")         private MatchPartner partner;

    public String getId() { return id; }
    public String getStatus() { return status; }
    public int getDurationSeconds() { return durationSeconds; }
    public String getConversationId() { return conversationId; }
    public MatchPartner getPartner() { return partner; }

    public static class MatchPartner {
        @SerializedName("_id")     private String id;
        @SerializedName("profile") private UserProfile profile;

        public String getId() { return id; }
        public UserProfile getProfile() { return profile; }
    }
}
