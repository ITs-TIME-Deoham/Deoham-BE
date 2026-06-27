package com.deoham.report.entity;

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
public class UserBlockId implements Serializable {

    @Column(name = "blocker_id")
    private UUID blockerId;

    @Column(name = "blocked_id")
    private UUID blockedId;
}
