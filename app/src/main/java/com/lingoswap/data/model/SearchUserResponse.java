package com.lingoswap.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Khớp với response backend GET /api/users?q=... */
public class SearchUserResponse {

    @SerializedName("results")
    private List<SearchUser> results;

    @SerializedName("pagination")
    private Pagination pagination;

    public List<SearchUser> getResults()   { return results;    }
    public Pagination       getPagination(){ return pagination; }

    public static class SearchUser {
        @SerializedName("_id")
        public String id;

        @SerializedName("fullName")
        public String fullName;

        @SerializedName("avatar")
        public String avatar;

        @SerializedName("country")
        public String country;

        @SerializedName("isFriend")
        public boolean isFriend;

        @SerializedName("isOnline")
        public boolean isOnline;

        public String getId()       { return id;       }
        public String getFullName() { return fullName; }
        public String getEmail()    { return "";       } // backend không trả email trong search
        public String getAvatar()   { return avatar;   }
        public boolean isFriend()   { return isFriend; }
        public boolean isOnline()   { return isOnline; }

        public Profile getProfile() {
            Profile p = new Profile();
            p.fullName = fullName;
            p.avatar   = avatar;
            p.country  = country;
            return p;
        }

        public static class Profile {
            public String fullName;
            public String avatar;
            public String country;
            public String getFullName() { return fullName; }
            public String getAvatar()   { return avatar;   }
            public String getCountry()  { return country;  }
        }
    }

    public static class Pagination {
        @SerializedName("total")      public int total;
        @SerializedName("page")       public int page;
        @SerializedName("limit")      public int limit;
        @SerializedName("totalPages") public int totalPages;
    }
}
