package com.lingoswap.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class UserStats {
    @SerializedName("streak")            private int streak;
    @SerializedName("totalHours")        private double totalHours;
    @SerializedName("totalSessions")     private int totalSessions;
    @SerializedName("lastStreakUpdate")  private String lastStreakUpdate;
    // /api/users/me trả learningCalendar lồng trong stats (mảng chuỗi ngày)
    @SerializedName("learningCalendar")  private List<String> learningCalendar;

    public int getStreak() { return streak; }
    public void setStreak(int streak) { this.streak = streak; }
    public double getTotalHours() { return totalHours; }
    public void setTotalHours(double totalHours) { this.totalHours = totalHours; }
    public int getTotalSessions() { return totalSessions; }
    public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }
    public String getLastStreakUpdate() { return lastStreakUpdate; }
    public void setLastStreakUpdate(String lastStreakUpdate) { this.lastStreakUpdate = lastStreakUpdate; }
    public List<String> getLearningCalendar() { return learningCalendar; }
    public void setLearningCalendar(List<String> learningCalendar) { this.learningCalendar = learningCalendar; }
}
