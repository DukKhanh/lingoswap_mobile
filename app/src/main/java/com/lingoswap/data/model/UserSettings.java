package com.lingoswap.data.model;

import com.google.gson.annotations.SerializedName;

public class UserSettings {
    @SerializedName("theme")      private String theme;      // "light" | "dark"
    @SerializedName("uiLanguage") private String uiLanguage;

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public String getUiLanguage() { return uiLanguage; }
    public void setUiLanguage(String uiLanguage) { this.uiLanguage = uiLanguage; }
}
