package com.deoham.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.deoham.card.repository.CardApplyRepository;
import com.deoham.card.repository.CardRepository;
import com.deoham.global.config.S3Properties;
import com.deoham.global.metrics.MetricsRegistry;
import com.deoham.notification.repository.FcmTokenRepository;
import com.deoham.notification.repository.NotificationRepository;
import com.deoham.user.entity.User;
import com.deoham.user.event.ProfileImageDeletedEvent;
import com.deoham.user.event.ProfileImageDeletionListener;
import com.deoham.user.repository.UserRepository;
import com.deoham.user.repository.UserSocialAccountRepository;
import com.deoham.user.service.impl.UserWriteServiceImpl;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

/**
 * 프로필 이미지 삭제가 S3를 직접 호출하지 않고 이벤트로 위임되는지 검증한다.
 * 이전 구현은 private 메서드에 붙은 {@code @Async}가 프록시를 타지 않아 트랜잭션 안에서 동기로 S3를 호출했다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("프로필 이미지 삭제 이벤트 테스트")
class ProfileImageDeletionEventTest {

    private static final String BUCKET = "test-bucket";
    private static final String REGION = "ap-northeast-2";
    private static final String KEY = "profiles/abc/image.png";
    private static final String IMAGE_URL =
            "https://" + BUCKET + ".s3." + REGION + ".amazonaws.com/" + KEY;

    @Mock private UserRepository userRepository;
    @Mock private CardRepository cardRepository;
    @Mock private CardApplyRepository cardApplyRepository;
    @Mock private UserSocialAccountRepository userSocialAccountRepository;
    @Mock private FcmTokenRepository fcmTokenRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private S3Client s3Client;
    @Mock private S3Properties s3Properties;
    @Mock private ApplicationEventPublisher eventPublisher;

    private UserWriteServiceImpl userWriteService;
    private ProfileImageDeletionListener listener;

    @BeforeEach
    void setUp() {
        when(s3Properties.bucket()).thenReturn(BUCKET);
        when(s3Properties.region()).thenReturn(REGION);
        userWriteService = new UserWriteServiceImpl(
                userRepository,
                cardRepository,
                cardApplyRepository,
                userSocialAccountRepository,
                fcmTokenRepository,
                notificationRepository,
                new MetricsRegistry(new SimpleMeterRegistry()),
                s3Client,
                s3Properties,
                eventPublisher);
        listener = new ProfileImageDeletionListener(s3Client, s3Properties);
    }

    @Test
    @DisplayName("프로필 이미지 삭제 시 S3를 직접 호출하지 않고 이벤트만 발행한다")
    void deleteProfileImage_publishesEventWithoutTouchingS3() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = User.builder().firebaseUid("uid").nickname("tester").build();
        user.updateProfile(null, IMAGE_URL);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        userWriteService.deleteProfileImage(userId);

        // Then — 트랜잭션 안에서는 S3를 건드리지 않는다 (롤백 시 파일 유실 방지)
        verifyNoInteractions(s3Client);
        ArgumentCaptor<ProfileImageDeletedEvent> captor =
                ArgumentCaptor.forClass(ProfileImageDeletedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().imageUrl()).isEqualTo(IMAGE_URL);
        assertThat(user.getProfileImageUrl()).isNull();
    }

    @Test
    @DisplayName("이미지가 없으면 이벤트를 발행하지 않는다")
    void deleteProfileImage_noImage_publishesNothing() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = User.builder().firebaseUid("uid").nickname("tester").build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        userWriteService.deleteProfileImage(userId);

        // Then
        verify(eventPublisher, never()).publishEvent(any(ProfileImageDeletedEvent.class));
    }

    @Test
    @DisplayName("리스너가 URL에서 키를 추출해 S3 객체를 삭제한다")
    void listener_deletesS3Object() {
        // When
        listener.handle(new ProfileImageDeletedEvent(IMAGE_URL));

        // Then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<DeleteObjectRequest.Builder>> captor =
                ArgumentCaptor.forClass(Consumer.class);
        verify(s3Client).deleteObject(captor.capture());

        DeleteObjectRequest.Builder builder = DeleteObjectRequest.builder();
        captor.getValue().accept(builder);
        DeleteObjectRequest request = builder.build();
        assertThat(request.bucket()).isEqualTo(BUCKET);
        assertThat(request.key()).isEqualTo(KEY);
    }

    @Test
    @DisplayName("버킷 URL 형식이 아니면 삭제를 시도하지 않는다")
    void listener_skipsUnknownUrl() {
        // When
        listener.handle(new ProfileImageDeletedEvent("https://other-host.example.com/x.png"));

        // Then
        verifyNoInteractions(s3Client);
    }

    @Test
    @DisplayName("S3 삭제가 실패해도 예외를 전파하지 않는다")
    void listener_swallowsFailure() {
        // Given
        when(s3Client.deleteObject(any(Consumer.class)))
                .thenThrow(new RuntimeException("S3 down"));

        // When & Then — 부가 작업이므로 본 흐름에 영향을 주지 않는다
        listener.handle(new ProfileImageDeletedEvent(IMAGE_URL));
    }
}
