package com.deoham.user.event;

import com.deoham.global.config.S3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * S3 프로필 이미지 실제 삭제기.
 *
 * <p>비즈니스 트랜잭션이 <b>커밋된 이후</b>({@link TransactionPhase#AFTER_COMMIT}) 별도 스레드에서 실행된다.
 * 커밋 전에 지우면 이후 롤백 시 DB에는 URL이 남고 실물 파일만 사라져 복구가 불가능하므로 커밋 이후로 미루고,
 * S3 네트워크 왕복이 DB 커넥션을 점유하지 않도록 전용 스레드 풀에서 처리한다.
 * 삭제 실패는 예외를 전파하지 않고 로깅만 한다(이미지 정리는 부가 작업이므로 본 흐름에 영향 주지 않음).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileImageDeletionListener {

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    /**
     * {@code fallbackExecution = true}는 트랜잭션 없이 발행된 경우에도 삭제가 유실되지 않도록 하는 방어책이다.
     * (현재 호출 경로는 모두 트랜잭션 안이므로 평상시에는 AFTER_COMMIT으로만 동작한다.)
     */
    @Async("s3TaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(ProfileImageDeletedEvent event) {
        String imageUrl = event.imageUrl();
        String key = extractS3Key(imageUrl);
        if (key == null || key.isBlank()) {
            log.warn("S3 키 추출 실패 - 프로필 이미지 삭제 스킵 [url={}]", imageUrl);
            return;
        }

        try {
            s3Client.deleteObject(builder -> builder
                    .bucket(s3Properties.bucket())
                    .key(key)
                    .build());
            log.debug("프로필 이미지 삭제 완료 [bucket={}, key={}]", s3Properties.bucket(), key);
        } catch (Exception exception) {
            log.error("프로필 이미지 삭제 실패 [bucket={}, key={}]", s3Properties.bucket(), key, exception);
        }
    }

    private String extractS3Key(String imageUrl) {
        String prefix = "https://" + s3Properties.bucket() + ".s3." + s3Properties.region() + ".amazonaws.com/";
        if (imageUrl != null && imageUrl.startsWith(prefix)) {
            return imageUrl.substring(prefix.length());
        }
        return null;
    }
}
