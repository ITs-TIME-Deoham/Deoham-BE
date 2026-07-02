package com.deoham.chat.repository;

import java.util.UUID;

public interface UnreadCountProjection {

    UUID getRoomId();

    Long getUnreadCount();
}
