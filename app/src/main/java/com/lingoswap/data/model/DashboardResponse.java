package com.lingoswap.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DashboardResponse {
    @SerializedName("greeting")          private String greeting;
    @SerializedName("stats")             private UserStats stats;
    @SerializedName("learningCalendar")  private List<Integer> learningCalendar;
    @SerializedName("suggestedPartners") private List<SuggestedPartner> suggestedPartners;

    public String getGreeting() { return greeting; }
    public UserStats getStats() { return stats; }
    public List<Integer> getLearningCalendar() { return learningCalendar; }
    public List<SuggestedPartner> getSuggestedPartners() { return suggestedPartners; }

    public static class SuggestedPartner {
        @SerializedName("_id")      private String id;
        @SerializedName("fullName") private String fullName;
        @SerializedName("avatar")   private String avatar;
        @SerializedName("country")  private String country;
        @SerializedName("isOnline") private boolean isOnline;

        public String getId() { return id; }
        public String getFullName() { return fullName; }
        public String getAvatar() { return avatar; }
        public String getCountry() { return country; }
        public boolean isOnline() { return isOnline; }
    }
}
