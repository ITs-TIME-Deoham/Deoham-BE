package com.deoham.notice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class NoticeReadId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "notice_id")
    private UUID noticeId;
}
