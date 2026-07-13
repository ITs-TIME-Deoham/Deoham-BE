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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomAccessService chatRoomAccessService;
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
            log.warn("메시지 전송 거부 [roomId={}, senderId={}]: 클라이언트가 ROOM_CLOSED 타입을 전송함", roomId, senderId);
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "ROOM_CLOSED 타입은 클라이언트가 전송할 수 없습니다");
        }
        try {
            ChatRoom room = findActiveRoomOrThrow(roomId);
            User sender = userRepository.findById(senderId)
                    .orElseThrow(() -> {
                        log.warn("메시지 전송 실패 [roomId={}, senderId={}]: 사용자를 찾을 수 없음", roomId, senderId);
                        return new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다");
                    });
            chatRoomAccessService.requireParticipant(room.getCard(), senderId);

            ChatMessage saved = chatMessageRepository.save(ChatMessage.builder()
                    .chatRoom(room)
                    .sender(sender)
                    .content(resolveContent(request))
                    .messageType(request.messageType())
                    .build());

            notifyOtherParticipant(room, senderId, saved);
            ChatMessageResponse response = toResponse(saved);
            metricsRegistry.recordChatMessageSendSuccess(sample);
            log.info("메시지 저장 완료 [roomId={}, messageId={}, senderId={}, type={}]",
                    roomId, saved.getId(), senderId, request.messageType());
            return response;
        } catch (Exception exception) {
            metricsRegistry.recordChatMessageSendFailure(sample, exception);
            log.warn("메시지 전송 실패 [roomId={}, senderId={}]: {}", roomId, senderId, exception.getMessage());
            throw exception;
        }
    }

    @Transactional
    public void sendRoomClosedMessage(ChatRoom room, UUID actorId) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        ChatMessage saved = chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room)
                .sender(actor)
                .content(actor.getNickname() + "님이 채팅을 종료했습니다")
                .messageType(ChatMessageType.ROOM_CLOSED)
                .build());

        messagingTemplate.convertAndSend("/sub/chat/rooms/" + room.getId(), toResponse(saved));
        log.info("채팅방 종료 안내 메시지 브로드캐스트 완료 [roomId={}, actorId={}]", room.getId(), actorId);
    }

    @Transactional
    public void markMessagesAsRead(UUID roomId, UUID userId) {
        ChatRoom room = chatRoomAccessService.findRoomOrThrow(roomId);
        chatRoomAccessService.requireParticipant(room.getCard(), userId);

        List<ChatMessage> unread =
                chatMessageRepository.findByChatRoomIdAndSenderIdNotAndReadAtIsNull(roomId, userId);
        if (unread.isEmpty()) {
            log.debug("읽음 처리 요청 무시 [roomId={}, userId={}]: 읽지 않은 메시지 없음", roomId, userId);
            return;
        }

        List<UUID> readMessageIds = unread.stream().map(ChatMessage::getId).toList();
        unread.forEach(ChatMessage::markRead);

        messagingTemplate.convertAndSend(
                "/sub/chat/rooms/" + roomId + "/read",
                new ChatReadEvent(roomId, userId, readMessageIds, Instant.now()));
        log.info("메시지 읽음 처리 완료 [roomId={}, userId={}, count={}]", roomId, userId, readMessageIds.size());
    }

    public ChatMessagePageResponse getMessages(UUID roomId, UUID userId, Instant before, int size) {
        Timer.Sample sample = metricsRegistry.startChatMessageGetTimer();
        try {
            ChatRoom room = chatRoomAccessService.findRoomOrThrow(roomId);
            chatRoomAccessService.requireParticipant(room.getCard(), userId);

            PageRequest pageRequest = PageRequest.of(0, size + 1);
            List<ChatMessage> messages = before != null
                    ? chatMessageRepository.findByChatRoomIdAndSentAtBeforeOrderBySentAtDesc(roomId, before, pageRequest)
                    : chatMessageRepository.findByChatRoomIdOrderBySentAtDesc(roomId, pageRequest);

            boolean hasNext = messages.size() > size;
            List<ChatMessage> page = hasNext ? messages.subList(0, size) : messages;
            Instant nextCursor = hasNext ? page.get(page.size() - 1).getSentAt() : null;

            ChatMessagePageResponse response = new ChatMessagePageResponse(page.stream().map(this::toResponse).toList(), hasNext, nextCursor);
            metricsRegistry.recordChatMessageGetSuccess(sample);
            log.debug("메시지 조회 완료 [roomId={}, userId={}, size={}, returned={}]", roomId, userId, size, page.size());
            return response;
        } catch (Exception exception) {
            metricsRegistry.recordChatMessageGetFailure(sample, exception);
            log.warn("메시지 조회 실패 [roomId={}, userId={}]: {}", roomId, userId, exception.getMessage());
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

    private ChatRoom findActiveRoomOrThrow(UUID roomId) {
        ChatRoom room = chatRoomAccessService.findRoomOrThrow(roomId);
        if (room.getStatus() == ChatRoomStatus.CLOSED) {
            log.warn("메시지 전송 거부 [roomId={}]: 종료된 채팅방", roomId);
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
