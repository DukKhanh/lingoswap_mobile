package com.lingoswap.data.model;
import com.google.gson.annotations.SerializedName;

public class ApiResponse {
    @SerializedName("message") public String message;
    @SerializedName("error")   public String error;
}
