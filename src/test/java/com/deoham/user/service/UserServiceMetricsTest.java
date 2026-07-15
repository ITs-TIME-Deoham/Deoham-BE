package com.deoham.user.service;

import com.deoham.global.config.S3Properties;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.global.metrics.MetricsRegistry;
import com.deoham.user.entity.User;
import com.deoham.user.repository.UserRepository;
import com.deoham.user.repository.UserSocialAccountRepository;
import com.deoham.user.service.impl.UserReadServiceImpl;
import com.deoham.user.service.impl.UserWriteServiceImpl;
import com.deoham.card.repository.CardApplyRepository;
import com.deoham.card.repository.CardRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Metrics Tests")
class UserServiceMetricsTest {

  private UserReadService userReadService;
  private UserWriteService userWriteService;
  private MetricsRegistry metricsRegistry;
  private MeterRegistry meterRegistry;

  @Mock private UserRepository userRepository;
  @Mock private UserSocialAccountRepository userSocialAccountRepository;
  @Mock private CardRepository cardRepository;
  @Mock private CardApplyRepository cardApplyRepository;
  @Mock private S3Client s3Client;
  @Mock private S3Properties s3Properties;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    metricsRegistry = new MetricsRegistry(meterRegistry);
    userReadService = new UserReadServiceImpl(userRepository, metricsRegistry);
    userWriteService =
        new UserWriteServiceImpl(
            userRepository,
            cardRepository,
            cardApplyRepository,
            userSocialAccountRepository,
            metricsRegistry,
            s3Client,
            s3Properties);
  }

  @Test
  @DisplayName("사용자 프로필 조회 성공 시 outcome=success 메트릭 기록")
  void recordUserProfileSuccess() {
    // Given
    UUID userId = UUID.randomUUID();
    User user = User.builder()
        .firebaseUid("firebase1")
        .nickname("testUser")
        .build();

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    // When
    userReadService.getProfile(userId);

    // Then
    assertThat(meterRegistry.timer("user.profile.duration", "outcome", "success").count())
        .isEqualTo(1L);
  }

  @Test
  @DisplayName("사용자 프로필 조회 실패 시 outcome=failure 메트릭 기록")
  void recordUserProfileFailure() {
    // Given
    UUID userId = UUID.randomUUID();

    when(userRepository.findById(userId))
        .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

    // When & Then
    assertThatThrownBy(() -> userReadService.getProfile(userId))
        .isInstanceOf(BusinessException.class);

    assertThat(meterRegistry.timer("user.profile.duration", "outcome", "failure").count())
        .isEqualTo(1L);
  }

  @Test
  @DisplayName("사용자 삭제 성공 시 outcome=success 메트릭 기록")
  void recordUserDeleteSuccess() {
    // Given
    UUID userId = UUID.randomUUID();
    User user = User.builder()
        .firebaseUid("firebase1")
        .nickname("testUser")
        .build();

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.save(user)).thenReturn(user);

    // When
    userWriteService.deleteUser(userId);

    // Then
    assertThat(meterRegistry.timer("user.delete.duration", "outcome", "success").count())
        .isEqualTo(1L);
  }

  @Test
  @DisplayName("사용자 삭제 실패 시 outcome=failure 메트릭 기록")
  void recordUserDeleteFailure() {
    // Given
    UUID userId = UUID.randomUUID();

    when(userRepository.findById(userId))
        .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

    // When & Then
    assertThatThrownBy(() -> userWriteService.deleteUser(userId))
        .isInstanceOf(BusinessException.class);

    assertThat(meterRegistry.timer("user.delete.duration", "outcome", "failure").count())
        .isEqualTo(1L);
  }

  @Test
  @DisplayName("메트릭 기록 중 예외 발생 시에도 비즈니스 응답 보존")
  void metricsFailurePreservesBusinessResponse() {
    // Given
    UUID userId = UUID.randomUUID();

    when(userRepository.findById(userId))
        .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

    // When & Then
    assertThatThrownBy(() -> userReadService.getProfile(userId))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
  }

  @Test
  @DisplayName("동일 메트릭이 2회 이상 기록되지 않음")
  void metricsRecordedOnceOnly() {
    // Given
    UUID userId = UUID.randomUUID();
    User user = User.builder()
        .firebaseUid("firebase1")
        .nickname("testUser")
        .build();

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    // When
    userReadService.getProfile(userId);

    // Then - should have exactly 1 record, not more
    assertThat(meterRegistry.timer("user.profile.duration", "outcome", "success").count())
        .isEqualTo(1L);
  }
}
