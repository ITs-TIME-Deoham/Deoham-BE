package com.deoham.notification.service;

import com.deoham.card.entity.CardApply;
import com.deoham.chat.entity.ChatMessage;
import com.deoham.chat.entity.ChatMessageType;
import com.deoham.notification.entity.Notification;
import com.deoham.notification.entity.NotifyType;
import com.deoham.notification.repository.NotificationRepository;
import com.deoham.user.entity.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final int PREVIEW_MAX_LENGTH = 50;

    private final NotificationRepository notificationRepository;

    @Transactional
    public void notifyChatMessage(ChatMessage chatMessage, List<User> recipients) {
        String preview = buildPreviewText(chatMessage);
        recipients.forEach(recipient -> notificationRepository.save(Notification.builder()
                .user(recipient)
                .type(NotifyType.CHAT_MESSAGE)
                .referenceId(chatMessage.getId())
                .targetId(chatMessage.getChatRoom().getId())
                .message(preview)
                .build()));
    }

    @Transactional
    public void notifyCardApplied(CardApply apply) {
        String preview = apply.getApplicant().getNickname() + "님이 회원님의 카드에 지원했습니다";
        notificationRepository.save(Notification.builder()
                .user(apply.getCard().getRequester())
                .type(NotifyType.CARD_APPLIED)
                .referenceId(apply.getId())
                .targetId(apply.getCard().getId())
                .message(preview)
                .build());
    }

    @Transactional
    public void notifyMatchAccepted(CardApply apply) {
        String preview = apply.getCard().getRequester().getNickname() + "님과 매칭되었습니다";
        notificationRepository.save(Notification.builder()
                .user(apply.getApplicant())
                .type(NotifyType.MATCH_ACCEPTED)
                .referenceId(apply.getId())
                .targetId(apply.getCard().getId())
                .message(preview)
                .build());
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
