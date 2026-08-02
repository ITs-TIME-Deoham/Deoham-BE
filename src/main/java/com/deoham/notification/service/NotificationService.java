package com.deoham.notification.service;

import com.deoham.card.entity.CardApply;
import com.deoham.chat.entity.ChatMessage;
import com.deoham.chat.entity.ChatMessageType;
import com.deoham.notification.entity.Notification;
import com.deoham.notification.entity.NotifyType;
import com.deoham.notification.event.FcmPushEvent;
import com.deoham.notification.repository.NotificationRepository;
import com.deoham.user.entity.User;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final int PREVIEW_MAX_LENGTH = 50;

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void notifyChatMessage(ChatMessage chatMessage, List<User> recipients) {
        String preview = buildPreviewText(chatMessage);
        UUID chatRoomId = chatMessage.getChatRoom().getId();
        recipients.forEach(recipient -> {
            notificationRepository.save(Notification.builder()
                    .user(recipient)
                    .type(NotifyType.CHAT_MESSAGE)
                    .referenceId(chatMessage.getId())
                    .targetId(chatRoomId)
                    .message(preview)
                    .build());
            eventPublisher.publishEvent(new FcmPushEvent(
                    recipient.getId(),
                    NotifyType.CHAT_MESSAGE,
                    "새 메시지",
                    preview,
                    Map.of(
                            "type", NotifyType.CHAT_MESSAGE.name(),
                            "chatRoomId", chatRoomId.toString(),
                            "messageId", chatMessage.getId().toString())));
        });
    }

    @Transactional
    public void notifyCardApplied(CardApply apply) {
        String preview = apply.getApplicant().getNickname() + "님이 회원님의 카드에 지원했습니다";
        User recipient = apply.getCard().getRequester();
        notificationRepository.save(Notification.builder()
                .user(recipient)
                .type(NotifyType.CARD_APPLIED)
                .referenceId(apply.getId())
                .targetId(apply.getCard().getId())
                .message(preview)
                .build());
        eventPublisher.publishEvent(new FcmPushEvent(
                recipient.getId(),
                NotifyType.CARD_APPLIED,
                "카드 지원 알림",
                preview,
                Map.of(
                        "type", NotifyType.CARD_APPLIED.name(),
                        "cardId", apply.getCard().getId().toString(),
                        "applyId", apply.getId().toString())));
    }

    @Transactional
    public void notifyMatchAccepted(CardApply apply) {
        String preview = apply.getCard().getRequester().getNickname() + "님과 매칭되었습니다";
        User recipient = apply.getApplicant();
        notificationRepository.save(Notification.builder()
                .user(recipient)
                .type(NotifyType.MATCH_ACCEPTED)
                .referenceId(apply.getId())
                .targetId(apply.getCard().getId())
                .message(preview)
                .build());
        eventPublisher.publishEvent(new FcmPushEvent(
                recipient.getId(),
                NotifyType.MATCH_ACCEPTED,
                "매칭 완료",
                preview,
                Map.of(
                        "type", NotifyType.MATCH_ACCEPTED.name(),
                        "cardId", apply.getCard().getId().toString(),
                        "applyId", apply.getId().toString())));
    }

    private String buildPreviewText(ChatMessage chatMessage) {
        String senderNickname = chatMessage.getSender().getNickname();
        if (chatMessage.getMessageType() == ChatMessageType.TEXT) {
            String content = chatMessage.getContent();
            String trimmed = content.length() > PREVIEW_MAX_LENGTH
                    ? content.substring(0, PREVIEW_MAX_LENGTH) + "..."
                    : content;
            return senderNickname + ": " + trimmed;
        }
        return senderNickname + "님이 " + (chatMessage.getMessageType() == ChatMessageType.IMAGE ? "사진을" : "위치를") + " 보냈습니다";
    }
}
