package com.lingoswap.data.model;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {

    @SerializedName("id")
    private String id;

    @SerializedName("token")
    private String accessToken;

    @SerializedName("email")
    private String email;

    @SerializedName("role")
    private String role;

    @SerializedName("profile")
    private Profile profile;

    public String  getId()          { return id; }
    public String  getAccessToken() { return accessToken; }
    public String  getEmail()       { return email; }
    public String  getRole()        { return role; }
    public Profile getProfile()     { return profile; }

    public static class Profile {

        @SerializedName("fullName")
        private String fullName;

        @SerializedName("avatar")
        private String avatar;

        @SerializedName("bio")
        private String bio;

        @SerializedName("country")
        private String country;

        public String getFullName() { return fullName; }
        public String getAvatar()   { return avatar;   }
        public String getBio()      { return bio;       }
        public String getCountry()  { return country;  }
    }
}
