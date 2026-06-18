package com.lingoswap.data.model;

import com.google.gson.annotations.SerializedName;

/** Khớp với response GET /api/user/matches. */
public class MatchHistoryResponse {

    @SerializedName("_id")
    public String id;

    @SerializedName("language")
    public String language;

    @SerializedName("durationSeconds")
    public int durationSeconds;

    @SerializedName("startedAt")
    public String startedAt;

    @SerializedName("partner")
    public Partner partner;

    @SerializedName("conversationId")
    public String conversationId;

    @SerializedName("myReview")
    public Object myReview; // null nếu chưa review

    public boolean hasReview() { return myReview != null; }

    public String getFormattedDuration() {
        int mins = durationSeconds / 60;
        int secs = durationSeconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    public static class Partner {
        @SerializedName("_id")
        public String id;

        @SerializedName("profile")
        public Profile profile;

        @SerializedName("email")
        public String email;

        public String getFullName() {
            return profile != null && profile.fullName != null
                    ? profile.fullName : "LingoSwap User";
        }

        public String getAvatar() {
            return profile != null ? profile.avatar : null;
        }

        public static class Profile {
            @SerializedName("fullName") public String fullName;
            @SerializedName("avatar")   public String avatar;
        }
    }
}
