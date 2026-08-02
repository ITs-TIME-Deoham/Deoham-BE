package com.deoham.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deoham.notification.dto.FcmTokenRegisterRequest;
import com.deoham.notification.entity.FcmToken;
import com.deoham.notification.entity.Platform;
import com.deoham.notification.repository.FcmTokenRepository;
import com.deoham.notification.service.impl.FcmTokenServiceImpl;
import com.deoham.user.entity.User;
import com.deoham.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmTokenService upsert 테스트")
class FcmTokenServiceTest {

    @Mock private FcmTokenRepository fcmTokenRepository;
    @Mock private UserRepository userRepository;

    private FcmTokenServiceImpl fcmTokenService;

    @BeforeEach
    void setUp() {
        fcmTokenService = new FcmTokenServiceImpl(fcmTokenRepository, userRepository);
    }

    @Test
    @DisplayName("신규 토큰이면 새로 저장한다")
    void register_newToken_inserts() {
        // Given
        UUID userId = UUID.randomUUID();
        FcmTokenRegisterRequest request = new FcmTokenRegisterRequest("token-new", "device-1", Platform.ANDROID);
        when(fcmTokenRepository.findByToken("token-new")).thenReturn(Optional.empty());
        when(fcmTokenRepository.findByUser_IdAndDeviceId(userId, "device-1")).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(mock(User.class)));

        // When
        fcmTokenService.register(userId, request);

        // Then
        verify(fcmTokenRepository).save(any(FcmToken.class));
    }

    @Test
    @DisplayName("같은 기기의 기존 토큰이 있으면 토큰 값만 갱신한다(rotation)")
    void register_sameDevice_updatesToken() {
        // Given
        UUID userId = UUID.randomUUID();
        FcmToken existing = FcmToken.builder()
                .user(mock(User.class))
                .token("token-old")
                .deviceId("device-1")
                .platform(Platform.ANDROID)
                .build();
        FcmTokenRegisterRequest request = new FcmTokenRegisterRequest("token-new", "device-1", Platform.IOS);
        when(fcmTokenRepository.findByToken("token-new")).thenReturn(Optional.empty());
        when(fcmTokenRepository.findByUser_IdAndDeviceId(userId, "device-1")).thenReturn(Optional.of(existing));

        // When
        fcmTokenService.register(userId, request);

        // Then
        assertThat(existing.getToken()).isEqualTo("token-new");
        assertThat(existing.getPlatform()).isEqualTo(Platform.IOS);
        verify(fcmTokenRepository, never()).save(any(FcmToken.class));
    }

    @Test
    @DisplayName("같은 토큰이 다른 유저 소유면 소유자를 재할당한다(계정 전환)")
    void register_tokenOwnedByOther_reassigns() {
        // Given
        UUID newUserId = UUID.randomUUID();
        User previousOwner = mock(User.class);
        User newOwner = mock(User.class);
        FcmToken existing = FcmToken.builder()
                .user(previousOwner)
                .token("token-shared")
                .deviceId("device-1")
                .platform(Platform.ANDROID)
                .build();
        FcmTokenRegisterRequest request = new FcmTokenRegisterRequest("token-shared", "device-1", Platform.ANDROID);
        when(fcmTokenRepository.findByToken("token-shared")).thenReturn(Optional.of(existing));
        when(fcmTokenRepository.findByUser_IdAndDeviceId(newUserId, "device-1")).thenReturn(Optional.empty());
        when(userRepository.findById(newUserId)).thenReturn(Optional.of(newOwner));

        // When
        fcmTokenService.register(newUserId, request);

        // Then
        assertThat(existing.getUser()).isSameAs(newOwner);
        assertThat(existing.getToken()).isEqualTo("token-shared");
        verify(fcmTokenRepository, never()).save(any(FcmToken.class));
    }
}
