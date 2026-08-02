package com.deoham.notification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.deoham.notification.entity.FcmToken;
import com.deoham.notification.entity.NotifyType;
import com.deoham.notification.entity.Platform;
import com.deoham.notification.event.FcmPushEvent;
import com.deoham.notification.repository.FcmTokenRepository;
import com.deoham.user.entity.User;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmSender 발송 테스트")
class FcmSenderTest {

    @Mock private ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;
    @Mock private FcmTokenRepository fcmTokenRepository;
    @Mock private FirebaseMessaging firebaseMessaging;

    private FcmSender fcmSender;

    @BeforeEach
    void setUp() {
        fcmSender = new FcmSender(firebaseMessagingProvider, fcmTokenRepository);
    }

    private FcmPushEvent event(UUID userId) {
        return new FcmPushEvent(userId, NotifyType.CHAT_MESSAGE, "제목", "내용",
                Map.of("type", NotifyType.CHAT_MESSAGE.name()));
    }

    @Test
    @DisplayName("FirebaseMessaging 빈이 없으면(키 부재) 발송을 스킵한다")
    void handle_messagingAbsent_skips() {
        // Given
        when(firebaseMessagingProvider.getIfAvailable()).thenReturn(null);

        // When
        fcmSender.handle(event(UUID.randomUUID()));

        // Then
        verifyNoInteractions(fcmTokenRepository);
    }

    @Test
    @DisplayName("UNREGISTERED 응답을 받은 토큰만 DB에서 삭제한다")
    void handle_invalidToken_cleanedUp() throws FirebaseMessagingException {
        // Given
        UUID userId = UUID.randomUUID();
        User owner = org.mockito.Mockito.mock(User.class);
        FcmToken t1 = FcmToken.builder().user(owner).token("t1").deviceId("d1").platform(Platform.ANDROID).build();
        FcmToken t2 = FcmToken.builder().user(owner).token("t2").deviceId("d2").platform(Platform.IOS).build();

        when(firebaseMessagingProvider.getIfAvailable()).thenReturn(firebaseMessaging);
        when(fcmTokenRepository.findByUser_Id(userId)).thenReturn(List.of(t1, t2));

        SendResponse ok = org.mockito.Mockito.mock(SendResponse.class);
        when(ok.isSuccessful()).thenReturn(true);
        SendResponse failed = org.mockito.Mockito.mock(SendResponse.class);
        when(failed.isSuccessful()).thenReturn(false);
        FirebaseMessagingException exception = org.mockito.Mockito.mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        when(failed.getException()).thenReturn(exception);

        BatchResponse batchResponse = org.mockito.Mockito.mock(BatchResponse.class);
        when(batchResponse.getFailureCount()).thenReturn(1);
        when(batchResponse.getResponses()).thenReturn(List.of(ok, failed));
        when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

        // When
        fcmSender.handle(event(userId));

        // Then
        verify(fcmTokenRepository).deleteByTokenIn(eq(List.of("t2")));
    }

    @Test
    @DisplayName("등록된 토큰이 없으면 발송을 시도하지 않는다")
    void handle_noTokens_skips() throws FirebaseMessagingException {
        // Given
        UUID userId = UUID.randomUUID();
        when(firebaseMessagingProvider.getIfAvailable()).thenReturn(firebaseMessaging);
        when(fcmTokenRepository.findByUser_Id(userId)).thenReturn(List.of());

        // When
        fcmSender.handle(event(userId));

        // Then
        verify(firebaseMessaging, never()).sendEachForMulticast(any(MulticastMessage.class));
    }
}
