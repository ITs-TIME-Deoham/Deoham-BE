package com.deoham.notification.service;

import com.deoham.notification.entity.FcmToken;
import com.deoham.notification.event.FcmPushEvent;
import com.deoham.notification.repository.FcmTokenRepository;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * FCM 실제 발송기.
 *
 * <p>비즈니스 트랜잭션이 <b>커밋된 이후</b>({@link TransactionPhase#AFTER_COMMIT}) 별도 스레드에서 실행되어,
 * 롤백된 알림은 발송하지 않고 외부 I/O가 비즈니스 트랜잭션을 블로킹하지 않는다.
 * 발송 실패는 예외를 전파하지 않고 로깅만 한다(알림은 부가 기능이므로 본 흐름에 영향 주지 않음).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FcmSender {

    private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;
    private final FcmTokenRepository fcmTokenRepository;

    @Async("fcmTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FcmPushEvent event) {
        FirebaseMessaging messaging = firebaseMessagingProvider.getIfAvailable();
        if (messaging == null) {
            log.warn("FCM 비활성화 상태 - 푸시 스킵 [type={}, userId={}]", event.type(), event.recipientUserId());
            return;
        }

        List<FcmToken> tokens = fcmTokenRepository.findByUser_Id(event.recipientUserId());
        if (tokens.isEmpty()) {
            log.debug("등록된 FCM 토큰 없음 - 푸시 스킵 [userId={}]", event.recipientUserId());
            return;
        }

        List<String> tokenValues = tokens.stream().map(FcmToken::getToken).toList();
        try {
            BatchResponse response = messaging.sendEachForMulticast(buildMessage(event, tokenValues));
            if (response.getFailureCount() > 0) {
                cleanUpInvalidTokens(response, tokenValues);
            }
            log.info("FCM 발송 완료 [type={}, userId={}, success={}, failure={}]",
                    event.type(), event.recipientUserId(), response.getSuccessCount(), response.getFailureCount());
        } catch (Exception exception) {
            log.error("FCM 발송 실패 [type={}, userId={}]", event.type(), event.recipientUserId(), exception);
        }
    }

    private MulticastMessage buildMessage(FcmPushEvent event, List<String> tokenValues) {
        MulticastMessage.Builder builder = MulticastMessage.builder()
                .addAllTokens(tokenValues)
                .setNotification(Notification.builder()
                        .setTitle(event.title())
                        .setBody(event.body())
                        .build());
        if (event.data() != null && !event.data().isEmpty()) {
            builder.putAllData(event.data());
        }
        return builder.build();
    }

    private void cleanUpInvalidTokens(BatchResponse response, List<String> tokenValues) {
        List<SendResponse> responses = response.getResponses();
        List<String> invalidTokens = new ArrayList<>();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (sendResponse.isSuccessful()) {
                continue;
            }
            MessagingErrorCode errorCode = sendResponse.getException().getMessagingErrorCode();
            if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                invalidTokens.add(tokenValues.get(i));
            }
        }
        if (!invalidTokens.isEmpty()) {
            fcmTokenRepository.deleteByTokenIn(invalidTokens);
            log.info("무효 FCM 토큰 정리 [count={}]", invalidTokens.size());
        }
    }
}
