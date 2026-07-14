package com.deoham.global.security;

import com.deoham.chat.service.ChatRoomAccessService;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern ROOM_DESTINATION_PATTERN = Pattern.compile("/sub/chat/rooms/([^/]+)");

    private final JwtDecoder jwtDecoder;
    private final AppJwtAuthenticationConverter authenticationConverter;
    private final ChatRoomAccessService chatRoomAccessService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscribe(accessor);
        } else if (StompCommand.SEND.equals(accessor.getCommand())) {
            authorizeSend(accessor);
        }

        return message;
    }

    /**
     * SimpleBroker는 클라이언트가 /sub/** 로 직접 SEND한 프레임을 구독자 전체에 그대로 중계한다.
     * 컨트롤러(@MessageMapping)와 참여자 검증을 우회한 위조 브로드캐스트를 막기 위해
     * 애플리케이션 prefix(/pub)로 향하는 인증된 SEND만 허용한다.
     */
    private void authorizeSend(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        String destination = accessor.getDestination();

        if (!(accessor.getUser() instanceof Authentication auth)
                || AuthenticationUtils.fromAuthentication(auth).isEmpty()) {
            log.warn("STOMP SEND 거부 [sessionId={}, destination={}]: 인증되지 않은 전송 요청", sessionId, destination);
            throw new AuthenticationServiceException("인증되지 않은 전송 요청입니다");
        }
        if (destination == null || !destination.startsWith("/pub/")) {
            log.warn("STOMP SEND 거부 [sessionId={}, destination={}]: 허용되지 않은 목적지", sessionId, destination);
            throw new AccessDeniedException("허용되지 않은 목적지입니다");
        }
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        try {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            String token = extractBearerToken(authHeader);
            Jwt jwt = jwtDecoder.decode(token);
            Authentication authentication = authenticationConverter.convert(jwt);
            accessor.setUser(authentication);
            log.info("STOMP CONNECT 인증 성공 [sessionId={}, principal={}]", sessionId, authentication.getName());
        } catch (Exception ex) {
            log.warn("STOMP CONNECT 인증 실패 [sessionId={}]: {}", sessionId, ex.getMessage());
            throw ex;
        }
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        String destination = accessor.getDestination();
        AuthPrincipal principal = AuthenticationUtils.fromAuthentication(
                accessor.getUser() instanceof Authentication auth ? auth : null)
                .orElseThrow(() -> {
                    log.warn("STOMP SUBSCRIBE 거부 [sessionId={}, destination={}]: 인증되지 않은 구독 요청", sessionId, destination);
                    return new AuthenticationServiceException("인증되지 않은 구독 요청입니다");
                });

        UUID roomId = extractRoomId(destination);
        if (roomId == null) {
            log.debug("STOMP SUBSCRIBE 허용 [sessionId={}, userId={}, destination={}]: 채팅방 목적지 아님",
                    sessionId, principal.userId(), destination);
            return;
        }

        UUID userId = principal.userId();
        try {
            chatRoomAccessService.verifySubscribeAccess(roomId, userId);
            log.info("STOMP SUBSCRIBE 허용 [sessionId={}, userId={}, roomId={}]", sessionId, userId, roomId);
        } catch (RuntimeException ex) {
            log.warn("STOMP SUBSCRIBE 거부 [sessionId={}, userId={}, roomId={}]: {}", sessionId, userId, roomId, ex.getMessage());
            throw ex;
        }
    }

    private static UUID extractRoomId(String destination) {
        if (destination == null) {
            return null;
        }
        Matcher matcher = ROOM_DESTINATION_PATTERN.matcher(destination);
        if (!matcher.find()) {
            return null;
        }
        try {
            return UUID.fromString(matcher.group(1));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadCredentialsException("Authorization 헤더가 없습니다");
        }
        return authHeader.substring("Bearer ".length());
    }
}
