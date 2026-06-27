package com.deoham.notification.service;

import com.deoham.chat.entity.ChatMessage;
import com.deoham.chat.entity.ChatMessageType;
import com.deoham.notification.entity.Notification;
import com.deoham.notification.entity.NotificationType;
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
                .type(NotificationType.CHAT_MESSAGE_RECEIVED)
                .chatMessage(chatMessage)
                .message(preview)
                .build()));
    }

    private String buildPreviewText(ChatMessage chatMessage) {
        String senderName = chatMessage.getSender().getName();
        if (chatMessage.getMessageType() == ChatMessageType.TEXT) {
            String content = chatMessage.getContent();
            String trimmed = content.length() > PREVIEW_MAX_LENGTH
                    ? content.substring(0, PREVIEW_MAX_LENGTH) + "..."
                    : content;
            return senderName + ": " + trimmed;
        }
        return senderName + "님이 " + (chatMessage.getMessageType() == ChatMessageType.IMAGE ? "사진을" : "파일을") + " 보냈습니다";
    }
}
