package com.deoham.global.metrics;

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

  // ============ Gauge (순간값) ============

  public void registerActiveWebSocketConnections(io.micrometer.core.instrument.Gauge.builder<? extends Number> gaugeBuilder) {
    gaugeBuilder
        .register(meterRegistry);
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
}
