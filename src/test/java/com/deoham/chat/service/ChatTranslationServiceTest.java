package com.deoham.chat.service;

import com.deoham.TestcontainersConfiguration;
import com.deoham.chat.dto.ChatMessageSendRequest;
import com.deoham.chat.dto.ChatRoomCreateRequest;
import com.deoham.chat.dto.ChatRoomResponse;
import com.deoham.chat.dto.ChatTranslationResponse;
import com.deoham.chat.entity.ChatMessageType;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.user.entity.User;
import com.deoham.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ChatTranslationService 통합 테스트
 *
 * 실제 번역 공급자(TranslationProvider) 대신 DummyTranslationProvider가 @Primary로 등록되어
 * 외부 API 없이도 테스트가 동작합니다.
 *
 * 주요 검증 항목:
 * 1. 번역 요청 시 DummyProvider가 올바른 형식으로 번역 결과를 반환하는지
 * 2. 동일한 (messageId, targetLanguage) 조합으로 두 번 요청하면 캐시에서 반환하는지 (cached = true)
 * 3. 멤버가 아닌 사용자의 번역 요청이 차단되는지
 * 4. 첨부 파일 메시지는 번역할 수 없는지
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ChatTranslationServiceTest {

    @Autowired
    private ChatTranslationService chatTranslationService;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private UserRepository userRepository;

    private User member;
    private User stranger;  // 채팅방 외부인
    private ChatRoomResponse room;
    private UUID textMessageId;    // TEXT 타입 메시지 ID
    private UUID imageMessageId;   // IMAGE 타입 메시지 ID

    @BeforeEach
    void setUp() {
        member   = savedUser("member@test.com",   "Member");
        User peer = savedUser("peer@test.com",    "Peer");
        stranger = savedUser("stranger@test.com", "Stranger");

        // 테스트용 1:1 채팅방 생성
        room = chatRoomService.createRoom(
                member.getId(),
                new ChatRoomCreateRequest(null, List.of(peer.getId()), null));

        // 번역 대상 TEXT 메시지 전송
        textMessageId = chatMessageService.sendMessage(
                room.id(),
                member.getId(),
                new ChatMessageSendRequest(ChatMessageType.TEXT, "안녕하세요!", null, null, null, null)
        ).id();

        // 번역 불가 타입 검증용 IMAGE 메시지 전송
        imageMessageId = chatMessageService.sendMessage(
                room.id(),
                member.getId(),
                new ChatMessageSendRequest(
                        ChatMessageType.IMAGE, null,
                        "chat/room/photo.png", "photo.png", "image/png", null)
        ).id();
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 번역 기본 동작
    // ───────────────────────────────────────────────────────────────────────────

    /**
     * 텍스트 메시지를 처음 번역하면:
     * - translatedText에 번역 결과가 채워져 있어야 합니다.
     * - DummyTranslationProvider는 "[LANG] 원문" 형식으로 반환합니다.
     * - cached = false (새로 번역된 결과)
     */
    @Test
    void translate_returnsTranslatedText_fromDummyProvider() {
        ChatTranslationResponse response = chatTranslationService.translate(
                member.getId(), textMessageId, "en");

        assertThat(response.messageId()).isEqualTo(textMessageId);
        assertThat(response.targetLanguage()).isEqualTo("en");
        // DummyTranslationProvider 반환 형식: "[EN] 원문"
        assertThat(response.translatedText()).isEqualTo("[EN] 안녕하세요!");
        assertThat(response.cached()).isFalse();
        assertThat(response.translatedAt()).isNotNull();
    }

    /**
     * 동일한 (messageId, targetLanguage) 조합으로 두 번 번역을 요청하면
     * 두 번째 응답은 DB에서 캐시된 값을 반환해야 합니다 (cached = true).
     *
     * 이 동작이 보장되어야 실제 번역 API 비용을 중복 지불하지 않습니다.
     */
    @Test
    void translate_returnsCachedResult_onSecondRequest() {
        // 첫 번째 요청 — TranslationProvider.translate() 실제 호출
        ChatTranslationResponse first = chatTranslationService.translate(
                member.getId(), textMessageId, "en");

        // 두 번째 요청 — DB에서 캐시된 결과를 반환해야 함
        ChatTranslationResponse second = chatTranslationService.translate(
                member.getId(), textMessageId, "en");

        assertThat(first.cached()).isFalse();
        assertThat(second.cached()).isTrue();
        // 번역 내용은 동일해야 함
        assertThat(second.translatedText()).isEqualTo(first.translatedText());
    }

    /**
     * 서로 다른 언어는 별도로 캐시됩니다.
     * "en"으로 번역한 캐시가 있어도 "ja"로 처음 요청하면 cached = false여야 합니다.
     */
    @Test
    void translate_differentLanguages_cachedSeparately() {
        chatTranslationService.translate(member.getId(), textMessageId, "en");

        // 다른 언어는 별도 캐시 — 첫 번역이므로 cached = false
        ChatTranslationResponse jaResult = chatTranslationService.translate(
                member.getId(), textMessageId, "ja");

        assertThat(jaResult.cached()).isFalse();
        assertThat(jaResult.translatedText()).isEqualTo("[JA] 안녕하세요!");
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 권한 검증
    // ───────────────────────────────────────────────────────────────────────────

    /**
     * 채팅방 멤버가 아닌 사용자가 번역을 요청하면 FORBIDDEN 예외가 발생해야 합니다.
     */
    @Test
    void translate_throwsForbidden_whenRequesterIsNotMember() {
        assertThatThrownBy(() ->
                chatTranslationService.translate(stranger.getId(), textMessageId, "en"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    /**
     * 존재하지 않는 메시지 ID로 번역을 요청하면 NOT_FOUND 예외가 발생해야 합니다.
     */
    @Test
    void translate_throwsNotFound_whenMessageDoesNotExist() {
        UUID nonExistentId = UUID.randomUUID();

        assertThatThrownBy(() ->
                chatTranslationService.translate(member.getId(), nonExistentId, "en"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.NOT_FOUND));
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 메시지 타입 검증
    // ───────────────────────────────────────────────────────────────────────────

    /**
     * IMAGE나 FILE 같은 첨부 파일 메시지는 번역 대상이 아닙니다.
     * 번역을 시도하면 INVALID_REQUEST 예외가 발생해야 합니다.
     */
    @Test
    void translate_throwsInvalidRequest_whenMessageIsNotText() {
        assertThatThrownBy(() ->
                chatTranslationService.translate(member.getId(), imageMessageId, "en"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 헬퍼
    // ───────────────────────────────────────────────────────────────────────────

    private User savedUser(String email, String name) {
        return userRepository.save(User.builder().email(email).name(name).build());
    }
}
