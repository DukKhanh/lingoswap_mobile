package com.lingoswap.utils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/** Trạng thái "có tin nhắn chưa đọc" dùng chung để hiện chấm đỏ trên tab Chat. */
public final class ChatUnreadStore {

    private static final MutableLiveData<Boolean> hasUnread = new MutableLiveData<>(false);

    private ChatUnreadStore() {}

    public static LiveData<Boolean> get() { return hasUnread; }

    public static void markUnread() { hasUnread.postValue(true); }

    public static void clear() { hasUnread.postValue(false); }
}
