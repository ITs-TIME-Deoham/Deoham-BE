package com.deoham.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
public class WebSocketEventListener {

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info("STOMP 세션 연결됨 [sessionId={}, principal={}]",
                accessor.getSessionId(), event.getUser() != null ? event.getUser().getName() : null);
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        log.info("STOMP 세션 종료 [sessionId={}, principal={}, closeStatus={}]",
                event.getSessionId(),
                event.getUser() != null ? event.getUser().getName() : null,
                event.getCloseStatus());
    }
}
