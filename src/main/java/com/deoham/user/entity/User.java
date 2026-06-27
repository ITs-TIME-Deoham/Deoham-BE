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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

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

    @Column(name = "supabase_id", nullable = false, unique = true, updatable = false)
    private UUID supabaseId;

    @Column(name = "nickname", nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(name = "profile_image_url")
    private String profileImageUrl;


    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified = false;


    @Column(name = "language", nullable = false, length = 10)
    private String language = "ko";

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "gender", columnDefinition = "user_gender")
    private GenderType gender;

    @Column(name = "age")
    private Integer age;

    @Column(name = "help_count", nullable = false)
    private int helpCount = 0;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Builder
    private User(UUID supabaseId, String nickname, String profileImageUrl, GenderType gender, Integer age) {
        this.supabaseId = supabaseId;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.gender = gender;
        this.age = age;
    }

    public void updateProfile(String nickname, String profileImageUrl) {
        if (nickname != null) this.nickname = nickname;
        if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
    }

    public void incrementHelpCount() {
        this.helpCount++;
    }

    public void updateLanguage(String language) {
        this.language = language;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
