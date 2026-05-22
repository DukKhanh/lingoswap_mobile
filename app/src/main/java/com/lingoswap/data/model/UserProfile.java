package com.lingoswap.data.model;

import com.google.gson.annotations.SerializedName;

public class UserProfile {
    @SerializedName("fullName") private String fullName;
    @SerializedName("avatar")   private String avatar;
    @SerializedName("bio")      private String bio;
    @SerializedName("country")  private String country;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}
