package com.deoham.chat.service;

import com.deoham.card.entity.Card;
import com.deoham.card.entity.CardApplyStatus;
import com.deoham.card.repository.CardApplyRepository;
import com.deoham.chat.dto.ChatMessagePageResponse;
import com.deoham.chat.dto.ChatMessageResponse;
import com.deoham.chat.dto.ChatMessageSendRequest;
import com.deoham.chat.entity.ChatMessage;
import com.deoham.chat.entity.ChatRoom;
import com.deoham.chat.repository.ChatMessageRepository;
import com.deoham.chat.repository.ChatRoomRepository;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.notification.service.NotificationService;
import com.deoham.user.entity.User;
import com.deoham.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final CardApplyRepository cardApplyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public ChatMessageResponse sendMessage(UUID roomId, UUID senderId, ChatMessageSendRequest request) {
        ChatRoom room = findActiveRoomOrThrow(roomId);
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다"));
        requireParticipant(room.getCard(), senderId);

        ChatMessage saved = chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(request.content())
                .messageType(request.messageType())
                .build());

        notifyOtherParticipant(room, senderId, saved);
        return toResponse(saved);
    }

    public ChatMessagePageResponse getMessages(UUID roomId, UUID userId, Instant before, int size) {
        ChatRoom room = findActiveRoomOrThrow(roomId);
        requireParticipant(room.getCard(), userId);

        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<ChatMessage> messages = before != null
                ? chatMessageRepository.findByChatRoomIdAndSentAtBeforeOrderBySentAtDesc(roomId, before, pageRequest)
                : chatMessageRepository.findByChatRoomIdOrderBySentAtDesc(roomId, pageRequest);

        boolean hasNext = messages.size() > size;
        List<ChatMessage> page = hasNext ? messages.subList(0, size) : messages;
        Instant nextCursor = hasNext ? page.get(page.size() - 1).getSentAt() : null;

        return new ChatMessagePageResponse(page.stream().map(this::toResponse).toList(), hasNext, nextCursor);
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

    private void requireParticipant(Card card, UUID userId) {
        if (card.getRequester().getId().equals(userId)) return;
        boolean isAcceptedApplicant = cardApplyRepository.findByCard(card).stream()
                .anyMatch(a -> a.getStatus() == CardApplyStatus.ACCEPTED
                            && a.getApplicant().getId().equals(userId));
        if (!isAcceptedApplicant) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "채팅방 참여자가 아닙니다");
        }
    }

    private ChatRoom findActiveRoomOrThrow(UUID roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "채팅방을 찾을 수 없습니다"));
        if (room.getStatus().name().equals("CLOSED")) {
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
