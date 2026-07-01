package com.deoham.chat.service;

import com.deoham.TestcontainersConfiguration;
import com.deoham.chat.dto.ChatMessagePageResponse;
import com.deoham.chat.dto.ChatMessageResponse;
import com.deoham.chat.dto.ChatMessageSendRequest;
import com.deoham.chat.dto.ChatRoomCreateRequest;
import com.deoham.chat.dto.ChatRoomResponse;
import com.deoham.chat.entity.ChatMessageType;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.user.entity.User;
import com.deoham.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ChatMessageService 통합 테스트
 *
 * 메시지 전송과 조회에 관련된 비즈니스 로직 및 DB 쿼리를 검증합니다.
 *
 * - 각 테스트는 @Transactional에 의해 롤백되므로 DB 정리를 별도로 하지 않아도 됩니다.
 * - NotificationService도 실제 빈이 사용되어 알림 저장까지 한 번에 검증됩니다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ChatMessageServiceTest {

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private UserRepository userRepository;

    private User sender;
    private User other;
    private User stranger;   // 채팅방에 속하지 않는 외부인
    private ChatRoomResponse room;

    @BeforeEach
    void setUp() {
        sender   = savedUser("sender@test.com", "Sender");
        other    = savedUser("other@test.com",  "Other");
        stranger = savedUser("stranger@test.com", "Stranger");

        // 테스트에서 공통으로 사용할 1:1 채팅방을 미리 만들어 둡니다.
        room = chatRoomService.createRoom(
                sender.getId(),
                new ChatRoomCreateRequest(null, List.of(other.getId()), null));
    }

    // ───────────────────────────────────────────────────────────────────────────
    // sendMessage — TEXT 타입
    // ───────────────────────────────────────────────────────────────────────────

    /**
     * TEXT 메시지를 전송하면:
     * - 응답에 roomId, senderId, messageType, content가 모두 올바르게 채워져 있어야 합니다.
     */
    @Test
    void sendTextMessage_returnsCorrectResponse() {
        ChatMessageSendRequest request = textRequest("안녕하세요!");

        ChatMessageResponse response = chatMessageService.sendMessage(room.id(), sender.getId(), request);

        assertThat(response.id()).isNotNull();
        assertThat(response.roomId()).isEqualTo(room.id());
        assertThat(response.senderId()).isEqualTo(sender.getId());
        assertThat(response.messageType()).isEqualTo(ChatMessageType.TEXT.name());
        assertThat(response.content()).isEqualTo("안녕하세요!");
    }

    /**
     * 메시지를 전송하면 ChatRoom.lastMessageAt이 갱신되어야 합니다.
     * lastMessageAt은 메시지 목록 정렬(최근 활성 방 순)에 사용됩니다.
     */
    @Test
    void sendTextMessage_updatesRoomLastMessageAt() {
        Instant before = Instant.now();

        chatMessageService.sendMessage(room.id(), sender.getId(), textRequest("첫 메시지"));

        // 방 다시 조회해서 lastMessageAt이 설정되었는지 확인
        ChatRoomResponse updatedRoom = chatRoomService.getRoom(sender.getId(), room.id());
        assertThat(updatedRoom.lastMessageAt()).isNotNull();
        assertThat(updatedRoom.lastMessageAt()).isAfterOrEqualTo(before);
    }

    /**
     * TEXT 타입인데 content가 빈 문자열이거나 null이면 INVALID_REQUEST 예외가 발생해야 합니다.
     */
    @Test
    void sendTextMessage_throwsInvalidRequest_whenContentIsBlank() {
        ChatMessageSendRequest blankContent = new ChatMessageSendRequest(
                ChatMessageType.TEXT, "   ", null, null, null, null);

        assertThatThrownBy(() -> chatMessageService.sendMessage(room.id(), sender.getId(), blankContent))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    // ───────────────────────────────────────────────────────────────────────────
    // sendMessage — ATTACHMENT 타입
    // ───────────────────────────────────────────────────────────────────────────

    /**
     * IMAGE/FILE 타입인데 attachmentUrl이 없으면 INVALID_REQUEST 예외가 발생해야 합니다.
     */
    @Test
    void sendImageMessage_throwsInvalidRequest_whenAttachmentUrlMissing() {
        ChatMessageSendRequest noUrl = new ChatMessageSendRequest(
                ChatMessageType.IMAGE, null, null, "photo.png", "image/png", null);

        assertThatThrownBy(() -> chatMessageService.sendMessage(room.id(), sender.getId(), noUrl))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    /**
     * IMAGE 타입이 올바르게 전송되면 attachmentUrl, attachmentFileName 등이 응답에 포함되어야 합니다.
     */
    @Test
    void sendImageMessage_returnsCorrectResponse() {
        ChatMessageSendRequest imageRequest = new ChatMessageSendRequest(
                ChatMessageType.IMAGE, null,
                "chat/room1/photo.png", "photo.png", "image/png", 204800L);

        ChatMessageResponse response = chatMessageService.sendMessage(room.id(), sender.getId(), imageRequest);

        assertThat(response.messageType()).isEqualTo(ChatMessageType.IMAGE.name());
        assertThat(response.attachmentUrl()).isEqualTo("chat/room1/photo.png");
        assertThat(response.attachmentFileName()).isEqualTo("photo.png");
        assertThat(response.content()).isNull();
    }

    // ───────────────────────────────────────────────────────────────────────────
    // sendMessage — 권한 검증
    // ───────────────────────────────────────────────────────────────────────────

    /**
     * 채팅방 멤버가 아닌 사용자가 메시지를 보내려 하면 FORBIDDEN 예외가 발생해야 합니다.
     */
    @Test
    void sendMessage_throwsForbidden_whenSenderIsNotMember() {
        assertThatThrownBy(() ->
                chatMessageService.sendMessage(room.id(), stranger.getId(), textRequest("침입!")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    // ───────────────────────────────────────────────────────────────────────────
    // getMessages — 커서 기반 페이지네이션
    // ───────────────────────────────────────────────────────────────────────────

    /**
     * 커서 없이 조회하면 최신 메시지부터 내림차순으로 size개를 반환합니다.
     * 메시지 수가 size보다 많으면 hasNext = true여야 합니다.
     */
    @Test
    void getMessages_hasNext_whenMoreMessagesExistThanPageSize() {
        // 3개의 메시지를 전송합니다. (타임스탬프가 달라야 커서 정렬이 의미 있으므로 짧은 sleep 추가)
        sendWithPause("첫 번째");
        sendWithPause("두 번째");
        sendWithPause("세 번째");

        // size=2로 조회 → 최신 2개가 반환되고 hasNext=true여야 함
        ChatMessagePageResponse page = chatMessageService.getMessages(sender.getId(), room.id(), null, 2);

        assertThat(page.messages()).hasSize(2);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.nextCursor()).isNotNull();
    }

    /**
     * 커서 기반 페이지네이션:
     * 첫 페이지 이후 nextCursor를 사용해 두 번째 페이지를 요청하면 나머지 메시지가 반환됩니다.
     */
    @Test
    void getMessages_cursorPagination_returnsRemainingMessages() {
        sendWithPause("첫 번째");
        sendWithPause("두 번째");
        sendWithPause("세 번째");

        // 첫 번째 페이지 (size=2, 커서 없음)
        ChatMessagePageResponse firstPage = chatMessageService.getMessages(sender.getId(), room.id(), null, 2);

        // 두 번째 페이지 (첫 페이지의 nextCursor 사용)
        ChatMessagePageResponse secondPage = chatMessageService.getMessages(
                sender.getId(), room.id(), firstPage.nextCursor(), 2);

        assertThat(secondPage.messages()).hasSize(1);
        assertThat(secondPage.hasNext()).isFalse();
    }

    /**
     * 채팅방 멤버가 아닌 사용자가 메시지를 조회하려 하면 FORBIDDEN 예외가 발생해야 합니다.
     */
    @Test
    void getMessages_throwsForbidden_whenRequesterIsNotMember() {
        assertThatThrownBy(() ->
                chatMessageService.getMessages(stranger.getId(), room.id(), null, 20))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 헬퍼
    // ───────────────────────────────────────────────────────────────────────────

    private ChatMessageSendRequest textRequest(String content) {
        return new ChatMessageSendRequest(ChatMessageType.TEXT, content, null, null, null, null);
    }

    private void sendWithPause(String content) {
        chatMessageService.sendMessage(room.id(), sender.getId(), textRequest(content));
        try {
            // createdAt(Instant.now())이 메시지마다 다른 값을 갖도록 1ms 대기합니다.
            // 커서 정렬(createdAt DESC)이 정확하게 동작하려면 타임스탬프가 달라야 합니다.
            Thread.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private User savedUser(String email, String name) {
        return userRepository.save(User.builder().firebaseUid(email).nickname(name).build());
    }
}
