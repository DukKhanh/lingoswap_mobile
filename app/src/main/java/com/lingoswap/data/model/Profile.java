package com.lingoswap.data.model;

import com.google.gson.annotations.SerializedName;

public class Profile {
    @SerializedName("fullName")
    private String fullName;

    @SerializedName("avatar")
    private String avatar;

    @SerializedName("bio")
    private String bio;

    @SerializedName("country")
    private String country;

    public String getFullName() { return fullName; }
    public String getAvatar()   { return avatar; }
    public String getBio()      { return bio; }
    public String getCountry()  { return country; }
}
