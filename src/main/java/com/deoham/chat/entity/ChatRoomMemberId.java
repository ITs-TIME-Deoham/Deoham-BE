package com.deoham.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomMemberId implements Serializable {

    @Column(name = "room_id")
    private UUID roomId;

    @Column(name = "user_id")
    private UUID userId;
}
