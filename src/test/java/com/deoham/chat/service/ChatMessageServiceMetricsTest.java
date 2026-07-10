package com.deoham.chat.service;

import com.deoham.global.metrics.MetricsRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChatMessageService Metrics Tests")
class ChatMessageServiceMetricsTest {

  private MetricsRegistry metricsRegistry;
  private MeterRegistry meterRegistry;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    metricsRegistry = new MetricsRegistry(meterRegistry);
  }

  @Test
  @DisplayName("채팅 메시지 전송 성공 시 outcome=success 메트릭 기록")
  void recordChatMessageSendSuccess() {
    // When
    Timer.Sample sample = metricsRegistry.startChatMessageSendTimer();
    metricsRegistry.recordChatMessageSendSuccess(sample);

    // Then
    assertThat(meterRegistry.timer("chat.message.send.duration", "outcome", "success").count())
        .isEqualTo(1L);
  }

  @Test
  @DisplayName("채팅 메시지 전송 실패 시 outcome=failure 메트릭 기록")
  void recordChatMessageSendFailure() {
    // When
    Timer.Sample sample = metricsRegistry.startChatMessageSendTimer();
    metricsRegistry.recordChatMessageSendFailure(sample, 500);

    // Then
    assertThat(meterRegistry.timer("chat.message.send.duration", "outcome", "failure").count())
        .isEqualTo(1L);
  }

  @Test
  @DisplayName("채팅 메시지 조회 성공 시 outcome=success 메트릭 기록")
  void recordChatMessageGetSuccess() {
    // When
    Timer.Sample sample = metricsRegistry.startChatMessageGetTimer();
    metricsRegistry.recordChatMessageGetSuccess(sample);

    // Then
    assertThat(meterRegistry.timer("chat.message.get.duration", "outcome", "success").count())
        .isEqualTo(1L);
  }

  @Test
  @DisplayName("채팅 메시지 조회 실패 시 outcome=failure 메트릭 기록")
  void recordChatMessageGetFailure() {
    // When
    Timer.Sample sample = metricsRegistry.startChatMessageGetTimer();
    metricsRegistry.recordChatMessageGetFailure(sample, 500);

    // Then
    assertThat(meterRegistry.timer("chat.message.get.duration", "outcome", "failure").count())
        .isEqualTo(1L);
  }

  @Test
  @DisplayName("메트릭 기록 시 태그 정확성 검증")
  void metricsRecordingWithCorrectTags() {
    // Given - record both success and failure
    Timer.Sample successSample = metricsRegistry.startChatMessageSendTimer();
    metricsRegistry.recordChatMessageSendSuccess(successSample);

    Timer.Sample failureSample = metricsRegistry.startChatMessageSendTimer();
    metricsRegistry.recordChatMessageSendFailure(failureSample, 500);

    // Then - verify both are recorded with correct outcome tags
    assertThat(meterRegistry.timer("chat.message.send.duration", "outcome", "success").count())
        .isEqualTo(1L);
    assertThat(meterRegistry.timer("chat.message.send.duration", "outcome", "failure").count())
        .isEqualTo(1L);
  }

  @Test
  @DisplayName("동일 메트릭이 정확히 한 번만 기록됨")
  void metricsRecordedExactlyOnce() {
    // When
    Timer.Sample sample = metricsRegistry.startChatMessageGetTimer();
    metricsRegistry.recordChatMessageGetSuccess(sample);

    // Then - should have exactly 1 record
    assertThat(meterRegistry.timer("chat.message.get.duration", "outcome", "success").count())
        .isEqualTo(1L);

    // When - record another one
    Timer.Sample sample2 = metricsRegistry.startChatMessageGetTimer();
    metricsRegistry.recordChatMessageGetSuccess(sample2);

    // Then - should now have 2 records
    assertThat(meterRegistry.timer("chat.message.get.duration", "outcome", "success").count())
        .isEqualTo(2L);
  }
}
