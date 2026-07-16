package com.deoham.chat.service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 실시간 위치 공유 중인 STOMP 세션을 추적하는 인메모리 presence 레지스트리.
 * 세션이 {@code /sub/chat/rooms/{roomId}/live-location} 을 구독하면 등록되고,
 * 구독 해제(Unsubscribe)/연결 종료(Disconnect) 시 제거되어 상대에게 종료(STOP)를 알리는 데 쓰인다.
 *
 * <p>단일 노드 배포 전제라 인메모리 맵으로 충분하다(다중 노드 확장 시 Redis 등으로 이전).
 * 한 세션(채팅 화면)당 live-location 구독은 하나이므로 sessionId 를 키로 둔다.
 */
@Component
public class LiveLocationSessionRegistry {

    public record Entry(String subscriptionId, UUID roomId, UUID userId) {
    }

    private final ConcurrentHashMap<String, Entry> sessions = new ConcurrentHashMap<>();

    public void register(String sessionId, String subscriptionId, UUID roomId, UUID userId) {
        sessions.put(sessionId, new Entry(subscriptionId, roomId, userId));
    }

    /** 해당 세션의 특정 구독이 해제된 경우에만 제거하고 반환한다. */
    public Optional<Entry> removeBySubscription(String sessionId, String subscriptionId) {
        Entry current = sessions.get(sessionId);
        if (current != null && current.subscriptionId().equals(subscriptionId)
                && sessions.remove(sessionId, current)) {
            return Optional.of(current);
        }
        return Optional.empty();
    }

    /** 세션이 종료되면 등록 정보를 제거하고 반환한다. */
    public Optional<Entry> removeBySession(String sessionId) {
        return Optional.ofNullable(sessions.remove(sessionId));
    }
}
