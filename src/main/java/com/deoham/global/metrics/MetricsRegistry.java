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
    sample.stop(
        Timer.builder("api.cards.search.duration")
            .tag("status", status)
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
    );
  }

  public void recordCardsDetailDuration(Timer.Sample sample, String status) {
    sample.stop(
        Timer.builder("api.cards.detail.duration")
            .tag("status", status)
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
    );
  }

  public void recordCardsCreateDuration(Timer.Sample sample, String status) {
    sample.stop(
        Timer.builder("api.cards.create.duration")
            .tag("status", status)
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
    );
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
    sample.stop(
        Timer.builder("api.users.login.duration")
            .tag("status", status)
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
    );
  }

  public void recordUserSignupDuration(Timer.Sample sample, String status) {
    sample.stop(
        Timer.builder("api.users.signup.duration")
            .tag("status", status)
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
    );
  }

  // ============ Counter for Business Events ============

  public void incrementCardsSearchCount(String status) {
    Counter.builder("api.cards.search.count")
        .tag("status", status)
        .register(meterRegistry)
        .increment();
  }

  public void incrementCardsDetailCount(String status) {
    Counter.builder("api.cards.detail.count")
        .tag("status", status)
        .register(meterRegistry)
        .increment();
  }

  public void incrementCardsCreateCount(String status) {
    Counter.builder("api.cards.create.count")
        .tag("status", status)
        .register(meterRegistry)
        .increment();
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
    Counter.builder("api.users.signup.count")
        .tag("status", status)
        .register(meterRegistry)
        .increment();
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

  // Card Search
  public Timer.Sample startCardSearchTimer() {
    return Timer.start(meterRegistry);
  }

  public void recordCardSearchSuccess(Timer.Sample sample) {
    safeRecord(() -> {
      sample.stop(
          Timer.builder("card.search.duration")
              .tag("outcome", "success")
              .publishPercentiles(0.5, 0.95, 0.99)
              .register(meterRegistry)
      );
      Counter.builder("card.search.count")
          .tag("result", "success")
          .register(meterRegistry)
          .increment();
    });
  }

  public void recordCardSearchFailure(Timer.Sample sample) {
    safeRecord(() -> sample.stop(
        Timer.builder("card.search.duration")
            .tag("outcome", "failure")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
    ));
  }

  // Card Detail
  public Timer.Sample startCardDetailTimer() {
    return Timer.start(meterRegistry);
  }

  public void recordCardDetailSuccess(Timer.Sample sample) {
    safeRecord(() -> {
      sample.stop(
          Timer.builder("card.detail.duration")
              .tag("outcome", "success")
              .publishPercentiles(0.5, 0.95, 0.99)
              .register(meterRegistry)
      );
      Counter.builder("card.detail.count")
          .tag("result", "success")
          .register(meterRegistry)
          .increment();
    });
  }

  public void recordCardDetailFailure(Timer.Sample sample) {
    safeRecord(() -> sample.stop(
        Timer.builder("card.detail.duration")
            .tag("outcome", "failure")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
    ));
  }

  // Card Create
  public Timer.Sample startCardCreateTimer() {
    return Timer.start(meterRegistry);
  }

  public void recordCardCreateSuccess(Timer.Sample sample) {
    safeRecord(() -> {
      sample.stop(
          Timer.builder("card.create.duration")
              .tag("outcome", "success")
              .publishPercentiles(0.5, 0.95, 0.99)
              .register(meterRegistry)
      );
      Counter.builder("card.create.count")
          .tag("result", "success")
          .register(meterRegistry)
          .increment();
    });
  }

  public void recordCardCreateFailure(Timer.Sample sample) {
    safeRecord(() -> sample.stop(
        Timer.builder("card.create.duration")
            .tag("outcome", "failure")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
    ));
  }

  // User Profile
  public Timer.Sample startUserProfileTimer() {
    return Timer.start(meterRegistry);
  }

  public void recordUserProfileSuccess(Timer.Sample sample) {
    safeRecord(() -> {
      sample.stop(
          Timer.builder("user.profile.duration")
              .tag("outcome", "success")
              .publishPercentiles(0.5, 0.95, 0.99)
              .register(meterRegistry)
      );
      Counter.builder("user.profile.count")
          .tag("result", "success")
          .register(meterRegistry)
          .increment();
    });
  }

  public void recordUserProfileFailure(Timer.Sample sample) {
    safeRecord(() -> sample.stop(
        Timer.builder("user.profile.duration")
            .tag("outcome", "failure")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
    ));
  }

  // User Delete
  public Timer.Sample startUserDeleteTimer() {
    return Timer.start(meterRegistry);
  }

  public void recordUserDeleteSuccess(Timer.Sample sample) {
    safeRecord(() -> {
      sample.stop(
          Timer.builder("user.delete.duration")
              .tag("outcome", "success")
              .publishPercentiles(0.5, 0.95, 0.99)
              .register(meterRegistry)
      );
      Counter.builder("user.delete.count")
          .tag("result", "success")
          .register(meterRegistry)
          .increment();
    });
  }

  public void recordUserDeleteFailure(Timer.Sample sample) {
    safeRecord(() -> sample.stop(
        Timer.builder("user.delete.duration")
            .tag("outcome", "failure")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
    ));
  }

  // Chat Message Send
  public Timer.Sample startChatMessageSendTimer() {
    return Timer.start(meterRegistry);
  }

  public void recordChatMessageSendSuccess(Timer.Sample sample) {
    safeRecord(() -> {
      sample.stop(
          Timer.builder("chat.message.send.duration")
              .tag("outcome", "success")
              .publishPercentiles(0.5, 0.95, 0.99)
              .register(meterRegistry)
      );
      Counter.builder("chat.message.send.count")
          .tag("result", "success")
          .register(meterRegistry)
          .increment();
    });
  }

  public void recordChatMessageSendFailure(Timer.Sample sample, int statusCode) {
    safeRecord(() -> sample.stop(
        Timer.builder("chat.message.send.duration")
            .tag("outcome", "failure")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
    ));
  }

  // Chat Message Get
  public Timer.Sample startChatMessageGetTimer() {
    return Timer.start(meterRegistry);
  }

  public void recordChatMessageGetSuccess(Timer.Sample sample) {
    safeRecord(() -> {
      sample.stop(
          Timer.builder("chat.message.get.duration")
              .tag("outcome", "success")
              .publishPercentiles(0.5, 0.95, 0.99)
              .register(meterRegistry)
      );
      Counter.builder("chat.message.get.count")
          .tag("result", "success")
          .register(meterRegistry)
          .increment();
    });
  }

  public void recordChatMessageGetFailure(Timer.Sample sample, int statusCode) {
    safeRecord(() -> sample.stop(
        Timer.builder("chat.message.get.duration")
            .tag("outcome", "failure")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
    ));
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

  // Card Search - with error_category
  public void recordCardSearchFailure(Timer.Sample sample, Throwable throwable) {
    safeRecord(() -> {
      String category = extractErrorCategory(throwable);
      sample.stop(
          Timer.builder("card.search.duration")
              .tag("outcome", "failure")
              .publishPercentiles(0.5, 0.95, 0.99)
              .register(meterRegistry)
      );
      Counter.builder("card.search.count")
          .tag("result", "failure")
          .tag("error_category", category)
          .register(meterRegistry)
          .increment();
    });
  }

  // Card Detail - with error_category
  public void recordCardDetailFailure(Timer.Sample sample, Throwable throwable) {
    safeRecord(() -> {
      String category = extractErrorCategory(throwable);
      sample.stop(
          Timer.builder("card.detail.duration")
              .tag("outcome", "failure")
              .publishPercentiles(0.5, 0.95, 0.99)
              .register(meterRegistry)
      );
      Counter.builder("card.detail.count")
          .tag("result", "failure")
          .tag("error_category", category)
          .register(meterRegistry)
          .increment();
    });
  }

  // Card Create - with error_category
  public void recordCardCreateFailure(Timer.Sample sample, Throwable throwable) {
    safeRecord(() -> {
      String category = extractErrorCategory(throwable);
      sample.stop(
          Timer.builder("card.create.duration")
              .tag("outcome", "failure")
              .publishPercentiles(0.5, 0.95, 0.99)
              .register(meterRegistry)
      );
      Counter.builder("card.create.count")
          .tag("result", "failure")
          .tag("error_category", category)
          .register(meterRegistry)
          .increment();
    });
  }

  // Location Search - with error_category
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

  // User Profile - with error_category
  public void recordUserProfileFailure(Timer.Sample sample, Throwable throwable) {
    safeRecord(() -> {
      String category = extractErrorCategory(throwable);
      sample.stop(
          Timer.builder("user.profile.duration")
              .tag("outcome", "failure")
              .publishPercentiles(0.5, 0.95, 0.99)
              .register(meterRegistry)
      );
      Counter.builder("user.profile.count")
          .tag("result", "failure")
          .tag("error_category", category)
          .register(meterRegistry)
          .increment();
    });
  }

  // User Delete - with error_category
  public void recordUserDeleteFailure(Timer.Sample sample, Throwable throwable) {
    safeRecord(() -> {
      String category = extractErrorCategory(throwable);
      sample.stop(
          Timer.builder("user.delete.duration")
              .tag("outcome", "failure")
              .publishPercentiles(0.5, 0.95, 0.99)
              .register(meterRegistry)
      );
      Counter.builder("user.delete.count")
          .tag("result", "failure")
          .tag("error_category", category)
          .register(meterRegistry)
          .increment();
    });
  }

  // User Login - with error_category
  public Timer.Sample startUserLoginTimer() {
    return Timer.start(meterRegistry);
  }

  public void recordUserLoginFailure(Timer.Sample sample, Throwable throwable) {
    safeRecord(() -> {
      String category = extractErrorCategory(throwable);
      sample.stop(
          Timer.builder("user.login.duration")
              .tag("outcome", "failure")
              .publishPercentiles(0.5, 0.95, 0.99)
              .register(meterRegistry)
      );
      Counter.builder("user.login.count")
          .tag("result", "failure")
          .tag("error_category", category)
          .register(meterRegistry)
          .increment();
    });
  }

  // User Signup - with error_category
  public Timer.Sample startUserSignupTimer() {
    return Timer.start(meterRegistry);
  }

  public void recordUserSignupFailure(Timer.Sample sample, Throwable throwable) {
    safeRecord(() -> {
      String category = extractErrorCategory(throwable);
      sample.stop(
          Timer.builder("user.signup.duration")
              .tag("outcome", "failure")
              .publishPercentiles(0.5, 0.95, 0.99)
              .register(meterRegistry)
      );
      Counter.builder("user.signup.count")
          .tag("result", "failure")
          .tag("error_category", category)
          .register(meterRegistry)
          .increment();
    });
  }

  // Chat Message Send - with error_category
  public void recordChatMessageSendFailure(Timer.Sample sample, Throwable throwable) {
    safeRecord(() -> {
      String category = extractErrorCategory(throwable);
      sample.stop(
          Timer.builder("chat.message.send.duration")
              .tag("outcome", "failure")
              .publishPercentiles(0.5, 0.95, 0.99)
              .register(meterRegistry)
      );
      Counter.builder("chat.message.send.count")
          .tag("result", "failure")
          .tag("error_category", category)
          .register(meterRegistry)
          .increment();
    });
  }

  // Chat Message Get - with error_category
  public void recordChatMessageGetFailure(Timer.Sample sample, Throwable throwable) {
    safeRecord(() -> {
      String category = extractErrorCategory(throwable);
      sample.stop(
          Timer.builder("chat.message.get.duration")
              .tag("outcome", "failure")
              .publishPercentiles(0.5, 0.95, 0.99)
              .register(meterRegistry)
      );
      Counter.builder("chat.message.get.count")
          .tag("result", "failure")
          .tag("error_category", category)
          .register(meterRegistry)
          .increment();
    });
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
