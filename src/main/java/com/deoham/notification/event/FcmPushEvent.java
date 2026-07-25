package com.deoham.notification.event;

import com.deoham.notification.entity.NotifyType;
import java.util.Map;
import java.util.UUID;

/**
 * 커밋 후 FCM 발송을 위한 도메인 이벤트.
 *
 * <p>엔티티가 아니라 원시값만 담아 트랜잭션 커밋 이후(다른 스레드)에도 detached/lazy 로딩 문제가 없도록 한다.
 * {@code data}는 클릭 시 딥링크에 쓸 값(예: chatRoomId, cardId, referenceId)이다.
 */
public record FcmPushEvent(
        UUID recipientUserId,
        NotifyType type,
        String title,
        String body,
        Map<String, String> data
) {
}
