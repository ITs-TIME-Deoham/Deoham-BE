package com.deoham.notification.service;

import com.deoham.TestcontainersConfiguration;
import com.deoham.card.entity.Card;
import com.deoham.card.entity.CardApply;
import com.deoham.card.entity.CardCategory;
import com.deoham.card.entity.PreferredGender;
import com.deoham.card.repository.CardApplyRepository;
import com.deoham.card.repository.CardRepository;
import com.deoham.chat.entity.ChatMessage;
import com.deoham.chat.entity.ChatMessageType;
import com.deoham.chat.entity.ChatRoom;
import com.deoham.chat.repository.ChatMessageRepository;
import com.deoham.chat.repository.ChatRoomRepository;
import com.deoham.notification.dto.NotificationResponse;
import com.deoham.notification.entity.Notification;
import com.deoham.notification.entity.NotifyType;
import com.deoham.notification.repository.NotificationRepository;
import com.deoham.user.entity.User;
import com.deoham.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class NotificationServiceTest {

    private static final GeometryFactory GEO = new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired NotificationService notificationService;
    @Autowired NotificationRepository notificationRepository;
    @Autowired UserRepository userRepository;
    @Autowired CardRepository cardRepository;
    @Autowired CardApplyRepository cardApplyRepository;
    @Autowired ChatRoomRepository chatRoomRepository;
    @Autowired ChatMessageRepository chatMessageRepository;

    private User requester;
    private User applicant;
    private Card card;
    private CardApply apply;
    private ChatRoom room;
    private ChatMessage message;

    @BeforeEach
    void setUp() {
        requester = savedUser("noti-requester-uid", "요청자");
        applicant = savedUser("noti-applicant-uid", "신청자");

        card = cardRepository.save(Card.builder()
                .requester(requester)
                .category(CardCategory.OTHER)
                .description("설명")
                .location(point(126.9903, 37.5326))
                .city("서울특별시")
                .radiusM(500)
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .preferredGender(PreferredGender.ANY)
                .build());

        apply = cardApplyRepository.save(CardApply.builder()
                .card(card)
                .applicant(applicant)
                .build());

        room = chatRoomRepository.save(ChatRoom.builder().card(card).build());

        message = chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room)
                .sender(requester)
                .content("안녕하세요")
                .messageType(ChatMessageType.TEXT)
                .build());
    }

    @Test
    void notifyCardApplied_setsTargetIdToCardId() {
        notificationService.notifyCardApplied(apply);

        Notification saved = notificationRepository.findByUserOrderByCreatedAtDesc(requester, Pageable.unpaged()).getContent().get(0);
        assertThat(saved.getType()).isEqualTo(NotifyType.CARD_APPLIED);
        assertThat(saved.getReferenceId()).isEqualTo(apply.getId());
        assertThat(saved.getTargetId()).isEqualTo(card.getId());
    }

    @Test
    void notifyMatchAccepted_setsTargetIdToCardId() {
        notificationService.notifyMatchAccepted(apply);

        Notification saved = notificationRepository.findByUserOrderByCreatedAtDesc(applicant, Pageable.unpaged()).getContent().get(0);
        assertThat(saved.getType()).isEqualTo(NotifyType.MATCH_ACCEPTED);
        assertThat(saved.getReferenceId()).isEqualTo(apply.getId());
        assertThat(saved.getTargetId()).isEqualTo(card.getId());
    }

    @Test
    void notifyChatMessage_setsTargetIdToChatRoomId() {
        notificationService.notifyChatMessage(message, List.of(applicant));

        Notification saved = notificationRepository.findByUserOrderByCreatedAtDesc(applicant, Pageable.unpaged()).getContent().get(0);
        assertThat(saved.getType()).isEqualTo(NotifyType.CHAT_MESSAGE);
        assertThat(saved.getReferenceId()).isEqualTo(message.getId());
        assertThat(saved.getTargetId()).isEqualTo(room.getId());
    }

    @Test
    void notificationResponse_includesTargetId() {
        notificationService.notifyCardApplied(apply);
        Notification saved = notificationRepository.findByUserOrderByCreatedAtDesc(requester, Pageable.unpaged()).getContent().get(0);

        NotificationResponse response = NotificationResponse.from(saved);

        assertThat(response.targetId()).isEqualTo(card.getId());
    }

    private User savedUser(String firebaseUid, String nickname) {
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
