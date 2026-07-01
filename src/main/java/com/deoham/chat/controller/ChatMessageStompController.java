package com.deoham.chat.controller;

import com.deoham.chat.dto.ChatMessageResponse;
import com.deoham.chat.dto.ChatMessageSendRequest;
import com.deoham.chat.service.ChatMessageService;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.global.security.AuthenticationUtils;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageStompController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/rooms/{roomId}/messages")
    public void sendMessage(@DestinationVariable UUID roomId,
                            @Payload ChatMessageSendRequest request,
                            Principal principal) {
        UUID senderId = resolveUserId(principal);
        ChatMessageResponse saved = chatMessageService.sendMessage(roomId, senderId, request);
        messagingTemplate.convertAndSend("/sub/chat/rooms/" + roomId, saved);
    }

    @MessageExceptionHandler(BusinessException.class)
    public void handleBusinessException(BusinessException ex) {
        log.warn("STOMP BusinessException [{}]: {}", ex.getErrorCode().name(), ex.getMessage());
    }

    private UUID resolveUserId(Principal principal) {
        if (!(principal instanceof Authentication authentication)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return AuthenticationUtils.fromAuthentication(authentication)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED))
                .userId();
    }
}
