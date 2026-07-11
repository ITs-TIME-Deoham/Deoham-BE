package com.deoham.chat.service;

import com.deoham.card.entity.Card;
import com.deoham.card.entity.CardApplyStatus;
import com.deoham.card.repository.CardApplyRepository;
import com.deoham.chat.dto.ChatMessagePageResponse;
import com.deoham.chat.dto.ChatMessageResponse;
import com.deoham.chat.dto.ChatMessageSendRequest;
import com.deoham.chat.dto.ChatReadEvent;
import com.deoham.chat.entity.ChatMessage;
import com.deoham.chat.entity.ChatMessageType;
import com.deoham.chat.entity.ChatRoom;
import com.deoham.chat.entity.ChatRoomStatus;
import com.deoham.chat.repository.ChatMessageRepository;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.global.metrics.MetricsRegistry;
import com.deoham.notification.service.NotificationService;
import com.deoham.user.entity.User;
import com.deoham.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatAccessGuard chatAccessGuard;
    private final CardApplyRepository cardApplyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final MetricsRegistry metricsRegistry;

    @Transactional
    public ChatMessageResponse sendMessage(UUID roomId, UUID senderId, ChatMessageSendRequest request) {
        Timer.Sample sample = metricsRegistry.startChatMessageSendTimer();
        if (request.messageType() == ChatMessageType.ROOM_CLOSED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "ROOM_CLOSED 타입은 클라이언트가 전송할 수 없습니다");
        }
        try {
            ChatRoom room = findActiveRoomOrThrow(roomId);
            User sender = findUserOrThrow(senderId);
            chatAccessGuard.requireParticipant(room.getCard(), senderId);

            ChatMessage saved = chatMessageRepository.save(ChatMessage.builder()
                    .chatRoom(room)
                    .sender(sender)
                    .content(resolveContent(request))
                    .messageType(request.messageType())
                    .build());

            notifyOtherParticipant(room, senderId, saved);
            ChatMessageResponse response = toResponse(saved);
            metricsRegistry.recordChatMessageSendSuccess(sample);
            return response;
        } catch (Exception exception) {
            metricsRegistry.recordChatMessageSendFailure(sample, exception);
            throw exception;
        }
    }

    @Transactional
    public void sendRoomClosedMessage(ChatRoom room, UUID actorId) {
        User actor = findUserOrThrow(actorId);

        ChatMessage saved = chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room)
                .sender(actor)
                .content(actor.getNickname() + "님이 채팅을 종료했습니다")
                .messageType(ChatMessageType.ROOM_CLOSED)
                .build());

        messagingTemplate.convertAndSend("/sub/chat/rooms/" + room.getId(), toResponse(saved));
    }

    @Transactional
    public void markMessagesAsRead(UUID roomId, UUID userId) {
        ChatRoom room = chatAccessGuard.findRoomOrThrow(roomId);
        chatAccessGuard.requireParticipant(room.getCard(), userId);

        List<ChatMessage> unread =
                chatMessageRepository.findByChatRoomIdAndSenderIdNotAndReadAtIsNull(roomId, userId);
        if (unread.isEmpty()) {
            return;
        }

        List<UUID> readMessageIds = unread.stream().map(ChatMessage::getId).toList();
        unread.forEach(ChatMessage::markRead);

        messagingTemplate.convertAndSend(
                "/sub/chat/rooms/" + roomId + "/read",
                new ChatReadEvent(roomId, userId, readMessageIds, Instant.now()));
    }

    public ChatMessagePageResponse getMessages(UUID roomId, UUID userId, Instant before, int size) {
        Timer.Sample sample = metricsRegistry.startChatMessageGetTimer();
        try {
            ChatRoom room = chatAccessGuard.findRoomOrThrow(roomId);
            chatAccessGuard.requireParticipant(room.getCard(), userId);

            PageRequest pageRequest = PageRequest.of(0, size + 1);
            List<ChatMessage> messages = before != null
                    ? chatMessageRepository.findByChatRoomIdAndSentAtBeforeOrderBySentAtDesc(roomId, before, pageRequest)
                    : chatMessageRepository.findByChatRoomIdOrderBySentAtDesc(roomId, pageRequest);

            boolean hasNext = messages.size() > size;
            List<ChatMessage> page = hasNext ? messages.subList(0, size) : messages;
            Instant nextCursor = hasNext ? page.get(page.size() - 1).getSentAt() : null;

            ChatMessagePageResponse response = new ChatMessagePageResponse(page.stream().map(this::toResponse).toList(), hasNext, nextCursor);
            metricsRegistry.recordChatMessageGetSuccess(sample);
            return response;
        } catch (Exception exception) {
            metricsRegistry.recordChatMessageGetFailure(sample, exception);
            throw exception;
        }
    }

    private void notifyOtherParticipant(ChatRoom room, UUID senderId, ChatMessage message) {
        Card card = room.getCard();
        User otherParticipant;
        if (card.getRequester().getId().equals(senderId)) {
            otherParticipant = cardApplyRepository.findByCard(card).stream()
                    .filter(a -> a.getStatus() == CardApplyStatus.ACCEPTED)
                    .map(a -> a.getApplicant())
                    .findFirst()
                    .orElse(null);
        } else {
            otherParticipant = card.getRequester();
        }
        if (otherParticipant != null) {
            notificationService.notifyChatMessage(message, List.of(otherParticipant));
        }
    }

    private String resolveContent(ChatMessageSendRequest request) {
        if (request.messageType() == ChatMessageType.LOCATION) {
            if (request.location() == null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "LOCATION 타입은 location 필드가 필요합니다");
            }
            try {
                return objectMapper.writeValueAsString(request.location());
            } catch (JsonProcessingException e) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "위치 정보 직렬화에 실패했습니다");
            }
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "content는 필수입니다");
        }
        return request.content();
    }

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다"));
    }

    private ChatRoom findActiveRoomOrThrow(UUID roomId) {
        ChatRoom room = chatAccessGuard.findRoomOrThrow(roomId);
        if (room.getStatus() == ChatRoomStatus.CLOSED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "종료된 채팅방입니다");
        }
        return room;
    }

    private ChatMessageResponse toResponse(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoom().getId(),
                message.getSender().getId(),
                message.getSender().getNickname(),
                message.getMessageType().name(),
                message.getContent(),
                message.getSentAt(),
                message.getReadAt());
    }
}
