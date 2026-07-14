package com.deoham.chat.event;

import com.deoham.chat.service.LiveLocationSessionRegistry;
import com.deoham.chat.service.LiveLocationSessionRegistry.Entry;
import com.deoham.chat.service.LiveLocationService;
import com.deoham.global.security.AuthPrincipal;
import com.deoham.global.security.AuthenticationUtils;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

/**
 * 실시간 위치 공유의 시작/종료를 STOMP 세션 생명주기에 연동한다.
 * <ul>
 *   <li>live-location 토픽 구독 = 화면 진입(자동 시작) → presence 등록</li>
 *   <li>구독 해제 / 연결 종료(강제종료·네트워크 끊김 포함) → 상대에게 종료(STOP) 브로드캐스트 + 캐시 정리</li>
 * </ul>
 * Spring 이벤트 멀티캐스터로 호출되므로 STOMP 인바운드 채널 빈 그래프 밖이라
 * {@link LiveLocationService}(SimpMessagingTemplate 의존)를 안전하게 주입할 수 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveLocationSessionListener {

    private static final Pattern LIVE_LOCATION_DESTINATION =
            Pattern.compile("^/sub/chat/rooms/([^/]+)/live-location$");

    private final LiveLocationSessionRegistry registry;
    private final LiveLocationService liveLocationService;

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        UUID roomId = extractRoomId(accessor.getDestination());
        if (roomId == null) {
            return;
        }
        Optional<UUID> userId = resolveUserId(event.getUser());
        if (userId.isEmpty()) {
            return;
        }
        registry.register(accessor.getSessionId(), accessor.getSubscriptionId(), roomId, userId.get());
        log.debug("실시간 위치 공유 시작(구독) [roomId={}, userId={}]", roomId, userId.get());
    }

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        registry.removeBySubscription(accessor.getSessionId(), accessor.getSubscriptionId())
                .ifPresent(this::teardown);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        registry.removeBySession(event.getSessionId())
                .ifPresent(this::teardown);
    }

    private void teardown(Entry entry) {
        liveLocationService.stop(entry.roomId(), entry.userId());
    }

    private static UUID extractRoomId(String destination) {
        if (destination == null) {
            return null;
        }
        Matcher matcher = LIVE_LOCATION_DESTINATION.matcher(destination);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return UUID.fromString(matcher.group(1));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Optional<UUID> resolveUserId(java.security.Principal principal) {
        if (!(principal instanceof Authentication authentication)) {
            return Optional.empty();
        }
        return AuthenticationUtils.fromAuthentication(authentication).map(AuthPrincipal::userId);
    }
}
