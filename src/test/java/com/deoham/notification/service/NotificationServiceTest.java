package com.deoham.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deoham.card.entity.CardApply;
import com.deoham.card.entity.Card;
import com.deoham.chat.entity.ChatMessage;
import com.deoham.chat.entity.ChatMessageType;
import com.deoham.chat.entity.ChatRoom;
import com.deoham.notification.entity.Notification;
import com.deoham.notification.entity.NotifyType;
import com.deoham.notification.event.FcmPushEvent;
import com.deoham.notification.repository.NotificationRepository;
import com.deoham.user.entity.User;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService 이벤트 발행 테스트")
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, eventPublisher);
    }

    @Test
    @DisplayName("카드 지원 시 알림 저장과 함께 FcmPushEvent를 발행한다")
    void notifyCardApplied_savesAndPublishes() {
        // Given
        UUID requesterId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        UUID applyId = UUID.randomUUID();

        User requester = org.mockito.Mockito.mock(User.class);
        when(requester.getId()).thenReturn(requesterId);
        User applicant = org.mockito.Mockito.mock(User.class);
        when(applicant.getNickname()).thenReturn("지원자");
        Card card = org.mockito.Mockito.mock(Card.class);
        when(card.getRequester()).thenReturn(requester);
        when(card.getId()).thenReturn(cardId);
        CardApply apply = org.mockito.Mockito.mock(CardApply.class);
        when(apply.getCard()).thenReturn(card);
        when(apply.getApplicant()).thenReturn(applicant);
        when(apply.getId()).thenReturn(applyId);

        // When
        notificationService.notifyCardApplied(apply);

        // Then
        verify(notificationRepository).save(any(Notification.class));
        ArgumentCaptor<FcmPushEvent> captor = ArgumentCaptor.forClass(FcmPushEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        FcmPushEvent published = captor.getValue();
        assertThat(published.recipientUserId()).isEqualTo(requesterId);
        assertThat(published.type()).isEqualTo(NotifyType.CARD_APPLIED);
        assertThat(published.data()).containsEntry("cardId", cardId.toString());
    }

    @Test
    @DisplayName("채팅 메시지 시 수신자별로 알림 저장과 FcmPushEvent를 발행한다")
    void notifyChatMessage_savesAndPublishesPerRecipient() {
        // Given
        UUID roomId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();

        User sender = org.mockito.Mockito.mock(User.class);
        when(sender.getNickname()).thenReturn("보낸이");
        ChatRoom room = org.mockito.Mockito.mock(ChatRoom.class);
        when(room.getId()).thenReturn(roomId);
        ChatMessage message = org.mockito.Mockito.mock(ChatMessage.class);
        when(message.getChatRoom()).thenReturn(room);
        when(message.getId()).thenReturn(messageId);
        when(message.getSender()).thenReturn(sender);
        when(message.getMessageType()).thenReturn(ChatMessageType.TEXT);
        when(message.getContent()).thenReturn("안녕하세요");

        User recipient = org.mockito.Mockito.mock(User.class);
        when(recipient.getId()).thenReturn(recipientId);

        // When
        notificationService.notifyChatMessage(message, List.of(recipient));

        // Then
        verify(notificationRepository).save(any(Notification.class));
        ArgumentCaptor<FcmPushEvent> captor = ArgumentCaptor.forClass(FcmPushEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        FcmPushEvent published = captor.getValue();
        assertThat(published.recipientUserId()).isEqualTo(recipientId);
        assertThat(published.type()).isEqualTo(NotifyType.CHAT_MESSAGE);
        assertThat(published.data()).containsEntry("chatRoomId", roomId.toString());
    }
}
