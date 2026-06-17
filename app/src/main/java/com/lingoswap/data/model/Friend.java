package com.lingoswap.data.model;

import com.google.gson.annotations.SerializedName;

public class Friend {
    @SerializedName("_id")   public String id;
    @SerializedName("conversationId") public String conversationId;
    
    // Server trả về các trường này trực tiếp ở cấp gốc (như trong Logcat)
    @SerializedName(value = "fullName", alternate = {"name", "full_name", "display_name", "displayName"})
    public String fullName;
    
    @SerializedName("avatar")         public String avatar;
    @SerializedName("email")          public String email;
    @SerializedName("status")         public String status; 
    
    @SerializedName("lastOnlineAt")   public TimeInfo lastOnlineAt;
    @SerializedName("partner")        public FriendPartner partner;
    @SerializedName("sentAt")         public TimeInfo sentAt;
    
    public boolean isPending = false;

    public boolean isOnline() { return "online".equals(status); }

    public static Friend fromRequest(FriendRequest request) {
        Friend f = new Friend();
        if (request.partner != null) {
            f.partner = request.partner;
            f.id = request.partner.id;
            f.fullName = request.partner.getDisplayName();
            f.avatar = request.partner.avatar;
            f.email = request.partner.email;
        }
        f.isPending = true;
        f.sentAt = request.sentAt;
        return f;
    }

    public static class FriendPartner {
        @SerializedName("_id")      public String id;
        
        // Dùng tên biến pUsername để tránh xung đột với key 'username' trong GSON
        @SerializedName(value = "username", alternate = {"userName", "user_name"})
        public String pUsername;
        
        @SerializedName(value = "fullName", alternate = {"name", "full_name", "display_name"}) 
        public String pFullName;
        
        @SerializedName("avatar")   public String avatar;
        @SerializedName("email")    public String email;

        public String getDisplayName() {
            if (pFullName != null && !pFullName.isEmpty()) return pFullName;
            if (pUsername != null && !pUsername.isEmpty()) return pUsername;
            if (email != null && !email.isEmpty()) {
                return email.split("@")[0];
            }
            return null;
        }
    }

    public static class TimeInfo {
        @SerializedName("full")     public String full;
        @SerializedName("friendly") public String friendly;
    }
}
