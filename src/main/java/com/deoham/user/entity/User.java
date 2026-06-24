package com.deoham.user.entity;

import com.deoham.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Builder
    private User(UUID supabaseId, String nickname, String profileImageUrl) {
        this.supabaseId = supabaseId;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

    public void updateProfile(String nickname, String profileImageUrl) {
        if (nickname != null) this.nickname = nickname;
        if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
    }

    public void updateLanguage(String language) {
        this.language = language;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
