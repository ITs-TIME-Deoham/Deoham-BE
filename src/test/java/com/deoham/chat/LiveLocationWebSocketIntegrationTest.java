package com.deoham.chat;

import com.deoham.TestcontainersConfiguration;
import com.deoham.card.entity.Card;
import com.deoham.card.entity.CardApply;
import com.deoham.card.entity.CardCategory;
import com.deoham.card.entity.PreferredGender;
import com.deoham.card.repository.CardApplyRepository;
import com.deoham.card.repository.CardRepository;
import com.deoham.chat.dto.LiveLocationEvent;
import com.deoham.chat.dto.LiveLocationRequest;
import com.deoham.chat.entity.ChatRoom;
import com.deoham.chat.repository.ChatRoomRepository;
import com.deoham.global.security.JwtTokenProvider;
import com.deoham.global.util.GeoUtils;
import com.deoham.user.entity.User;
import com.deoham.user.repository.UserRepository;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LiveLocationWebSocketIntegrationTest {

    private static final String TEST_JWT_SECRET = "test-jwt-secret-key-for-websocket-integration-tests!!";
    private static final GeometryFactory GEO = new GeometryFactory(new PrecisionModel(), 4326);

    @DynamicPropertySource
    static void overrideJwtSecret(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> TEST_JWT_SECRET);
    }

    @LocalServerPort
    private int port;

    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private CardRepository cardRepository;
    @Autowired private CardApplyRepository cardApplyRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private TestRestTemplate restTemplate;
    @Autowired private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private WebSocketStompClient stompClient;
    private User requester;
    private User applicant;
    private Card card;
    private ChatRoom room;

    @BeforeEach
    void setUp() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper); // 서버와 동일한 직렬화 설정(Instant 등) 사용
        stompClient.setMessageConverter(converter);

        requester = userRepository.save(User.builder()
                .firebaseUid("live-requester-uid")
                .nickname("위치요청자")
                .build());
        applicant = userRepository.save(User.builder()
                .firebaseUid("live-applicant-uid")
                .nickname("위치신청자")
                .build());

        card = cardRepository.save(Card.builder()
                .requester(requester)
                .category(CardCategory.OTHER)
                .description("실시간 위치 통합 테스트용 카드")
                .location(point(126.9903, 37.5326))
                .city("서울특별시")
                .radiusM(500)
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .preferredGender(PreferredGender.ANY)
                .build());

        CardApply apply = CardApply.builder()
                .card(card)
                .applicant(applicant)
                .build();
        apply.accept();
        cardApplyRepository.save(apply); // ACCEPTED 상태를 DB에 반영(참여자 검증 통과용)

        room = chatRoomRepository.save(ChatRoom.builder().card(card).build());
    }

    @AfterEach
    void tearDown() {
        if (stompClient != null) {
            stompClient.stop();
        }
        chatRoomRepository.deleteAll();
        cardApplyRepository.deleteAll();
        cardRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("참여자가 보낸 위치 갱신을 구독 중인 상대가 UPDATE 이벤트로 실시간 수신한다")
    void updateBroadcast_deliversToSubscriber() throws Exception {
        StompSession subscriber = connect(requester);
        BlockingQueue<LiveLocationEvent> received = subscribeLiveLocation(subscriber);

        StompSession sender = connect(applicant);
        sender.send(pub(), new LiveLocationRequest(37.5665, 126.9780, 5.0));

        LiveLocationEvent event = received.poll(5, TimeUnit.SECONDS);
        assertThat(event).isNotNull();
        assertThat(event.type()).isEqualTo(LiveLocationEvent.Type.UPDATE);
        assertThat(event.senderId()).isEqualTo(applicant.getId());
        assertThat(event.latitude()).isEqualTo(37.5665);
        assertThat(event.longitude()).isEqualTo(126.9780);
        // 카드 목표 지점(37.5326, 126.9903)까지 남은 거리를 서버가 계산해 포함한다
        assertThat(event.distanceToTargetMeters())
                .isEqualTo(GeoUtils.haversineMeters(37.5665, 126.9780, 37.5326, 126.9903));

        subscriber.disconnect();
        sender.disconnect();
    }

    @Test
    @DisplayName("최소 간격 내에 연속 전송한 위치 갱신은 서버 throttle로 한 번만 브로드캐스트된다")
    void rapidUpdates_areThrottled() throws Exception {
        StompSession subscriber = connect(requester);
        BlockingQueue<LiveLocationEvent> received = subscribeLiveLocation(subscriber);

        StompSession sender = connect(applicant);
        sender.send(pub(), new LiveLocationRequest(37.1, 127.1, null));
        // 첫 프레임 수신을 확인한 직후(브로드캐스트 시점 기준 1s 이내) 두 번째를 보낸다.
        assertThat(received.poll(5, TimeUnit.SECONDS)).isNotNull();
        sender.send(pub(), new LiveLocationRequest(37.2, 127.2, null));
        // 두 번째 프레임은 최소 간격(1s) 내라 throttle로 드롭되어야 한다.
        assertThat(received.poll(1500, TimeUnit.MILLISECONDS)).isNull();

        subscriber.disconnect();
        sender.disconnect();
    }

    @Test
    @DisplayName("명시적 stop 전송 시 상대에게 STOP 이벤트가 브로드캐스트된다")
    void explicitStop_broadcastsStop() throws Exception {
        StompSession subscriber = connect(requester);
        BlockingQueue<LiveLocationEvent> received = subscribeLiveLocation(subscriber);

        StompSession sender = connect(applicant);
        sender.send("/pub/chat/rooms/" + room.getId() + "/live-location/stop", null);

        LiveLocationEvent event = received.poll(5, TimeUnit.SECONDS);
        assertThat(event).isNotNull();
        assertThat(event.type()).isEqualTo(LiveLocationEvent.Type.STOP);
        assertThat(event.senderId()).isEqualTo(applicant.getId());
        assertThat(event.distanceToTargetMeters()).isNull();

        subscriber.disconnect();
        sender.disconnect();
    }

    @Test
    @DisplayName("공유 중이던 세션이 끊기면 남은 상대에게 STOP 이벤트가 전송된다")
    void disconnect_broadcastsStopToPeer() throws Exception {
        StompSession subscriber = connect(requester);
        BlockingQueue<LiveLocationEvent> received = subscribeLiveLocation(subscriber);

        StompSession sender = connect(applicant);
        subscribeLiveLocation(sender); // 구독해야 presence에 등록되어 disconnect 시 STOP이 발생한다.
        sender.disconnect();

        LiveLocationEvent event = received.poll(5, TimeUnit.SECONDS);
        assertThat(event).isNotNull();
        assertThat(event.type()).isEqualTo(LiveLocationEvent.Type.STOP);
        assertThat(event.senderId()).isEqualTo(applicant.getId());

        subscriber.disconnect();
    }

    @Test
    @DisplayName("스냅샷 REST는 캐시된 상대의 마지막 위치를 반환한다(늦은 입장 대응)")
    void snapshot_returnsCachedLocation() throws Exception {
        StompSession subscriber = connect(requester);
        BlockingQueue<LiveLocationEvent> received = subscribeLiveLocation(subscriber);

        StompSession sender = connect(applicant);
        sender.send(pub(), new LiveLocationRequest(37.5665, 126.9780, null));
        assertThat(received.poll(5, TimeUnit.SECONDS)).isNotNull(); // 캐시 저장 완료 보장

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtTokenProvider.generateAccessToken(
                requester.getId(), requester.getId() + "@test.com", "USER"));
        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/chat/rooms/" + room.getId() + "/live-location",
                HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains(applicant.getId().toString());
        assertThat(response.getBody()).contains("UPDATE");
        // Redis 직렬화 왕복 후에도 남은 거리 필드가 유지된다
        assertThat(response.getBody()).contains("distanceToTargetMeters");

        subscriber.disconnect();
        sender.disconnect();
    }

    private BlockingQueue<LiveLocationEvent> subscribeLiveLocation(StompSession session) {
        BlockingQueue<LiveLocationEvent> queue = new LinkedBlockingQueue<>();
        session.subscribe("/sub/chat/rooms/" + room.getId() + "/live-location",
                new LiveLocationFrameHandler(queue));
        return queue;
    }

    private String pub() {
        return "/pub/chat/rooms/" + room.getId() + "/live-location";
    }

    private StompSession connect(User user) throws Exception {
        String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getId() + "@test.com", "USER");
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);
        return stompClient
                .connectAsync("ws://localhost:" + port + "/ws", (WebSocketHttpHeaders) null, connectHeaders,
                        new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
    }

    private Point point(double lng, double lat) {
        Point p = GEO.createPoint(new Coordinate(lng, lat));
        p.setSRID(4326);
        return p;
    }

    private record LiveLocationFrameHandler(BlockingQueue<LiveLocationEvent> queue) implements StompFrameHandler {
        @Override
        public Type getPayloadType(StompHeaders headers) {
            return LiveLocationEvent.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            queue.add((LiveLocationEvent) payload);
        }
    }
}
