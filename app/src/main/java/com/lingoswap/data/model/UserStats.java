package com.lingoswap.data.model;

import com.google.gson.annotations.SerializedName;

public class UserStats {
    @SerializedName("streak")         private int streak;
    @SerializedName("totalHours")     private double totalHours;
    @SerializedName("totalSessions")  private int totalSessions;

    public int getStreak() { return streak; }
    public void setStreak(int streak) { this.streak = streak; }
    public double getTotalHours() { return totalHours; }
    public void setTotalHours(double totalHours) { this.totalHours = totalHours; }
    public int getTotalSessions() { return totalSessions; }
    public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }
}
