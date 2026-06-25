package com.deoham.chat.service;

import com.deoham.chat.dto.ChatMessagePageResponse;
import com.deoham.chat.dto.ChatMessageResponse;
import com.deoham.chat.dto.ChatMessageSendRequest;
import com.deoham.chat.entity.ChatMessage;
import com.deoham.chat.entity.ChatMessageType;
import com.deoham.chat.entity.ChatRoomMember;
import com.deoham.chat.entity.ChatRoomMember;
import com.deoham.chat.repository.ChatMessageRepository;
import com.deoham.chat.repository.ChatRoomMemberRepository;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.notification.service.NotificationService;
import com.deoham.user.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final NotificationService notificationService;

    @Transactional
    public ChatMessageResponse sendMessage(UUID roomId, UUID senderId, ChatMessageSendRequest request) {
        ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndUserIdAndLeftAtIsNull(roomId, senderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "채팅방 멤버가 아닙니다"));
        validateContentByType(request);

        ChatMessage saved = chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(member.getChatRoom())
                .sender(member.getUser())
                .messageType(request.messageType())
                .content(request.content())
                .attachmentUrl(request.attachmentUrl())
                .attachmentFileName(request.attachmentFileName())
                .attachmentContentType(request.attachmentContentType())
                .attachmentSizeBytes(request.attachmentSizeBytes())
                .build());

        member.getChatRoom().touchLastMessageAt(saved.getCreatedAt());
        notifyOtherMembers(roomId, senderId, saved);

        return toResponse(saved);
    }

    public ChatMessagePageResponse getMessages(UUID userId, UUID roomId, Instant before, int size) {
        boolean isMember = chatRoomMemberRepository.existsByChatRoomIdAndUserIdAndLeftAtIsNull(roomId, userId);
        if (!isMember) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "채팅방 멤버가 아닙니다");
        }

        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<ChatMessage> messages = before != null
                ? chatMessageRepository.findByChatRoomIdAndCreatedAtLessThanOrderByCreatedAtDesc(roomId, before, pageRequest)
                : chatMessageRepository.findByChatRoomIdOrderByCreatedAtDesc(roomId, pageRequest);

        boolean hasNext = messages.size() > size;
        List<ChatMessage> page = hasNext ? messages.subList(0, size) : messages;
        Instant nextCursor = hasNext ? page.get(page.size() - 1).getCreatedAt() : null;

        return new ChatMessagePageResponse(page.stream().map(this::toResponse).toList(), hasNext, nextCursor);
    }

    private void notifyOtherMembers(UUID roomId, UUID senderId, ChatMessage message) {
        List<User> recipients = chatRoomMemberRepository.findByChatRoomIdAndLeftAtIsNull(roomId).stream()
                .map(ChatRoomMember::getUser)
                .filter(user -> !user.getId().equals(senderId))
                .toList();
        if (!recipients.isEmpty()) {
            notificationService.notifyChatMessage(message, recipients);
        }
    }

    private void validateContentByType(ChatMessageSendRequest request) {
        if (request.messageType() == ChatMessageType.TEXT) {
            if (!StringUtils.hasText(request.content())) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "텍스트 메시지는 content가 필요합니다");
            }
        } else {
            if (!StringUtils.hasText(request.attachmentUrl())) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "첨부 메시지는 attachmentUrl이 필요합니다");
            }
        }
    }

    private ChatMessageResponse toResponse(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoom().getId(),
                message.getSender().getId(),
                message.getSender().getName(),
                message.getMessageType().name(),
                message.getContent(),
                message.getAttachmentUrl(),
                message.getAttachmentFileName(),
                message.getAttachmentContentType(),
                message.getAttachmentSizeBytes(),
                message.getCreatedAt());
    }
}
