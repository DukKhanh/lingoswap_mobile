package com.lingoswap.data.model;

import com.google.gson.annotations.SerializedName;

public class Notification {
    @SerializedName("_id")      private String id;
    @SerializedName("type")     private String type;
    @SerializedName("content")  private String content;
    @SerializedName("isRead")   private boolean isRead;
    @SerializedName("senderId") private NotifSender senderId;
    @SerializedName("metadata") private NotifMetadata metadata;
    @SerializedName("createdAt") private String createdAt;

    public String getId() { return id; }
    public String getType() { return type; }
    public String getContent() { return content; }
    public boolean isRead() { return isRead; }
    public NotifSender getSenderId() { return senderId; }
    public NotifMetadata getMetadata() { return metadata; }
    public String getCreatedAt() { return createdAt; }

    public static class NotifSender {
        @SerializedName("_id")     private String id;
        @SerializedName("profile") private UserProfile profile;

        public String getId() { return id; }
        public UserProfile getProfile() { return profile; }
    }

    public static class NotifMetadata {
        @SerializedName("friendshipId") private String friendshipId;
        @SerializedName("reportId")     private String reportId;

        public String getFriendshipId() { return friendshipId; }
        public String getReportId() { return reportId; }
    }
}
