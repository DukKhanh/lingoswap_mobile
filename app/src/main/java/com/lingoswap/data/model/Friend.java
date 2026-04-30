package com.lingoswap.data.model;

public class Friend {
    public final String name;
    public final String langs;
    public final boolean isOnline;
    public final boolean isPending;

    public Friend(String name, String langs, boolean isOnline, boolean isPending) {
        this.name = name;
        this.langs = langs;
        this.isOnline = isOnline;
        this.isPending = isPending;
    }
}
