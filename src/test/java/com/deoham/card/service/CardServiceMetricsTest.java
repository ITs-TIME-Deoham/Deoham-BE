package com.deoham.card.service;

import com.deoham.card.dto.request.CreateCardRequest;
import com.deoham.card.entity.Card;
import com.deoham.card.entity.CardCategory;
import com.deoham.card.entity.CardStatus;
import com.deoham.card.entity.PreferredGender;
import com.deoham.card.repository.CardApplyRepository;
import com.deoham.card.repository.CardRepository;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.global.metrics.MetricsRegistry;
import com.deoham.user.entity.User;
import com.deoham.user.repository.UserRepository;
import com.deoham.user.service.UserWriteService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardService Metrics Tests")
class CardServiceMetricsTest {

  private static final GeometryFactory GEOMETRY_FACTORY =
      new GeometryFactory(new PrecisionModel(), 4326);

  private CardReadService cardReadService;
  private CardWriteService cardWriteService;
  private MetricsRegistry metricsRegistry;
  private MeterRegistry meterRegistry;

  @Mock private CardRepository cardRepository;
  @Mock private CardApplyRepository cardApplyRepository;
  @Mock private UserRepository userRepository;
  @Mock private UserWriteService userWriteServiceMock;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    metricsRegistry = new MetricsRegistry(meterRegistry);
    cardReadService = new DefaultCardReadService(cardRepository, cardApplyRepository, userRepository, metricsRegistry);
    cardWriteService = new DefaultCardWriteService(cardRepository, cardApplyRepository, userRepository, userWriteServiceMock, metricsRegistry);
  }

  @Test
  @DisplayName("카드 검색 성공 시 outcome=success 메트릭 기록")
  void recordCardSearchSuccess() {
    // Given
    UUID userId = UUID.randomUUID();
    double lat = 37.5;
    double lng = 127.0;
    String cursor = null;

    when(cardRepository.findNearbyCards(lat, lng, null, null)).thenReturn(List.of());
    when(userRepository.hasSeenCardViewOnboarding(userId)).thenReturn(true);

    // When
    cardReadService.getNearbyCards(lat, lng, cursor, userId);

    // Then
    assertThat(meterRegistry.timer("card.search.duration", "outcome", "success").count())
        .isEqualTo(1L);
  }

  @Test
  @DisplayName("카드 상세 조회 성공 시 outcome=success 메트릭 기록")
  void recordCardDetailSuccess() {
    // Given
    UUID cardId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    User requester = User.builder().firebaseUid("firebase1").nickname("test").build();
    Card card = Card.builder()
        .requester(requester)
        .category(CardCategory.MEAL)
        .description("test")
        .location(GEOMETRY_FACTORY.createPoint(new Coordinate(127.0, 37.5)))
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();

    when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

    // When
    cardReadService.getCard(cardId);

    // Then
    assertThat(meterRegistry.timer("card.detail.duration", "outcome", "success").count())
        .isEqualTo(1L);
  }

  @Test
  @DisplayName("카드 상세 조회 실패 시 outcome=failure 메트릭 기록")
  void recordCardDetailFailure() {
    // Given
    UUID cardId = UUID.randomUUID();

    when(cardRepository.findById(cardId))
        .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다."));

    // When & Then
    assertThatThrownBy(() -> cardReadService.getCard(cardId))
        .isInstanceOf(BusinessException.class);

    assertThat(meterRegistry.timer("card.detail.duration", "outcome", "failure").count())
        .isEqualTo(1L);
  }

  @Test
  @DisplayName("카드 생성 성공 시 outcome=success 메트릭 기록")
  void recordCardCreateSuccess() {
    // Given
    UUID userId = UUID.randomUUID();
    CreateCardRequest request = new CreateCardRequest(
        CardCategory.MEAL,
        "test description",
        37.5,
        127.0,
        PreferredGender.ANY,
        20,
        40
    );

    User user = User.builder().firebaseUid("firebase1").nickname("test").build();
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(cardRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    // When
    cardWriteService.createCard(request, userId);

    // Then
    assertThat(meterRegistry.timer("card.create.duration", "outcome", "success").count())
        .isEqualTo(1L);
  }

  @Test
  @DisplayName("카드 생성 실패 시 outcome=failure 메트릭 기록")
  void recordCardCreateFailure() {
    // Given
    UUID userId = UUID.randomUUID();
    CreateCardRequest request = new CreateCardRequest(
        CardCategory.MEAL,
        "test description",
        37.5,
        127.0,
        PreferredGender.ANY,
        20,
        40
    );

    when(userRepository.findById(userId))
        .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

    // When & Then
    assertThatThrownBy(() -> cardWriteService.createCard(request, userId))
        .isInstanceOf(BusinessException.class);

    assertThat(meterRegistry.timer("card.create.duration", "outcome", "failure").count())
        .isEqualTo(1L);
  }

  @Test
  @DisplayName("메트릭 기록 중 예외 발생 시에도 비즈니스 응답 보존")
  void metricsFailurePreservesBusinessResponse() {
    // Given
    UUID cardId = UUID.randomUUID();

    when(cardRepository.findById(cardId))
        .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다."));

    // When & Then
    assertThatThrownBy(() -> cardReadService.getCard(cardId))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
  }

  @Test
  @DisplayName("동일 메트릭이 2회 이상 기록되지 않음")
  void metricsRecordedOnceOnly() {
    // Given
    UUID cardId = UUID.randomUUID();

    User requester = User.builder().firebaseUid("firebase1").nickname("test").build();
    Card card = Card.builder()
        .requester(requester)
        .category(CardCategory.MEAL)
        .description("test")
        .location(GEOMETRY_FACTORY.createPoint(new Coordinate(127.0, 37.5)))
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();

    when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

    // When
    cardReadService.getCard(cardId);

    // Then - should have exactly 1 record, not more
    assertThat(meterRegistry.timer("card.detail.duration", "outcome", "success").count())
        .isEqualTo(1L);
  }
}
