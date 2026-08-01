package com.deoham.chat.translation;

import static org.assertj.core.api.Assertions.assertThat;

import com.deoham.chat.translation.SimpleCircuitBreaker.State;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SimpleCircuitBreakerTest {

    private static final Duration COOLDOWN = Duration.ofSeconds(30);

    @Test
    void staysClosed_untilThresholdReached() {
        MutableClock clock = new MutableClock();
        SimpleCircuitBreaker breaker = new SimpleCircuitBreaker(3, COOLDOWN, clock);

        breaker.onFailure();
        breaker.onFailure();

        assertThat(breaker.state()).isEqualTo(State.CLOSED);
        assertThat(breaker.tryAcquire()).isTrue();
    }

    @Test
    void opens_afterConsecutiveFailuresReachThreshold_andBlocksDuringCooldown() {
        MutableClock clock = new MutableClock();
        SimpleCircuitBreaker breaker = new SimpleCircuitBreaker(3, COOLDOWN, clock);

        breaker.onFailure();
        breaker.onFailure();
        breaker.onFailure();

        assertThat(breaker.state()).isEqualTo(State.OPEN);
        assertThat(breaker.tryAcquire()).isFalse(); // 쿨다운 중에는 차단
    }

    @Test
    void success_resetsFailureCount() {
        MutableClock clock = new MutableClock();
        SimpleCircuitBreaker breaker = new SimpleCircuitBreaker(3, COOLDOWN, clock);

        breaker.onFailure();
        breaker.onFailure();
        breaker.onSuccess();
        breaker.onFailure();
        breaker.onFailure();

        assertThat(breaker.state()).isEqualTo(State.CLOSED); // 리셋됐으므로 아직 임계치 미달
    }

    @Test
    void halfOpen_allowsSingleProbe_afterCooldown() {
        MutableClock clock = new MutableClock();
        SimpleCircuitBreaker breaker = new SimpleCircuitBreaker(1, COOLDOWN, clock);

        breaker.onFailure(); // OPEN
        clock.advance(COOLDOWN.plusSeconds(1));

        assertThat(breaker.tryAcquire()).isTrue();  // 첫 요청 = 탐침
        assertThat(breaker.state()).isEqualTo(State.HALF_OPEN);
        assertThat(breaker.tryAcquire()).isFalse(); // 탐침 진행 중 → 나머지는 차단
    }

    @Test
    void halfOpenProbeSuccess_closesCircuit() {
        MutableClock clock = new MutableClock();
        SimpleCircuitBreaker breaker = new SimpleCircuitBreaker(1, COOLDOWN, clock);

        breaker.onFailure(); // OPEN
        clock.advance(COOLDOWN.plusSeconds(1));
        breaker.tryAcquire(); // HALF_OPEN 탐침
        breaker.onSuccess();

        assertThat(breaker.state()).isEqualTo(State.CLOSED);
        assertThat(breaker.tryAcquire()).isTrue();
    }

    @Test
    void halfOpenProbeFailure_reopensCircuit_andRestartsCooldown() {
        MutableClock clock = new MutableClock();
        SimpleCircuitBreaker breaker = new SimpleCircuitBreaker(1, COOLDOWN, clock);

        breaker.onFailure(); // OPEN
        clock.advance(COOLDOWN.plusSeconds(1));
        breaker.tryAcquire(); // HALF_OPEN 탐침
        breaker.onFailure();  // 탐침 실패 → 다시 OPEN

        assertThat(breaker.state()).isEqualTo(State.OPEN);
        assertThat(breaker.tryAcquire()).isFalse(); // 쿨다운 재시작됨
    }

    /** 테스트에서 시간을 수동으로 전진시키는 Clock. */
    private static final class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration d) {
            instant = instant.plus(d);
        }

        @Override public Instant instant() { return instant; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public long millis() { return instant.toEpochMilli(); }
    }
}
