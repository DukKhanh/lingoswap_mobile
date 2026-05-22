package com.lingoswap.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Model cuộc hội thoại — khớp với response của getAllConversation()
 * trong conversation.service.js (đã được enrich partner + lastMessage).
 */
public class Conversation {

    @SerializedName("_id")
    private String id;

    /** Thông tin người dùng đối diện (đã được enrich bởi backend) */
    @SerializedName("partner")
    private Partner partner;

    @SerializedName("lastMessage")
    private LastMessage lastMessage;

    @SerializedName("updatedAt")
    private Message.TimestampField updatedAt;

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getId()               { return id; }
    public Partner getPartner()         { return partner; }
    public LastMessage getLastMessage() { return lastMessage; }
    public Message.TimestampField getUpdatedAt() { return updatedAt; }

    // ── Nested: Partner ───────────────────────────────────────────────────────

    public static class Partner {
        @SerializedName("_id")
        private String id;

        @SerializedName("profile")
        private Profile profile;

        @SerializedName("email")
        private String email;

        /** "online" | "offline" — được gán bởi presenceService phía server */
        @SerializedName("status")
        private String status;

        @SerializedName("lastSeen")
        private Message.TimestampField lastSeen;

        public String getId()       { return id; }
        public Profile getProfile() { return profile; }
        public String getEmail()    { return email; }
        public String getStatus()   { return status; }
        public boolean isOnline()   { return "online".equals(status); }
        public Message.TimestampField getLastSeen() { return lastSeen; }

        public String getDisplayName() {
            if (profile != null && profile.getFullName() != null) return profile.getFullName();
            return email != null ? email : "LingoSwap User";
        }

        public String getAvatarUrl() {
            return profile != null ? profile.getAvatar() : null;
        }
    }

    public static class Profile {
        @SerializedName("fullName")
        private String fullName;

        @SerializedName("avatar")
        private String avatar;

        public String getFullName() { return fullName; }
        public String getAvatar()   { return avatar; }
    }

    // ── Nested: LastMessage ───────────────────────────────────────────────────

    public static class LastMessage {
        @SerializedName("content")
        private String content;

        @SerializedName("time")
        private Message.TimestampField time;

        public String getContent()              { return content; }
        public Message.TimestampField getTime() { return time; }

        public String getPreview() {
            if (content == null) return "";
            return content.length() > 50 ? content.substring(0, 50) + "…" : content;
        }
    }
}
