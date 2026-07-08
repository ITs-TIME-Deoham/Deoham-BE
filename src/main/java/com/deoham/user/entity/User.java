package com.deoham.user.entity;

import com.deoham.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "users")
@Where(clause = "deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "firebase_uid", nullable = false, unique = true, updatable = false, length = 128)
    private String firebaseUid;

    @Column(name = "nickname", nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified = false;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    @Column(name = "language", nullable = false, length = 10)
    private String language = "ko";

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "gender", columnDefinition = "user_gender")
    private GenderType gender;

    @Column(name = "age")
    private Integer age;

    @Column(name = "help_count", nullable = false)
    private int helpCount = 0;

    @Column(name = "help_request_count", nullable = false)
    private int helpRequestCount = 0;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "role", nullable = false, columnDefinition = "user_role")
    private UserRole role = UserRole.USER;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "user_status")
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "has_created_card", nullable = false)
    private boolean hasCreatedCard = false;

    @Builder
    private User(String firebaseUid, String nickname, String profileImageUrl, GenderType gender, Integer age) {
        this.firebaseUid = firebaseUid != null ? firebaseUid : UUID.randomUUID().toString();
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.gender = gender;
        this.age = age;
        this.role = UserRole.USER;
        this.status = UserStatus.ACTIVE;
    }

    public void updateProfile(String nickname, String profileImageUrl) {
        if (nickname != null) this.nickname = nickname;
        if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
    }

    public void incrementHelpCount() {
        this.helpCount++;
    }

    public void incrementHelpRequestCount() {
        this.helpRequestCount++;
    }

    public void setHelpCount(int helpCount) {
        this.helpCount = helpCount;
    }

    public void setHelpRequestCount(int helpRequestCount) {
        this.helpRequestCount = helpRequestCount;
    }

    public void updateLanguage(String language) {
        this.language = language;
    }

    public void updateAge(Integer age) {
        this.age = age;
    }

    public void verify() {
        this.isVerified = true;
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    public void delete() {
        this.status = UserStatus.DELETED;
        this.deletedAt = Instant.now();
    }

    public void markCardCreated() {
        this.hasCreatedCard = true;
    }
}
