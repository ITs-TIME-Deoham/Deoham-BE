package com.deoham.chat.translation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * 단일 다운스트림(DeepL) 보호용 경량 서킷 브레이커. 외부 라이브러리 없이 필요한 최소 동작만 담는다.
 * 모든 상태 전이가 {@code synchronized}라 스레드 세이프하다.
 *
 * <ul>
 *   <li><b>CLOSED</b> — 정상. 연속 실패가 {@code failureThreshold}에 도달하면 OPEN 으로 전환.</li>
 *   <li><b>OPEN</b> — {@code openDuration} 동안 요청 차단(곧장 fallback). 경과 후 첫 요청 1건만
 *       HALF_OPEN 탐침으로 허용.</li>
 *   <li><b>HALF_OPEN</b> — 탐침 1건 진행 중. 성공 → CLOSED, 실패 → OPEN(쿨다운 재시작).
 *       탐침 진행 중 들어온 다른 요청은 허용하지 않아(곧장 fallback) 죽은 다운스트림을 한 번에 하나만 두드린다.</li>
 * </ul>
 *
 * 호출자는 {@link #tryAcquire()}가 true를 준 경우 반드시 {@link #onSuccess()} 또는 {@link #onFailure()}
 * 중 하나로 결과를 보고해야 한다(HALF_OPEN 탐침이 미결로 남지 않도록).
 */
class SimpleCircuitBreaker {

    enum State { CLOSED, OPEN, HALF_OPEN }

    private final int failureThreshold;
    private final Duration openDuration;
    private final Clock clock;

    private State state = State.CLOSED;
    private int consecutiveFailures = 0;
    private Instant openedAt = Instant.EPOCH;
    private boolean probeInFlight = false;

    SimpleCircuitBreaker(int failureThreshold, Duration openDuration, Clock clock) {
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
        this.clock = clock;
    }

    /** 다운스트림을 시도해도 되는지. true면 호출자는 반드시 결과를 {@link #onSuccess()}/{@link #onFailure()}로 보고한다. */
    synchronized boolean tryAcquire() {
        switch (state) {
            case OPEN:
                if (cooldownElapsed()) {
                    state = State.HALF_OPEN;
                    probeInFlight = true;
                    return true; // 이 요청이 탐침
                }
                return false;
            case HALF_OPEN:
                if (probeInFlight) {
                    return false; // 이미 탐침 진행 중 → 나머지는 곧장 fallback
                }
                probeInFlight = true;
                return true;
            case CLOSED:
            default:
                return true;
        }
    }

    synchronized void onSuccess() {
        state = State.CLOSED;
        consecutiveFailures = 0;
        probeInFlight = false;
    }

    synchronized void onFailure() {
        probeInFlight = false;
        if (state == State.HALF_OPEN) {
            open(); // 탐침 실패 → 다시 열고 쿨다운 재시작
            return;
        }
        consecutiveFailures++;
        if (consecutiveFailures >= failureThreshold) {
            open();
        }
    }

    /** 관측/테스트용 현재 상태. */
    synchronized State state() {
        return state;
    }

    private void open() {
        state = State.OPEN;
        openedAt = Instant.now(clock);
        consecutiveFailures = 0;
    }

    private boolean cooldownElapsed() {
        return !Instant.now(clock).isBefore(openedAt.plus(openDuration));
    }
}
