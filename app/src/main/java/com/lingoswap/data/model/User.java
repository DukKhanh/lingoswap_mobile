package com.lingoswap.data.model;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("_id")           private String id;
    @SerializedName("email")         private String email;
    @SerializedName("profile")       private UserProfile profile;
    @SerializedName("settings")      private UserSettings settings;
    @SerializedName("stats")         private UserStats stats;
    @SerializedName("role")          private String role;          // "user" | "admin"
    @SerializedName("statusAccount") private String statusAccount; // "active"|"warned"|"banned"
    @SerializedName("status")        private String status;        // "idle"|"waiting"|"in-call"

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public UserProfile getProfile() { return profile; }
    public void setProfile(UserProfile profile) { this.profile = profile; }
    public UserSettings getSettings() { return settings; }
    public void setSettings(UserSettings settings) { this.settings = settings; }
    public UserStats getStats() { return stats; }
    public void setStats(UserStats stats) { this.stats = stats; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatusAccount() { return statusAccount; }
    public void setStatusAccount(String statusAccount) { this.statusAccount = statusAccount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
