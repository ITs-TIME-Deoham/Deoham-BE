package com.deoham.card.entity;

import com.deoham.global.entity.BaseEntity;
import com.deoham.project.entity.Contact;
import com.deoham.project.entity.Project;
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
@Table(name = "card")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 30)
    private CardCategory category;

    @Column(name = "original_message", columnDefinition = "TEXT")
    private String originalMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private CardSource sourceType;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Builder
    private Card(Project project, Contact contact, CardCategory category,
                 String originalMessage, CardSource sourceType, String memo, Instant occurredAt) {
        this.project = project;
        this.contact = contact;
        this.category = category;
        this.originalMessage = originalMessage;
        this.sourceType = sourceType == null ? CardSource.MANUAL : sourceType;
        this.memo = memo;
        this.occurredAt = occurredAt;
    }
}
