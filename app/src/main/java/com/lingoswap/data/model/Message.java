package com.lingoswap.data.model;

public class Message {
    public final String text;
    public final String time;
    public final boolean isMine;

    public Message(String text, String time, boolean isMine) {
        this.text = text;
        this.time = time;
        this.isMine = isMine;
    }
}
