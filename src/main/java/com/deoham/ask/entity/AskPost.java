package com.deoham.ask.entity;

import com.deoham.global.entity.BaseEntity;
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
import org.locationtech.jts.geom.Point;

import java.util.UUID;

@Getter
@Entity
@Table(name = "ask_posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AskPost extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false, updatable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "category", nullable = false, columnDefinition = "ask_category")
    private AskCategory category;

    @Column(name = "title", length = 100)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "location", nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "preferred_gender", columnDefinition = "preferred_gender")
    private PreferredGender preferredGender;

    @Column(name = "preferred_age_min")
    private Integer preferredAgeMin;

    @Column(name = "preferred_age_max")
    private Integer preferredAgeMax;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "ask_status")
    private AskStatus status = AskStatus.OPEN;

    @Builder
    private AskPost(User author, AskCategory category, String title, String description, Point location,
                    PreferredGender preferredGender, Integer preferredAgeMin, Integer preferredAgeMax) {
        this.author = author;
        this.category = category;
        this.title = title;
        this.description = description;
        this.location = location;
        this.preferredGender = preferredGender;
        this.preferredAgeMin = preferredAgeMin;
        this.preferredAgeMax = preferredAgeMax;
        this.status = AskStatus.OPEN;
    }

    public void updateStatus(AskStatus status) {
        this.status = status;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
