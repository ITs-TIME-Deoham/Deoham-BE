package com.deoham.user.entity;

import com.deoham.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "job_type", length = 100)
    private JobType jobType;

    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, length = 20)
    @Builder.Default
    private PlanType planType = PlanType.FREE;

    @Column(name = "noti_new_card", nullable = false)
    private boolean notiNewCard;

    @Column(name = "noti_link_viewed", nullable = false)
    private boolean notiLinkViewed;

    @Column(name = "noti_counterpart_confirmed", nullable = false)
    private boolean notiCounterpartConfirmed;

    @Builder
    private User(String email, String name, String jobType, String phone, PlanType planType) {
        this.email = email;
        this.name = name;
        this.jobType = jobType;
        this.phone = phone;
        this.planType = planType == null ? PlanType.FREE : planType;
        this.notiNewCard = true;
        this.notiLinkViewed = true;
        this.notiCounterpartConfirmed = true;
    }
}
