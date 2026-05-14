package com.deoham.project.entity;

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
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(name = "project")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "collab_type", nullable = false, length = 20)
    private CollabType collabType;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "contract_amount", precision = 15, scale = 2)
    private BigDecimal contractAmount;

    @Column(name = "contract_start")
    private LocalDate contractStart;

    @Column(name = "contract_end")
    private LocalDate contractEnd;

    @Column(name = "file_url", columnDefinition = "TEXT")
    private String fileUrl;

    @Builder
    private Project(User user, CollabType collabType, String name,
                    BigDecimal contractAmount, LocalDate contractStart, LocalDate contractEnd,
                    String fileUrl) {
        this.user = user;
        this.collabType = collabType;
        this.name = name;
        this.contractAmount = contractAmount;
        this.contractStart = contractStart;
        this.contractEnd = contractEnd;
        this.fileUrl = fileUrl;
    }
}
