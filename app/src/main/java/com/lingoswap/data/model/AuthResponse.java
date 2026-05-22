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

    // Getters
    public String getId()          { return id; }
    public String getAccessToken() { return accessToken; }
    public String getEmail()       { return email; }
    public String getRole()        { return role; }
    public Profile getProfile()    { return profile; }
}
