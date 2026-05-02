package com.deoham.card.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.EntityListeners;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "ai_analysis")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AiAnalysis {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Column(name = "classification", length = 100)
    private String classification;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "confidence_score")
    private Float confidenceScore;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @Column(name = "is_latest", nullable = false)
    private boolean isLatest;

    @CreatedDate
    @Column(name = "analyzed_at", nullable = false, updatable = false)
    private Instant analyzedAt;

    @Builder
    private AiAnalysis(Card card, String classification, String summary,
                       Float confidenceScore, String modelVersion, Boolean isLatest) {
        this.card = card;
        this.classification = classification;
        this.summary = summary;
        this.confidenceScore = confidenceScore;
        this.modelVersion = modelVersion;
        this.isLatest = isLatest == null ? true : isLatest;
    }

    public void markAsStale() {
        this.isLatest = false;
    }
}
