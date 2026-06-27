package com.deoham.ask.entity;

import com.deoham.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "ask_applies")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AskApply {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ask_id", nullable = false, updatable = false)
    private AskPost ask;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id", nullable = false, updatable = false)
    private User applicant;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "apply_status")
    private ApplyStatus status = ApplyStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    private AskApply(AskPost ask, User applicant) {
        this.ask = ask;
        this.applicant = applicant;
        this.status = ApplyStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void accept() {
        this.status = ApplyStatus.ACCEPTED;
    }

    public void reject() {
        this.status = ApplyStatus.REJECTED;
    }
}
