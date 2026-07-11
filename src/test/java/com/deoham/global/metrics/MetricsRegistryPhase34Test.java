package com.deoham.global.metrics;

import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 3.4: 오류 카테고리 분류 메트릭 테스트
 *
 * 검증 범위:
 * - 오류 카테고리 추출 정확성
 * - 오류 카테고리별 메트릭 기록
 * - 카운터에 error_category 태그 포함
 * - 메트릭이 정확히 한 번만 기록됨
 */
@DisplayName("Phase 3.4: 오류 카테고리 분류 메트릭")
class MetricsRegistryPhase34Test {

  private MetricsRegistry metricsRegistry;
  private MeterRegistry meterRegistry;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    metricsRegistry = new MetricsRegistry(meterRegistry);
  }

  // ============ 오류 카테고리 추출 테스트 ============

  @Test
  @DisplayName("INVALID_REQUEST -> validation")
  void extractErrorCategory_InvalidRequest() {
    // When
    String category = metricsRegistry.extractErrorCategory(
        new BusinessException(ErrorCode.INVALID_REQUEST)
    );

    // Then
    assertThat(category).isEqualTo("validation");
  }

  @Test
  @DisplayName("UNAUTHORIZED -> authentication")
  void extractErrorCategory_Unauthorized() {
    // When
    String category = metricsRegistry.extractErrorCategory(
        new BusinessException(ErrorCode.UNAUTHORIZED)
    );

    // Then
    assertThat(category).isEqualTo("authentication");
  }

  @Test
  @DisplayName("FORBIDDEN -> authorization")
  void extractErrorCategory_Forbidden() {
    // When
    String category = metricsRegistry.extractErrorCategory(
        new BusinessException(ErrorCode.FORBIDDEN)
    );

    // Then
    assertThat(category).isEqualTo("authorization");
  }

  @Test
  @DisplayName("NOT_FOUND -> not_found")
  void extractErrorCategory_NotFound() {
    // When
    String category = metricsRegistry.extractErrorCategory(
        new BusinessException(ErrorCode.NOT_FOUND)
    );

    // Then
    assertThat(category).isEqualTo("not_found");
  }

  @Test
  @DisplayName("CONFLICT -> conflict")
  void extractErrorCategory_Conflict() {
    // When
    String category = metricsRegistry.extractErrorCategory(
        new BusinessException(ErrorCode.CONFLICT)
    );

    // Then
    assertThat(category).isEqualTo("conflict");
  }

  @Test
  @DisplayName("INTERNAL_ERROR -> internal")
  void extractErrorCategory_InternalError() {
    // When
    String category = metricsRegistry.extractErrorCategory(
        new BusinessException(ErrorCode.INTERNAL_ERROR)
    );

    // Then
    assertThat(category).isEqualTo("internal");
  }

  @Test
  @DisplayName("일반 예외 -> internal")
  void extractErrorCategory_GenericException() {
    // When
    String category = metricsRegistry.extractErrorCategory(
        new NullPointerException("Some error")
    );

    // Then
    assertThat(category).isEqualTo("internal");
  }

  // ============ 카드 검색 메트릭 - error_category 태그 ============

  @Test
  @DisplayName("카드 검색 실패(NOT_FOUND) 시 error_category=not_found 태그")
  void recordCardSearchFailure_WithNotFoundError() {
    // When
    Timer.Sample sample = metricsRegistry.startCardSearchTimer();
    metricsRegistry.recordCardSearchFailure(sample, new BusinessException(ErrorCode.NOT_FOUND));

    // Then
    assertThat(meterRegistry.counter("card.search.count", "result", "failure", "error_category", "not_found").count())
        .isEqualTo(1.0);
  }

  @Test
  @DisplayName("카드 검색 실패(INVALID_REQUEST) 시 error_category=validation 태그")
  void recordCardSearchFailure_WithValidationError() {
    // When
    Timer.Sample sample = metricsRegistry.startCardSearchTimer();
    metricsRegistry.recordCardSearchFailure(sample, new BusinessException(ErrorCode.INVALID_REQUEST));

    // Then
    assertThat(meterRegistry.counter("card.search.count", "result", "failure", "error_category", "validation").count())
        .isEqualTo(1.0);
  }

  @Test
  @DisplayName("카드 상세 조회 실패 시 error_category 태그 기록")
  void recordCardDetailFailure_WithErrorCategory() {
    // When
    Timer.Sample sample = metricsRegistry.startCardDetailTimer();
    metricsRegistry.recordCardDetailFailure(sample, new BusinessException(ErrorCode.NOT_FOUND));

    // Then
    assertThat(meterRegistry.counter("card.detail.count", "result", "failure", "error_category", "not_found").count())
        .isEqualTo(1.0);
  }

  @Test
  @DisplayName("카드 생성 실패 시 error_category 태그 기록")
  void recordCardCreateFailure_WithErrorCategory() {
    // When
    Timer.Sample sample = metricsRegistry.startCardCreateTimer();
    metricsRegistry.recordCardCreateFailure(sample, new BusinessException(ErrorCode.CONFLICT));

    // Then
    assertThat(meterRegistry.counter("card.create.count", "result", "failure", "error_category", "conflict").count())
        .isEqualTo(1.0);
  }

  // ============ 위치 검색 메트릭 - error_category 태그 ============

  @Test
  @DisplayName("위치 검색 실패 시 search_type과 error_category 태그 함께 기록")
  void recordLocationSearchFailure_WithSearchTypeAndErrorCategory() {
    // When
    Timer.Sample sample = metricsRegistry.startLocationSearchTimer();
    metricsRegistry.recordLocationSearchFailure(sample, "keyword", new BusinessException(ErrorCode.NOT_FOUND));

    // Then
    assertThat(meterRegistry.counter("location.search.count", "search_type", "keyword", "result", "failure", "error_category", "not_found").count())
        .isEqualTo(1.0);
  }

  // ============ 사용자 프로필 메트릭 - error_category 태그 ============

  @Test
  @DisplayName("사용자 프로필 조회 실패 시 error_category 태그 기록")
  void recordUserProfileFailure_WithErrorCategory() {
    // When
    Timer.Sample sample = metricsRegistry.startUserProfileTimer();
    metricsRegistry.recordUserProfileFailure(sample, new BusinessException(ErrorCode.NOT_FOUND));

    // Then
    assertThat(meterRegistry.counter("user.profile.count", "result", "failure", "error_category", "not_found").count())
        .isEqualTo(1.0);
  }

  // ============ 사용자 삭제 메트릭 - error_category 태그 ============

  @Test
  @DisplayName("사용자 삭제 실패 시 error_category 태그 기록")
  void recordUserDeleteFailure_WithErrorCategory() {
    // When
    Timer.Sample sample = metricsRegistry.startUserDeleteTimer();
    metricsRegistry.recordUserDeleteFailure(sample, new BusinessException(ErrorCode.FORBIDDEN));

    // Then
    assertThat(meterRegistry.counter("user.delete.count", "result", "failure", "error_category", "authorization").count())
        .isEqualTo(1.0);
  }

  // ============ 사용자 로그인 메트릭 - error_category 태그 ============

  @Test
  @DisplayName("사용자 로그인 실패 시 error_category=authentication 태그")
  void recordUserLoginFailure_WithErrorCategory() {
    // When
    Timer.Sample sample = metricsRegistry.startUserLoginTimer();
    metricsRegistry.recordUserLoginFailure(sample, new BusinessException(ErrorCode.UNAUTHORIZED));

    // Then
    assertThat(meterRegistry.counter("user.login.count", "result", "failure", "error_category", "authentication").count())
        .isEqualTo(1.0);
  }

  // ============ 사용자 회원가입 메트릭 - error_category 태그 ============

  @Test
  @DisplayName("사용자 회원가입 실패 시 error_category 태그 기록")
  void recordUserSignupFailure_WithErrorCategory() {
    // When
    Timer.Sample sample = metricsRegistry.startUserSignupTimer();
    metricsRegistry.recordUserSignupFailure(sample, new BusinessException(ErrorCode.CONFLICT));

    // Then
    assertThat(meterRegistry.counter("user.signup.count", "result", "failure", "error_category", "conflict").count())
        .isEqualTo(1.0);
  }

  // ============ 채팅 메시지 메트릭 - error_category 태그 ============

  @Test
  @DisplayName("채팅 메시지 전송 실패 시 error_category 태그 기록")
  void recordChatMessageSendFailure_WithErrorCategory() {
    // When
    Timer.Sample sample = metricsRegistry.startChatMessageSendTimer();
    metricsRegistry.recordChatMessageSendFailure(sample, new BusinessException(ErrorCode.FORBIDDEN));

    // Then
    assertThat(meterRegistry.counter("chat.message.send.count", "result", "failure", "error_category", "authorization").count())
        .isEqualTo(1.0);
  }

  @Test
  @DisplayName("채팅 메시지 조회 실패 시 error_category 태그 기록")
  void recordChatMessageGetFailure_WithErrorCategory() {
    // When
    Timer.Sample sample = metricsRegistry.startChatMessageGetTimer();
    metricsRegistry.recordChatMessageGetFailure(sample, new BusinessException(ErrorCode.NOT_FOUND));

    // Then
    assertThat(meterRegistry.counter("chat.message.get.count", "result", "failure", "error_category", "not_found").count())
        .isEqualTo(1.0);
  }

  // ============ 메트릭 정확성 검증 ============

  @Test
  @DisplayName("성공 메트릭과 실패 메트릭이 중복 기록되지 않음")
  void successAndFailureNotDoubly() {
    // When - record success
    Timer.Sample successSample = metricsRegistry.startCardSearchTimer();
    metricsRegistry.recordCardSearchSuccess(successSample);

    // When - record failure with category
    Timer.Sample failureSample = metricsRegistry.startCardSearchTimer();
    metricsRegistry.recordCardSearchFailure(failureSample, new BusinessException(ErrorCode.NOT_FOUND));

    // Then - success should have 1, failure should have 1
    assertThat(meterRegistry.counter("card.search.count", "result", "success").count()).isEqualTo(1.0);
    assertThat(meterRegistry.counter("card.search.count", "result", "failure", "error_category", "not_found").count()).isEqualTo(1.0);
  }

  @Test
  @DisplayName("다양한 error_category가 각각 정확히 기록됨")
  void multipleErrorCategories() {
    // When - record different error categories
    Timer.Sample sample1 = metricsRegistry.startCardSearchTimer();
    metricsRegistry.recordCardSearchFailure(sample1, new BusinessException(ErrorCode.NOT_FOUND));

    Timer.Sample sample2 = metricsRegistry.startCardSearchTimer();
    metricsRegistry.recordCardSearchFailure(sample2, new BusinessException(ErrorCode.INVALID_REQUEST));

    Timer.Sample sample3 = metricsRegistry.startCardSearchTimer();
    metricsRegistry.recordCardSearchFailure(sample3, new BusinessException(ErrorCode.CONFLICT));

    // Then - each category should have exactly 1 record
    assertThat(meterRegistry.counter("card.search.count", "result", "failure", "error_category", "not_found").count()).isEqualTo(1.0);
    assertThat(meterRegistry.counter("card.search.count", "result", "failure", "error_category", "validation").count()).isEqualTo(1.0);
    assertThat(meterRegistry.counter("card.search.count", "result", "failure", "error_category", "conflict").count()).isEqualTo(1.0);
  }

  // ============ Timer 정확성 검증 ============

  @Test
  @DisplayName("error_category가 있는 실패 메트릭도 Timer 기록")
  void timerRecordedWithErrorCategory() {
    // When
    Timer.Sample sample = metricsRegistry.startCardSearchTimer();
    metricsRegistry.recordCardSearchFailure(sample, new BusinessException(ErrorCode.NOT_FOUND));

    // Then - Timer should be recorded with outcome=failure
    assertThat(meterRegistry.timer("card.search.duration", "outcome", "failure").count()).isEqualTo(1);
  }
}
