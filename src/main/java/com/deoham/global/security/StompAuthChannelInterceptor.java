package com.deoham.global.security;

import com.deoham.card.entity.CardApplyStatus;
import com.deoham.card.repository.CardApplyRepository;
import com.deoham.chat.entity.ChatRoom;
import com.deoham.chat.repository.ChatRoomRepository;
import com.deoham.global.security.jwt.JwtTokenProvider;
import java.util.List;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern ROOM_DESTINATION_PATTERN = Pattern.compile("/sub/chat/rooms/([^/]+)");

    private final JwtTokenProvider jwtTokenProvider;
    private final ChatRoomRepository chatRoomRepository;
    private final CardApplyRepository cardApplyRepository;

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
        Jwt jwt = jwtTokenProvider.parseToken(token);
        String role = jwt.getClaimAsString("role");
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "USER")));
        accessor.setUser(new JwtAuthenticationToken(jwt, authorities));
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        AuthPrincipal principal = AuthenticationUtils.fromAuthentication(
                accessor.getUser() instanceof Authentication auth ? auth : null)
                .orElseThrow(() -> new AuthenticationServiceException("Unauthenticated subscription request."));

        String destination = accessor.getDestination();
        UUID roomId = extractRoomId(destination);
        if (roomId == null) {
            return;
        }

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new AccessDeniedException("Chat room not found."));

        UUID userId = principal.userId();
        var card = room.getCard();
        if (card.getRequester().getId().equals(userId)) return;

        boolean isAccepted = cardApplyRepository.findByCard(card).stream()
                .anyMatch(a -> a.getStatus() == CardApplyStatus.ACCEPTED
                            && a.getApplicant().getId().equals(userId));
        if (!isAccepted) {
            throw new AccessDeniedException("User is not a chat room participant.");
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
            throw new BadCredentialsException("Authorization header is missing.");
        }
        return authHeader.substring("Bearer ".length());
    }
}
