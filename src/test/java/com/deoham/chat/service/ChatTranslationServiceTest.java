package com.deoham.chat.service;

import com.deoham.TestcontainersConfiguration;
import com.deoham.card.entity.Card;
import com.deoham.card.entity.CardApply;
import com.deoham.card.entity.CardCategory;
import com.deoham.card.entity.PreferredGender;
import com.deoham.card.repository.CardApplyRepository;
import com.deoham.card.repository.CardRepository;
import com.deoham.chat.dto.ChatTranslationResponse;
import com.deoham.chat.entity.ChatMessage;
import com.deoham.chat.entity.ChatMessageType;
import com.deoham.chat.entity.ChatRoom;
import com.deoham.chat.repository.ChatMessageRepository;
import com.deoham.chat.repository.ChatMessageTranslationRepository;
import com.deoham.chat.repository.ChatRoomRepository;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.user.entity.User;
import com.deoham.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ChatTranslationServiceTest {

    private static final GeometryFactory GEO = new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired ChatTranslationService chatTranslationService;
    @Autowired UserRepository userRepository;
    @Autowired CardRepository cardRepository;
    @Autowired CardApplyRepository cardApplyRepository;
    @Autowired ChatRoomRepository chatRoomRepository;
    @Autowired ChatMessageRepository chatMessageRepository;
    @Autowired ChatMessageTranslationRepository chatMessageTranslationRepository;

    private User requester;
    private User applicant;
    private User stranger;
    private ChatMessage textMessage;

    @BeforeEach
    void setUp() {
        requester = savedUser("req@test.com", "trans-requester-uid", "요청자");
        applicant = savedUser("app@test.com", "trans-applicant-uid", "신청자");
        stranger  = savedUser("str@test.com", "trans-stranger-uid",  "제3자");

        Card card = cardRepository.save(Card.builder()
                .requester(requester)
                .category(CardCategory.OTHER)
                .description("설명")
                .location(point(126.9903, 37.5326))
                .city("서울특별시")
                .radiusM(500)
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .preferredGender(PreferredGender.ANY)
                .build());

        CardApply apply = cardApplyRepository.save(CardApply.builder()
                .card(card)
                .applicant(applicant)
                .build());
        apply.accept();

        ChatRoom room = chatRoomRepository.save(ChatRoom.builder().card(card).build());

        textMessage = chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room)
                .sender(requester)
                .content("안녕하세요")
                .messageType(ChatMessageType.TEXT)
                .build());
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 정상 번역
    // ───────────────────────────────────────────────────────────────────────────

    @Test
    void translate_returnsFreshTranslation_onFirstRequest() {
        ChatTranslationResponse response = chatTranslationService.translate(requester.getId(), textMessage.getId(), "en");

        assertThat(response.messageId()).isEqualTo(textMessage.getId());
        assertThat(response.targetLanguage()).isEqualTo("en");
        assertThat(response.translatedText()).isEqualTo("[EN] 안녕하세요");
        assertThat(response.cached()).isFalse();
        assertThat(chatMessageTranslationRepository.findAll()).hasSize(1);
    }

    @Test
    void translate_returnsCachedTranslation_onSecondRequest() {
        chatTranslationService.translate(requester.getId(), textMessage.getId(), "en");

        ChatTranslationResponse second = chatTranslationService.translate(applicant.getId(), textMessage.getId(), "en");

        assertThat(second.cached()).isTrue();
        assertThat(second.translatedText()).isEqualTo("[EN] 안녕하세요");
        assertThat(chatMessageTranslationRepository.findAll()).hasSize(1);
    }

    @Test
    void translate_createsSeparateCacheEntries_perTargetLanguage() {
        chatTranslationService.translate(requester.getId(), textMessage.getId(), "en");
        chatTranslationService.translate(requester.getId(), textMessage.getId(), "ja");

        assertThat(chatMessageTranslationRepository.findAll()).hasSize(2);
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 예외
    // ───────────────────────────────────────────────────────────────────────────

    @Test
    void translate_throwsForbidden_whenRequesterNotParticipant() {
        assertThatThrownBy(() -> chatTranslationService.translate(stranger.getId(), textMessage.getId(), "en"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void translate_throwsNotFound_whenMessageDoesNotExist() {
        assertThatThrownBy(() -> chatTranslationService.translate(requester.getId(), UUID.randomUUID(), "en"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void translate_throwsInvalidRequest_whenMessageIsNotText() {
        ChatMessage locationMessage = chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(textMessage.getChatRoom())
                .sender(requester)
                .content("{\"latitude\":37.5,\"longitude\":127.0}")
                .messageType(ChatMessageType.LOCATION)
                .build());

        assertThatThrownBy(() -> chatTranslationService.translate(requester.getId(), locationMessage.getId(), "en"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 헬퍼
    // ───────────────────────────────────────────────────────────────────────────

    private User savedUser(String email, String firebaseUid, String nickname) {
        return userRepository.save(User.builder()
                .firebaseUid(firebaseUid)
                .nickname(nickname)
                .build());
    }

    private Point point(double lng, double lat) {
        Point p = GEO.createPoint(new Coordinate(lng, lat));
        p.setSRID(4326);
        return p;
    }
}
