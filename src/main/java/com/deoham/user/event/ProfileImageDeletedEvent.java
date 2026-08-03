package com.deoham.user.event;

/**
 * 커밋 후 S3 프로필 이미지 삭제를 위한 도메인 이벤트.
 *
 * <p>엔티티가 아니라 URL 문자열만 담아 트랜잭션 커밋 이후(다른 스레드)에도 detached/lazy 로딩 문제가 없도록 한다.
 */
public record ProfileImageDeletedEvent(String imageUrl) {
}
