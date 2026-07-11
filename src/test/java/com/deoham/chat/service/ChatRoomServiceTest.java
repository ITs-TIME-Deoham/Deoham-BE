package com.deoham.chat.service;

import com.deoham.TestcontainersConfiguration;
import com.deoham.card.entity.Card;
import com.deoham.card.entity.CardApply;
import com.deoham.card.entity.CardCategory;
import com.deoham.card.entity.PreferredGender;
import com.deoham.card.repository.CardApplyRepository;
import com.deoham.card.repository.CardRepository;
import com.deoham.chat.dto.ChatRoomResponse;
import com.deoham.chat.entity.ChatMessage;
import com.deoham.chat.entity.ChatMessageType;
import com.deoham.chat.entity.ChatRoom;
import com.deoham.chat.entity.ChatRoomStatus;
import com.deoham.chat.repository.ChatMessageRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ChatRoomServiceTest {

    private static final GeometryFactory GEO = new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired ChatRoomService chatRoomService;
    @Autowired UserRepository userRepository;
    @Autowired CardRepository cardRepository;
    @Autowired CardApplyRepository cardApplyRepository;
    @Autowired ChatRoomRepository chatRoomRepository;
    @Autowired ChatMessageRepository chatMessageRepository;

    private User requester;
    private User applicant;
    private User stranger;
    private Card card;

    @BeforeEach
    void setUp() {
        requester = savedUser("req@test.com", "room-requester-uid", "요청자");
        applicant = savedUser("app@test.com", "room-applicant-uid", "신청자");
        stranger  = savedUser("str@test.com", "room-stranger-uid",  "제3자");

        card = savedCard(requester);

        CardApply apply = cardApplyRepository.save(CardApply.builder()
                .card(card)
                .applicant(applicant)
                .build());
        apply.accept();
    }

    // ───────────────────────────────────────────────────────────────────────────
    // getOrCreateRoom
    // ───────────────────────────────────────────────────────────────────────────

    @Test
    void getOrCreateRoom_createsNewRoom_whenNoneExists() {
        ChatRoomResponse response = chatRoomService.getOrCreateRoom(card.getId(), requester.getId());

        assertThat(response.cardId()).isEqualTo(card.getId());
        assertThat(response.status()).isEqualTo(ChatRoomStatus.ACTIVE);
        assertThat(response.unreadCount()).isZero();
        assertThat(chatRoomRepository.findAll()).hasSize(1);
    }

    @Test
    void getOrCreateRoom_returnsSameRoom_onSecondCall() {
        ChatRoomResponse first = chatRoomService.getOrCreateRoom(card.getId(), requester.getId());
        ChatRoomResponse second = chatRoomService.getOrCreateRoom(card.getId(), applicant.getId());

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(chatRoomRepository.findAll()).hasSize(1);
    }

    @Test
    void getOrCreateRoom_throwsForbidden_whenNotParticipant() {
        assertThatThrownBy(() -> chatRoomService.getOrCreateRoom(card.getId(), stranger.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void getOrCreateRoom_throwsNotFound_whenCardDoesNotExist() {
        assertThatThrownBy(() -> chatRoomService.getOrCreateRoom(UUID.randomUUID(), requester.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    // ───────────────────────────────────────────────────────────────────────────
    // getRoom
    // ───────────────────────────────────────────────────────────────────────────

    @Test
    void getRoom_returnsRoom_forParticipant() {
        ChatRoom room = chatRoomRepository.save(ChatRoom.builder().card(card).build());

        ChatRoomResponse response = chatRoomService.getRoom(room.getId(), requester.getId());

        assertThat(response.id()).isEqualTo(room.getId());
        assertThat(response.cardId()).isEqualTo(card.getId());
    }

    @Test
    void getRoom_reflectsUnreadCount_fromOtherParticipantOnly() {
        ChatRoom room = chatRoomRepository.save(ChatRoom.builder().card(card).build());
        saveMessage(room, applicant, "상대방 메시지 1");
        saveMessage(room, applicant, "상대방 메시지 2");
        saveMessage(room, requester, "내 메시지");

        ChatRoomResponse response = chatRoomService.getRoom(room.getId(), requester.getId());

        assertThat(response.unreadCount()).isEqualTo(2);
    }

    @Test
    void getRoom_throwsForbidden_whenNotParticipant() {
        ChatRoom room = chatRoomRepository.save(ChatRoom.builder().card(card).build());

        assertThatThrownBy(() -> chatRoomService.getRoom(room.getId(), stranger.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void getRoom_throwsNotFound_whenRoomDoesNotExist() {
        assertThatThrownBy(() -> chatRoomService.getRoom(UUID.randomUUID(), requester.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    // ───────────────────────────────────────────────────────────────────────────
    // getMyRooms
    // ───────────────────────────────────────────────────────────────────────────

    @Test
    void getMyRooms_excludesRoomsUserDoesNotParticipateIn() {
        ChatRoom myRoom = chatRoomRepository.save(ChatRoom.builder().card(card).build());

        User otherRequester = savedUser("other-req@test.com", "other-requester-uid", "다른요청자");
        Card otherCard = savedCard(otherRequester);
        chatRoomRepository.save(ChatRoom.builder().card(otherCard).build());

        Page<ChatRoomResponse> result = chatRoomService.getMyRooms(requester.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(ChatRoomResponse::id).containsExactly(myRoom.getId());
    }

    @Test
    void getMyRooms_includesCorrectUnreadCountPerRoom() {
        ChatRoom room1 = chatRoomRepository.save(ChatRoom.builder().card(card).build());
        saveMessage(room1, applicant, "room1 미읽음 1");
        saveMessage(room1, applicant, "room1 미읽음 2");

        User otherApplicant = savedUser("other-app@test.com", "other-applicant-uid", "다른신청자");
        Card card2 = savedCard(requester);
        CardApply apply2 = cardApplyRepository.save(CardApply.builder().card(card2).applicant(otherApplicant).build());
        apply2.accept();
        ChatRoom room2 = chatRoomRepository.save(ChatRoom.builder().card(card2).build());
        saveMessage(room2, otherApplicant, "room2 미읽음 1");

        Page<ChatRoomResponse> result = chatRoomService.getMyRooms(requester.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent())
                .filteredOn(r -> r.id().equals(room1.getId()))
                .extracting(ChatRoomResponse::unreadCount)
                .containsExactly(2L);
        assertThat(result.getContent())
                .filteredOn(r -> r.id().equals(room2.getId()))
                .extracting(ChatRoomResponse::unreadCount)
                .containsExactly(1L);
    }

    @Test
    void getMyRooms_paginatesResults() {
        for (int i = 0; i < 3; i++) {
            Card extraCard = savedCard(requester);
            chatRoomRepository.save(ChatRoom.builder().card(extraCard).build());
        }

        Page<ChatRoomResponse> page = chatRoomService.getMyRooms(requester.getId(), PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.hasNext()).isTrue();
    }

    // ───────────────────────────────────────────────────────────────────────────
    // closeRoom
    // ───────────────────────────────────────────────────────────────────────────

    @Test
    void closeRoom_marksRoomClosed() {
        ChatRoom room = chatRoomRepository.save(ChatRoom.builder().card(card).build());

        chatRoomService.closeRoom(room.getId(), requester.getId());

        ChatRoom reloaded = chatRoomRepository.findById(room.getId()).orElseThrow();
        assertThat(reloaded.getStatus().name()).isEqualTo("CLOSED");
        assertThat(reloaded.getClosedAt()).isNotNull();
    }

    @Test
    void closeRoom_throwsForbidden_whenNotParticipant() {
        ChatRoom room = chatRoomRepository.save(ChatRoom.builder().card(card).build());

        assertThatThrownBy(() -> chatRoomService.closeRoom(room.getId(), stranger.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void closeRoom_throwsNotFound_whenRoomDoesNotExist() {
        assertThatThrownBy(() -> chatRoomService.closeRoom(UUID.randomUUID(), requester.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 헬퍼
    // ───────────────────────────────────────────────────────────────────────────

    private void saveMessage(ChatRoom room, User sender, String content) {
        chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(content)
                .messageType(ChatMessageType.TEXT)
                .build());
    }

    private Card savedCard(User requester) {
        return cardRepository.save(Card.builder()
                .requester(requester)
                .category(CardCategory.OTHER)
                .description("설명")
                .location(point(126.9903, 37.5326))
                .city("서울특별시")
                .radiusM(500)
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .preferredGender(PreferredGender.ANY)
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
