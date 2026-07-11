package com.deoham.chat.repository;

import java.util.UUID;

public interface LastMessageProjection {

    UUID getRoomId();

    String getContent();
}
