package com.deoham.global.security;

import com.deoham.chat.repository.ChatRoomMemberRepository;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern ROOM_DESTINATION_PATTERN = Pattern.compile("/sub/chat/rooms/([^/]+)");

    private final JwtDecoder jwtDecoder;
    private final SupabaseJwtAuthenticationConverter authenticationConverter;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

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
        }

        return message;
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        String token = extractBearerToken(authHeader);
        Jwt jwt = jwtDecoder.decode(token);
        Authentication authentication = authenticationConverter.convert(jwt);
        accessor.setUser(authentication);
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        SupabasePrincipal principal = SupabaseAuthenticationUtils.fromAuthentication(accessor.getUser() instanceof Authentication auth ? auth : null)
                .orElseThrow(() -> new AuthenticationServiceException("인증되지 않은 구독 요청입니다"));

        String destination = accessor.getDestination();
        UUID roomId = extractRoomId(destination);
        if (roomId == null) {
            return;
        }

        boolean isMember = chatRoomMemberRepository.existsByChatRoomIdAndUserIdAndLeftAtIsNull(roomId, principal.userId());
        if (!isMember) {
            throw new AccessDeniedException("채팅방 멤버가 아닙니다");
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
