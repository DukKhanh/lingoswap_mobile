package com.lingoswap.data.model.request;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    @SerializedName("email")           private String email;
    @SerializedName("password")        private String password;
    @SerializedName("confirmPassword") private String confirmPassword;
    @SerializedName("fullName")        private String fullName;
    @SerializedName("country")         private String country;

    public RegisterRequest(String email, String password, String confirmPassword, String fullName, String country) {
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.fullName = fullName;
        this.country = country;
    }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getConfirmPassword() { return confirmPassword; }
    public String getFullName() { return fullName; }
    public String getCountry() { return country; }
}
