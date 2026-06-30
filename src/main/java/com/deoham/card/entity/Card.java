package com.deoham.card.entity;

import com.deoham.global.entity.BaseEntity;
import com.deoham.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

@Getter
@Entity
@Table(name = "cards")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false, updatable = false)
    private User requester;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "category", nullable = false, columnDefinition = "card_category")
    private CardCategory category;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "location", nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "radius_m")
    private Integer radiusM;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "preferred_gender", columnDefinition = "preferred_gender")
    private PreferredGender preferredGender;

    @Column(name = "preferred_age_min")
    private Integer preferredAgeMin;

    @Column(name = "preferred_age_max")
    private Integer preferredAgeMax;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "card_status")
    private CardStatus status = CardStatus.OPEN;

    @Builder
    private Card(User requester, CardCategory category, String title, String description,
                 Point location, String city, Integer radiusM, Instant expiresAt,
                 PreferredGender preferredGender, Integer preferredAgeMin, Integer preferredAgeMax) {
        this.requester = requester;
        this.category = category;
        this.title = title;
        this.description = description;
        this.location = location;
        this.city = city;
        this.radiusM = radiusM;
        this.expiresAt = expiresAt;
        this.preferredGender = preferredGender;
        this.preferredAgeMin = preferredAgeMin;
        this.preferredAgeMax = preferredAgeMax;
        this.status = CardStatus.OPEN;
    }

    public void updateStatus(CardStatus status) {
        this.status = status;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
