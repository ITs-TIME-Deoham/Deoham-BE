package com.deoham.chat;

import com.deoham.TestcontainersConfiguration;
import com.deoham.card.entity.Card;
import com.deoham.card.entity.CardApply;
import com.deoham.card.entity.CardCategory;
import com.deoham.card.entity.PreferredGender;
import com.deoham.card.repository.CardApplyRepository;
import com.deoham.card.repository.CardRepository;
import com.deoham.chat.dto.ChatMessageResponse;
import com.deoham.chat.dto.ChatMessageSendRequest;
import com.deoham.chat.entity.ChatMessageType;
import com.deoham.chat.entity.ChatRoom;
import com.deoham.chat.repository.ChatRoomRepository;
import com.deoham.global.security.JwtTokenProvider;
import com.deoham.user.entity.User;
import com.deoham.user.repository.UserRepository;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatWebSocketIntegrationTest {

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

    private WebSocketStompClient stompClient;
    private User requester;
    private User applicant;
    private Card card;
    private ChatRoom room;

    @BeforeEach
    void setUp() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        requester = userRepository.save(User.builder()
                .firebaseUid("ws-requester-uid")
                .nickname("웹소켓요청자")
                .build());
        applicant = userRepository.save(User.builder()
                .firebaseUid("ws-applicant-uid")
                .nickname("웹소켓신청자")
                .build());

        card = cardRepository.save(Card.builder()
                .requester(requester)
                .category(CardCategory.OTHER)
                .description("웹소켓 통합 테스트용 카드")
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
    @DisplayName("정상 JWT로 CONNECT하고 채팅방을 SUBSCRIBE하면, 다른 참여자가 보낸 메시지를 실시간으로 수신한다")
    void connectSubscribeAndSend_deliversMessageToSubscriber() throws Exception {
        StompSession subscriberSession = connect(requester);
        BlockingQueue<ChatMessageResponse> receivedMessages = new LinkedBlockingQueue<>();
        subscriberSession.subscribe("/sub/chat/rooms/" + room.getId(), new ChatMessageFrameHandler(receivedMessages));

        StompSession senderSession = connect(applicant);
        ChatMessageSendRequest request = new ChatMessageSendRequest(ChatMessageType.TEXT, "안녕하세요, 테스트입니다", null);
        senderSession.send("/pub/chat/rooms/" + room.getId() + "/messages", request);

        ChatMessageResponse received = receivedMessages.poll(5, TimeUnit.SECONDS);

        assertThat(received).isNotNull();
        assertThat(received.chatRoomId()).isEqualTo(room.getId());
        assertThat(received.senderId()).isEqualTo(applicant.getId());
        assertThat(received.content()).isEqualTo("안녕하세요, 테스트입니다");

        subscriberSession.disconnect();
        senderSession.disconnect();
    }

    @Test
    @DisplayName("Authorization 헤더 없이 CONNECT하면 서버가 연결을 거부한다")
    void connectWithoutAuthorizationHeader_isRejected() {
        StompHeaders connectHeaders = new StompHeaders();

        assertThatThrownBy(() -> connectRaw(connectHeaders, new StompSessionHandlerAdapter() {}))
                .isInstanceOf(ExecutionException.class);
    }

    @Test
    @DisplayName("서명이 잘못된 JWT로 CONNECT하면 서버가 연결을 거부한다")
    void connectWithInvalidJwt_isRejected() {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer invalid.jwt.token");

        assertThatThrownBy(() -> connectRaw(connectHeaders, new StompSessionHandlerAdapter() {}))
                .isInstanceOf(ExecutionException.class);
    }

    @Test
    @DisplayName("채팅방 참여자가 아닌 사용자가 SUBSCRIBE하면 서버가 구독을 거부하고 연결을 종료한다")
    void subscribeAsNonParticipant_isRejected() throws Exception {
        User stranger = userRepository.save(User.builder()
                .firebaseUid("ws-stranger-uid")
                .nickname("웹소켓제3자")
                .build());

        CompletableFuture<Throwable> errorFuture = new CompletableFuture<>();
        StompSession session = connect(stranger, new StompSessionHandlerAdapter() {
            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers,
                    byte[] payload, Throwable exception) {
                errorFuture.complete(exception);
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                errorFuture.complete(exception);
            }
        });

        session.subscribe("/sub/chat/rooms/" + room.getId(),
                new ChatMessageFrameHandler(new LinkedBlockingQueue<>()));

        Throwable error = errorFuture.get(5, TimeUnit.SECONDS);
        assertThat(error).isNotNull();
    }

    private StompSession connect(User user) throws Exception {
        return connect(user, new StompSessionHandlerAdapter() {});
    }

    private StompSession connect(User user, StompSessionHandlerAdapter handler) throws Exception {
        String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getId() + "@test.com", "USER");
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);
        return connectRaw(connectHeaders, handler);
    }

    private StompSession connectRaw(StompHeaders connectHeaders, StompSessionHandlerAdapter handler) throws Exception {
        return stompClient
                .connectAsync("ws://localhost:" + port + "/ws", (WebSocketHttpHeaders) null, connectHeaders, handler)
                .get(5, TimeUnit.SECONDS);
    }

    private Point point(double lng, double lat) {
        Point p = GEO.createPoint(new Coordinate(lng, lat));
        p.setSRID(4326);
        return p;
    }

    private record ChatMessageFrameHandler(BlockingQueue<ChatMessageResponse> queue) implements StompFrameHandler {
        @Override
        public Type getPayloadType(StompHeaders headers) {
            return ChatMessageResponse.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            queue.add((ChatMessageResponse) payload);
        }
    }
}
