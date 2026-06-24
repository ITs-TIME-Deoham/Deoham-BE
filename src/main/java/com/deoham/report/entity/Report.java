package com.deoham.report.entity;

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
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false, updatable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, columnDefinition = "report_target")
    private ReportTarget targetType;

    @Column(name = "target_id", nullable = false, updatable = false)
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, columnDefinition = "report_reason")
    private ReportReason reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    private Report(User reporter, ReportTarget targetType, UUID targetId, ReportReason reason) {
        this.reporter = reporter;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.createdAt = Instant.now();
    }
}
