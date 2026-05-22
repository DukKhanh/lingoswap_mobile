package com.lingoswap.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Model tin nhắn — khớp với backend Message schema + conversation.service.js format.
 *
 * Backend trả về createdAt dạng object { full, friendly } sau khi đi qua
 * getMessagesByConversation(). Socket event trả về Date string thô (ISO).
 * → dùng TimestampField để xử lý cả hai trường hợp.
 */
public class Message {

    @SerializedName("_id")
    private String id;

    @SerializedName("conversationId")
    private String conversationId;

    @SerializedName("senderId")
    private String senderId;

    /** "text" | "image" | "system" | "game_taboo" | "game_canvas_draw" */
    @SerializedName("type")
    private String type;

    @SerializedName("content")
    private String content;

    /** "sent" | "delivered" | "read" */
    @SerializedName("status")
    private String status;

    /**
     * REST API (getMessagesByConversation) trả về object { full, friendly }.
     * Socket (receive_message / message_sent_success) trả về ISO string thô.
     * → dùng TimestampField để deserialize linh hoạt.
     */
    @SerializedName("createdAt")
    private TimestampField createdAt;

    @SerializedName("grammarCorrection")
    private GrammarCorrection grammarCorrection;

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getId()             { return id; }
    public String getConversationId() { return conversationId; }
    public String getSenderId()       { return senderId; }
    public String getType()           { return type != null ? type : "text"; }
    public String getContent()        { return content; }
    public String getStatus()         { return status; }
    public TimestampField getCreatedAt() { return createdAt; }
    public GrammarCorrection getGrammarCorrection() { return grammarCorrection; }

    // ── Setters (cần cho manual build khi nhận socket) ────────────────────────

    public void setId(String id)                   { this.id = id; }
    public void setConversationId(String cid)      { this.conversationId = cid; }
    public void setSenderId(String sid)            { this.senderId = sid; }
    public void setType(String type)               { this.type = type; }
    public void setContent(String content)         { this.content = content; }
    public void setStatus(String status)           { this.status = status; }
    public void setCreatedAt(TimestampField ts)    { this.createdAt = ts; }

    // ── Nested: TimestampField ────────────────────────────────────────────────

    /**
     * Backend có thể trả về createdAt là:
     *   - Object: { "full": "19/05/2025 10:30", "friendly": "10:30" }  ← REST
     *   - String: "2025-05-19T10:30:00.000Z"                           ← Socket
     *
     * Gson không tự parse string → object nên ta dùng custom deserializer
     * đăng ký trong GsonBuilder. Xem GsonProvider.java bên dưới.
     */
    public static class TimestampField {
        @SerializedName("full")
        private String full;

        @SerializedName("friendly")
        private String friendly;

        public TimestampField() {}

        /** Constructor dùng khi nhận ISO string từ socket */
        public TimestampField(String isoOrFull) {
            this.full = isoOrFull;
            // Parse giờ:phút từ ISO string "2025-05-19T10:30:00.000Z"
            if (isoOrFull != null && isoOrFull.contains("T")) {
                this.friendly = isoOrFull.substring(11, 16); // "10:30"
            } else {
                this.friendly = isoOrFull;
            }
        }

        public String getFull()     { return full; }
        public String getFriendly() { return friendly != null ? friendly : ""; }
        public void setFull(String full)         { this.full = full; }
        public void setFriendly(String friendly) { this.friendly = friendly; }
    }

    // ── Nested: GrammarCorrection ─────────────────────────────────────────────

    public static class GrammarCorrection {
        @SerializedName("isCorrected")
        private boolean isCorrected;

        @SerializedName("correctedText")
        private String correctedText;

        @SerializedName("explanation")
        private String explanation;

        public boolean isCorrected()    { return isCorrected; }
        public String getCorrectedText(){ return correctedText; }
        public String getExplanation()  { return explanation; }
    }
}
