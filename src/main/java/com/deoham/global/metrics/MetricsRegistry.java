package com.deoham.global.metrics;

import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MetricsRegistry {

  private final MeterRegistry meterRegistry;

  // ============ Counter (누적 값) ============

  public void incrementApiCallCount(String endpoint, String status) {
    Counter.builder("api.requests")
        .tag("endpoint", endpoint)
        .tag("status", status)
        .register(meterRegistry)
        .increment();
  }

  public void incrementApiErrorCount(String endpoint, String errorCode) {
    Counter.builder("api.error.total")
        .tag("endpoint", endpoint)
        .tag("error_code", errorCode)
        .register(meterRegistry)
        .increment();
  }

  public void incrementDbErrorCount(String operation) {
    Counter.builder("api.error.database")
        .tag("operation", operation)
        .register(meterRegistry)
        .increment();
  }

  public void incrementExternalApiErrorCount(String apiName) {
    Counter.builder("api.error.external_api")
        .tag("api_name", apiName)
        .register(meterRegistry)
        .increment();
  }

  // ============ Timer (응답시간 측정) ============

  public Timer.Sample recordApiDurationStart() {
    return Timer.start(meterRegistry);
  }

  public void recordApiDuration(Timer.Sample sample, String endpoint, String status) {
    sample.stop(
        Timer.builder("api.request.duration")
            .tag("endpoint", endpoint)
            .tag("status", status)
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
    );
  }

  public void recordCardsSearchDuration(Timer.Sample sample, String status) {
    stopStatusTimer(sample, "api.cards.search.duration", status);
  }

  public void recordCardsDetailDuration(Timer.Sample sample, String status) {
    stopStatusTimer(sample, "api.cards.detail.duration", status);
  }

  public void recordCardsCreateDuration(Timer.Sample sample, String status) {
    stopStatusTimer(sample, "api.cards.create.duration", status);
  }

  public void recordLocationSearchDuration(Timer.Sample sample, String searchType, String status) {
    sample.stop(
        Timer.builder("api.location.search.duration")
            .tag("search_type", searchType)
            .tag("status", status)
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
    );
  }

  public void recordUserLoginDuration(Timer.Sample sample, String status) {
    stopStatusTimer(sample, "api.users.login.duration", status);
  }

  public void recordUserSignupDuration(Timer.Sample sample, String status) {
    stopStatusTimer(sample, "api.users.signup.duration", status);
  }

  private void stopStatusTimer(Timer.Sample sample, String timerName, String status) {
    sample.stop(
        Timer.builder(timerName)
            .tag("status", status)
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
    );
  }

  // ============ Counter for Business Events ============

  public void incrementCardsSearchCount(String status) {
    incrementStatusCounter("api.cards.search.count", status);
  }

  public void incrementCardsDetailCount(String status) {
    incrementStatusCounter("api.cards.detail.count", status);
  }

  public void incrementCardsCreateCount(String status) {
    incrementStatusCounter("api.cards.create.count", status);
  }

  public void incrementLocationSearchCount(String searchType, String status) {
    Counter.builder("api.location.search.count")
        .tag("search_type", searchType)
        .tag("status", status)
        .register(meterRegistry)
        .increment();
  }

  public void incrementUserLoginCount(String status, String failReason) {
    Counter.builder("api.users.login.count")
        .tag("status", status)
        .tag("fail_reason", failReason)
        .register(meterRegistry)
        .increment();
  }

  public void incrementUserSignupCount(String status) {
    incrementStatusCounter("api.users.signup.count", status);
  }

  public void incrementChatMessageCount() {
    Counter.builder("api.chat.message.count")
        .register(meterRegistry)
        .increment();
  }

  public void incrementChatMessageErrorCount() {
    Counter.builder("api.chat.message.error")
        .register(meterRegistry)
        .increment();
  }

  private void incrementStatusCounter(String counterName, String status) {
    Counter.builder(counterName)
        .tag("status", status)
        .register(meterRegistry)
        .increment();
  }

  // ============ Helper Methods ============

  public String getHttpStatus(int statusCode) {
    return String.valueOf(statusCode);
  }

  public String getHttpStatusCategory(int statusCode) {
    if (statusCode >= 200 && statusCode < 300) {
      return "2xx";
    } else if (statusCode >= 300 && statusCode < 400) {
      return "3xx";
    } else if (statusCode >= 400 && statusCode < 500) {
      return "4xx";
    } else if (statusCode >= 500) {
      return "5xx";
    }
    return "unknown";
  }

  // ============ Phase 3.3: Service 레벨 메트릭 ============
  // 메트릭 이름은 "<prefix>.duration"(outcome 태그) + "<prefix>.count"(result 태그) 규칙을 따른다.
  // 아래 도메인별 메서드는 모두 공통 헬퍼(recordServiceSuccess/recordServiceFailure)에 위임한다.

  // Card Search
  public Timer.Sample startCardSearchTimer() {
    return Timer.start(meterRegistry);
  }

  public void recordCardSearchSuccess(Timer.Sample sample) {
    recordServiceSuccess(sample, "card.search");
  }

  public void recordCardSearchFailure(Timer.Sample sample) {
    recordServiceFailureDurationOnly(sample, "card.search");
  }

  // Card Detail
  public Timer.Sample startCardDetailTimer() {
    return Timer.start(meterRegistry);
  }

  public void recordCardDetailSuccess(Timer.Sample sample) {
    recordServiceSuccess(sample, "card.detail");
  }

  public void recordCardDetailFailure(Timer.Sample sample) {
    recordServiceFailureDurationOnly(sample, "card.detail");
  }

  // Card Create
  public Timer.Sample startCardCreateTimer() {
    return Timer.start(meterRegistry);
  }

  public void recordCardCreateSuccess(Timer.Sample sample) {
    recordServiceSuccess(sample, "card.create");
  }

  public void recordCardCreateFailure(Timer.Sample sample) {
    recordServiceFailureDurationOnly(sample, "card.create");
  }

  // User Profile
  public Timer.Sample startUserProfileTimer() {
    return Timer.start(meterRegistry);
  }

  public void recordUserProfileSuccess(Timer.Sample sample) {
    recordServiceSuccess(sample, "user.profile");
  }

  public void recordUserProfileFailure(Timer.Sample sample) {
    recordServiceFailureDurationOnly(sample, "user.profile");
  }

  // User Delete
  public Timer.Sample startUserDeleteTimer() {
    return Timer.start(meterRegistry);
  }

  public void recordUserDeleteSuccess(Timer.Sample sample) {
    recordServiceSuccess(sample, "user.delete");
  }

  public void recordUserDeleteFailure(Timer.Sample sample) {
    recordServiceFailureDurationOnly(sample, "user.delete");
  }

  // Chat Message Send
  public Timer.Sample startChatMessageSendTimer() {
    return Timer.start(meterRegistry);
  }

  public void recordChatMessageSendSuccess(Timer.Sample sample) {
    recordServiceSuccess(sample, "chat.message.send");
  }

  public void recordChatMessageSendFailure(Timer.Sample sample, int statusCode) {
    recordServiceFailureDurationOnly(sample, "chat.message.send");
  }

  // Chat Message Get
  public Timer.Sample startChatMessageGetTimer() {
    return Timer.start(meterRegistry);
  }

  public void recordChatMessageGetSuccess(Timer.Sample sample) {
    recordServiceSuccess(sample, "chat.message.get");
  }

  public void recordChatMessageGetFailure(Timer.Sample sample, int statusCode) {
    recordServiceFailureDurationOnly(sample, "chat.message.get");
  }

  // ============ Phase 3.4: 오류 카테고리 분류 ============

  public String extractErrorCategory(Throwable throwable) {
    if (throwable instanceof BusinessException businessException) {
      return mapErrorCodeToCategory(businessException.getErrorCode());
    }
    return "internal";
  }

  private String mapErrorCodeToCategory(ErrorCode errorCode) {
    return switch (errorCode) {
      case INVALID_REQUEST -> "validation";
      case UNAUTHORIZED -> "authentication";
      case FORBIDDEN -> "authorization";
      case NOT_FOUND -> "not_found";
      case CONFLICT -> "conflict";
      case INTERNAL_ERROR -> "internal";
    };
  }

  public void recordCardSearchFailure(Timer.Sample sample, Throwable throwable) {
    recordServiceFailure(sample, "card.search", throwable);
  }

  public void recordCardDetailFailure(Timer.Sample sample, Throwable throwable) {
    recordServiceFailure(sample, "card.detail", throwable);
  }

  public void recordCardCreateFailure(Timer.Sample sample, Throwable throwable) {
    recordServiceFailure(sample, "card.create", throwable);
  }

  // Location Search - search_type 태그가 추가로 붙는 특수 케이스
  public Timer.Sample startLocationSearchTimer() {
    return Timer.start(meterRegistry);
  }

  public void recordLocationSearchFailure(Timer.Sample sample, String searchType, Throwable throwable) {
    safeRecord(() -> {
      String category = extractErrorCategory(throwable);
      sample.stop(
          Timer.builder("location.search.duration")
              .tag("search_type", searchType)
              .tag("outcome", "failure")
              .publishPercentiles(0.5, 0.95, 0.99)
              .register(meterRegistry)
      );
      Counter.builder("location.search.count")
          .tag("search_type", searchType)
          .tag("result", "failure")
          .tag("error_category", category)
          .register(meterRegistry)
          .increment();
    });
  }

  public void recordUserProfileFailure(Timer.Sample sample, Throwable throwable) {
    recordServiceFailure(sample, "user.profile", throwable);
  }

  public void recordUserDeleteFailure(Timer.Sample sample, Throwable throwable) {
    recordServiceFailure(sample, "user.delete", throwable);
  }

  public Timer.Sample startUserLoginTimer() {
    return Timer.start(meterRegistry);
  }

  public void recordUserLoginFailure(Timer.Sample sample, Throwable throwable) {
    recordServiceFailure(sample, "user.login", throwable);
  }

  public Timer.Sample startUserSignupTimer() {
    return Timer.start(meterRegistry);
  }

  public void recordUserSignupFailure(Timer.Sample sample, Throwable throwable) {
    recordServiceFailure(sample, "user.signup", throwable);
  }

  public void recordChatMessageSendFailure(Timer.Sample sample, Throwable throwable) {
    recordServiceFailure(sample, "chat.message.send", throwable);
  }

  public void recordChatMessageGetFailure(Timer.Sample sample, Throwable throwable) {
    recordServiceFailure(sample, "chat.message.get", throwable);
  }

  // ============ 공통 기록 헬퍼 ============

  /** "<prefix>.duration"(outcome=success) 타이머 종료 + "<prefix>.count"(result=success) 카운터 증가. */
  private void recordServiceSuccess(Timer.Sample sample, String metricPrefix) {
    safeRecord(() -> {
      stopOutcomeTimer(sample, metricPrefix, "success");
      Counter.builder(metricPrefix + ".count")
          .tag("result", "success")
          .register(meterRegistry)
          .increment();
    });
  }

  /** "<prefix>.duration"(outcome=failure) 타이머만 종료. (Phase 3.3 시그니처 호환용) */
  private void recordServiceFailureDurationOnly(Timer.Sample sample, String metricPrefix) {
    safeRecord(() -> stopOutcomeTimer(sample, metricPrefix, "failure"));
  }

  /** 타이머 종료 + error_category 태그가 붙은 실패 카운터 증가. (Phase 3.4) */
  private void recordServiceFailure(Timer.Sample sample, String metricPrefix, Throwable throwable) {
    safeRecord(() -> {
      String category = extractErrorCategory(throwable);
      stopOutcomeTimer(sample, metricPrefix, "failure");
      Counter.builder(metricPrefix + ".count")
          .tag("result", "failure")
          .tag("error_category", category)
          .register(meterRegistry)
          .increment();
    });
  }

  private void stopOutcomeTimer(Timer.Sample sample, String metricPrefix, String outcome) {
    sample.stop(
        Timer.builder(metricPrefix + ".duration")
            .tag("outcome", outcome)
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
    );
  }

  // ============ Fail-open 메커니즘 ============

  private void safeRecord(Runnable recordingTask) {
    try {
      recordingTask.run();
    } catch (RuntimeException exception) {
      // Fail-open: 메트릭 기록 실패가 비즈니스 로직에 영향을 주지 않도록
    }
  }
}
