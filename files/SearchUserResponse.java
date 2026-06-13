package com.lingoswap.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Khớp với response backend GET /api/users?q=...
 * {
 *   "results": [ { _id, fullName, avatar, country, isFriend, isOnline } ],
 *   "pagination": { total, page, limit, totalPages }
 * }
 */
public class SearchUserResponse {

    @SerializedName("results")
    private List<SearchUser> results;

    @SerializedName("pagination")
    private Pagination pagination;

    public List<SearchUser> getResults()   { return results;    }
    public Pagination       getPagination(){ return pagination; }

    // ── SearchUser — flat object từ backend ───────────────────────────

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

        // Helper cho SearchUserAdapter
        public String getId()       { return id;       }
        public String getFullName() { return fullName; }
        public String getEmail()    { return "";       } // backend không trả email trong search
        public String getAvatar()   { return avatar;   }
        public boolean isFriend()   { return isFriend; }
        public boolean isOnline()   { return isOnline; }

        // Tương thích với SearchUserAdapter đang dùng user.getProfile().getFullName()
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

    // ── Pagination ────────────────────────────────────────────────────

    public static class Pagination {
        @SerializedName("total")      public int total;
        @SerializedName("page")       public int page;
        @SerializedName("limit")      public int limit;
        @SerializedName("totalPages") public int totalPages;
    }
}
