package com.deoham.chat.service;

import com.deoham.TestcontainersConfiguration;
import com.deoham.card.entity.Card;
import com.deoham.card.entity.CardApply;
import com.deoham.card.entity.CardCategory;
import com.deoham.card.entity.PreferredGender;
import com.deoham.card.repository.CardApplyRepository;
import com.deoham.card.repository.CardRepository;
import com.deoham.chat.dto.ChatRoomLocationResponse;
import com.deoham.chat.entity.ChatRoom;
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
import static org.assertj.core.api.Assertions.within;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ChatRoomLocationTest {

    private static final double TEST_LAT = 37.5326;
    private static final double TEST_LNG = 126.9903;
    private static final String TEST_CITY = "서울특별시";

    private static final GeometryFactory GEO = new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired ChatRoomService chatRoomService;
    @Autowired UserRepository userRepository;
    @Autowired CardRepository cardRepository;
    @Autowired CardApplyRepository cardApplyRepository;
    @Autowired ChatRoomRepository chatRoomRepository;

    private User requester;
    private User applicant;
    private User stranger;
    private ChatRoom room;

    @BeforeEach
    void setUp() {
        requester = savedUser("req@test.com", "requester-uid", "요청자");
        applicant = savedUser("app@test.com", "applicant-uid", "신청자");
        stranger  = savedUser("str@test.com", "stranger-uid",  "제3자");

        Card card = cardRepository.save(Card.builder()
                .requester(requester)
                .category(CardCategory.OTHER)
                .title("테스트 카드")
                .description("설명")
                .location(point(TEST_LNG, TEST_LAT))
                .city(TEST_CITY)
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
    // 정상 조회
    // ───────────────────────────────────────────────────────────────────────────

    @Test
    void getCardLocation_returnsCorrectCoordinates_forRequester() {
        ChatRoomLocationResponse result = chatRoomService.getCardLocation(room.getId(), requester.getId());

        assertThat(result.latitude()).isCloseTo(TEST_LAT, within(0.0001));
        assertThat(result.longitude()).isCloseTo(TEST_LNG, within(0.0001));
        assertThat(result.city()).isEqualTo(TEST_CITY);
    }

    @Test
    void getCardLocation_returnsCorrectCoordinates_forAcceptedApplicant() {
        ChatRoomLocationResponse result = chatRoomService.getCardLocation(room.getId(), applicant.getId());

        assertThat(result.latitude()).isCloseTo(TEST_LAT, within(0.0001));
        assertThat(result.longitude()).isCloseTo(TEST_LNG, within(0.0001));
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 예외
    // ───────────────────────────────────────────────────────────────────────────

    @Test
    void getCardLocation_throwsForbidden_whenNotParticipant() {
        assertThatThrownBy(() -> chatRoomService.getCardLocation(room.getId(), stranger.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void getCardLocation_throwsNotFound_whenRoomDoesNotExist() {
        UUID nonExistentRoomId = UUID.randomUUID();

        assertThatThrownBy(() -> chatRoomService.getCardLocation(nonExistentRoomId, requester.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.NOT_FOUND));
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
        // JTS Coordinate: x = 경도(longitude), y = 위도(latitude)
        Point p = GEO.createPoint(new Coordinate(lng, lat));
        p.setSRID(4326);
        return p;
    }
}
