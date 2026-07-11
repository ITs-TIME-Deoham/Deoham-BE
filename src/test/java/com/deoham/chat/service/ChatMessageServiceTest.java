package com.deoham.chat.service;

import com.deoham.TestcontainersConfiguration;
import com.deoham.card.entity.Card;
import com.deoham.card.entity.CardApply;
import com.deoham.card.entity.CardCategory;
import com.deoham.card.entity.PreferredGender;
import com.deoham.card.repository.CardApplyRepository;
import com.deoham.card.repository.CardRepository;
import com.deoham.chat.dto.ChatMessagePageResponse;
import com.deoham.chat.dto.ChatMessageResponse;
import com.deoham.chat.dto.ChatMessageSendRequest;
import com.deoham.chat.dto.LocationPayload;
import com.deoham.chat.entity.ChatMessage;
import com.deoham.chat.entity.ChatMessageType;
import com.deoham.chat.entity.ChatRoom;
import com.deoham.chat.repository.ChatMessageRepository;
import com.deoham.chat.repository.ChatRoomRepository;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.notification.repository.NotificationRepository;
import com.deoham.user.entity.User;
import com.deoham.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ChatMessageServiceTest {

    private static final GeometryFactory GEO = new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired ChatMessageService chatMessageService;
    @Autowired UserRepository userRepository;
    @Autowired CardRepository cardRepository;
    @Autowired CardApplyRepository cardApplyRepository;
    @Autowired ChatRoomRepository chatRoomRepository;
    @Autowired ChatMessageRepository chatMessageRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired ObjectMapper objectMapper;

    private User requester;
    private User applicant;
    private User stranger;
    private ChatRoom room;

    @BeforeEach
    void setUp() {
        requester = savedUser("req@test.com", "msg-requester-uid", "요청자");
        applicant = savedUser("app@test.com", "msg-applicant-uid", "신청자");
        stranger  = savedUser("str@test.com", "msg-stranger-uid",  "제3자");

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

        room = chatRoomRepository.save(ChatRoom.builder().card(card).build());
    }

    // ───────────────────────────────────────────────────────────────────────────
    // sendMessage
    // ───────────────────────────────────────────────────────────────────────────

    @Test
    void sendMessage_savesTextMessage_andReturnsResponse() {
        ChatMessageResponse response = chatMessageService.sendMessage(
                room.getId(), requester.getId(),
                new ChatMessageSendRequest(ChatMessageType.TEXT, "안녕하세요", null));

        assertThat(response.chatRoomId()).isEqualTo(room.getId());
        assertThat(response.senderId()).isEqualTo(requester.getId());
        assertThat(response.messageType()).isEqualTo(ChatMessageType.TEXT);
        assertThat(response.content()).isEqualTo("안녕하세요");
        assertThat(response.sentAt()).isNotNull();
        assertThat(response.readAt()).isNull();
        assertThat(chatMessageRepository.findAll()).hasSize(1);
    }

    @Test
    void sendMessage_savesLocationMessage_asSerializedJson() throws Exception {
        LocationPayload location = new LocationPayload(37.5665, 126.9780, "시청역");

        ChatMessageResponse response = chatMessageService.sendMessage(
                room.getId(), applicant.getId(),
                new ChatMessageSendRequest(ChatMessageType.LOCATION, null, location));

        assertThat(response.messageType()).isEqualTo(ChatMessageType.LOCATION);
        LocationPayload roundTripped = objectMapper.readValue(response.content(), LocationPayload.class);
        assertThat(roundTripped).isEqualTo(location);
    }

    @Test
    void sendMessage_notifiesOtherParticipant() {
        chatMessageService.sendMessage(room.getId(), requester.getId(),
                new ChatMessageSendRequest(ChatMessageType.TEXT, "알림 테스트", null));

        assertThat(notificationRepository.findByUserOrderByCreatedAtDesc(applicant, org.springframework.data.domain.Pageable.unpaged()))
                .isNotEmpty();
    }

    @Test
    void sendMessage_throwsNotFound_whenRoomDoesNotExist() {
        assertThatThrownBy(() -> chatMessageService.sendMessage(
                UUID.randomUUID(), requester.getId(),
                new ChatMessageSendRequest(ChatMessageType.TEXT, "내용", null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void sendMessage_throwsForbidden_whenSenderNotParticipant() {
        assertThatThrownBy(() -> chatMessageService.sendMessage(
                room.getId(), stranger.getId(),
                new ChatMessageSendRequest(ChatMessageType.TEXT, "내용", null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void sendMessage_throwsInvalidRequest_whenRoomIsClosed() {
        room.close();
        chatRoomRepository.save(room);

        assertThatThrownBy(() -> chatMessageService.sendMessage(
                room.getId(), requester.getId(),
                new ChatMessageSendRequest(ChatMessageType.TEXT, "내용", null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void sendMessage_throwsInvalidRequest_whenTextContentIsBlank() {
        assertThatThrownBy(() -> chatMessageService.sendMessage(
                room.getId(), requester.getId(),
                new ChatMessageSendRequest(ChatMessageType.TEXT, "   ", null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void sendMessage_throwsInvalidRequest_whenLocationTypeMissingLocationField() {
        assertThatThrownBy(() -> chatMessageService.sendMessage(
                room.getId(), requester.getId(),
                new ChatMessageSendRequest(ChatMessageType.LOCATION, null, null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    // ───────────────────────────────────────────────────────────────────────────
    // markMessagesAsRead
    // ───────────────────────────────────────────────────────────────────────────

    @Test
    void markMessagesAsRead_marksUnreadMessagesFromOtherSender() {
        ChatMessage m1 = saveMessage(applicant, "메시지1");
        ChatMessage m2 = saveMessage(applicant, "메시지2");

        chatMessageService.markMessagesAsRead(room.getId(), requester.getId());

        assertThat(chatMessageRepository.findById(m1.getId()).orElseThrow().getReadAt()).isNotNull();
        assertThat(chatMessageRepository.findById(m2.getId()).orElseThrow().getReadAt()).isNotNull();
    }

    @Test
    void markMessagesAsRead_doesNotMarkOwnMessages() {
        ChatMessage own = saveMessage(requester, "내가 보낸 메시지");
        ChatMessage other = saveMessage(applicant, "상대가 보낸 메시지");

        chatMessageService.markMessagesAsRead(room.getId(), requester.getId());

        assertThat(chatMessageRepository.findById(own.getId()).orElseThrow().getReadAt()).isNull();
        assertThat(chatMessageRepository.findById(other.getId()).orElseThrow().getReadAt()).isNotNull();
    }

    @Test
    void markMessagesAsRead_doesNothing_whenNoUnreadMessages() {
        assertThatCode(() -> chatMessageService.markMessagesAsRead(room.getId(), requester.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    void markMessagesAsRead_throwsForbidden_whenNotParticipant() {
        assertThatThrownBy(() -> chatMessageService.markMessagesAsRead(room.getId(), stranger.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void markMessagesAsRead_throwsNotFound_whenRoomDoesNotExist() {
        assertThatThrownBy(() -> chatMessageService.markMessagesAsRead(UUID.randomUUID(), requester.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    // ───────────────────────────────────────────────────────────────────────────
    // getMessages
    // ───────────────────────────────────────────────────────────────────────────

    @Test
    void getMessages_returnsFirstPage_withHasNextTrue_whenMoreMessagesExist() throws InterruptedException {
        ChatMessage first = saveMessage(requester, "첫번째");
        Thread.sleep(5);
        ChatMessage second = saveMessage(applicant, "두번째");
        Thread.sleep(5);
        saveMessage(requester, "세번째");

        ChatMessagePageResponse page = chatMessageService.getMessages(room.getId(), requester.getId(), null, 2);

        assertThat(page.messages()).hasSize(2);
        assertThat(page.messages().get(0).content()).isEqualTo("세번째");
        assertThat(page.messages().get(1).content()).isEqualTo("두번째");
        assertThat(page.hasNext()).isTrue();
        assertThat(page.nextCursor()).isEqualTo(second.getSentAt());
    }

    @Test
    void getMessages_returnsRemainder_whenPagingWithCursor() throws InterruptedException {
        saveMessage(requester, "첫번째");
        Thread.sleep(5);
        saveMessage(applicant, "두번째");
        Thread.sleep(5);
        saveMessage(requester, "세번째");

        ChatMessagePageResponse firstPage = chatMessageService.getMessages(room.getId(), requester.getId(), null, 2);
        ChatMessagePageResponse secondPage = chatMessageService.getMessages(
                room.getId(), requester.getId(), firstPage.nextCursor(), 2);

        assertThat(secondPage.messages()).hasSize(1);
        assertThat(secondPage.messages().get(0).content()).isEqualTo("첫번째");
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(secondPage.nextCursor()).isNull();
    }

    @Test
    void getMessages_returnsEmptyPage_whenNoMessages() {
        ChatMessagePageResponse page = chatMessageService.getMessages(room.getId(), requester.getId(), null, 20);

        assertThat(page.messages()).isEmpty();
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void getMessages_throwsForbidden_whenNotParticipant() {
        assertThatThrownBy(() -> chatMessageService.getMessages(room.getId(), stranger.getId(), null, 20))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void getMessages_throwsNotFound_whenRoomDoesNotExist() {
        assertThatThrownBy(() -> chatMessageService.getMessages(UUID.randomUUID(), requester.getId(), null, 20))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 헬퍼
    // ───────────────────────────────────────────────────────────────────────────

    private ChatMessage saveMessage(User sender, String content) {
        return chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(content)
                .messageType(ChatMessageType.TEXT)
                .build());
    }

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
