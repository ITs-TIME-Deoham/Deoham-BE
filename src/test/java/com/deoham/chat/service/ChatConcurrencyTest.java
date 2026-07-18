package com.deoham.chat.service;

import com.deoham.TestcontainersConfiguration;
import com.deoham.card.entity.Card;
import com.deoham.card.entity.CardApply;
import com.deoham.card.entity.CardCategory;
import com.deoham.card.entity.PreferredGender;
import com.deoham.card.repository.CardApplyRepository;
import com.deoham.card.repository.CardRepository;
import com.deoham.chat.dto.ChatMessageSendRequest;
import com.deoham.chat.entity.ChatMessage;
import com.deoham.chat.entity.ChatMessageType;
import com.deoham.chat.entity.ChatRoom;
import com.deoham.chat.repository.ChatMessageRepository;
import com.deoham.chat.repository.ChatRoomRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ChatConcurrencyTest {

    private static final GeometryFactory GEO = new GeometryFactory(new PrecisionModel(), 4326);
    private static final int THREAD_COUNT = 5;

    @Autowired ChatMessageService chatMessageService;
    @Autowired ChatRoomService chatRoomService;
    @Autowired UserRepository userRepository;
    @Autowired CardRepository cardRepository;
    @Autowired CardApplyRepository cardApplyRepository;
    @Autowired ChatRoomRepository chatRoomRepository;
    @Autowired ChatMessageRepository chatMessageRepository;

    private User requester;
    private User applicant;
    private List<User> multipleUsers;
    private Card card;
    private ChatRoom room;

    @BeforeEach
    void setUp() {
        requester = savedUser("req@test.com", "concurrency-req-uid", "요청자");
        applicant = savedUser("app@test.com", "concurrency-app-uid", "신청자");

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

        CardApply apply = cardApplyRepository.save(CardApply.builder()
                .card(card)
                .applicant(applicant)
                .build());
        apply.accept();

        room = chatRoomRepository.save(ChatRoom.builder().card(card).build());

        multipleUsers = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            multipleUsers.add(savedUser(
                    "user" + i + "@test.com",
                    "concurrency-user-" + i + "-uid",
                    "사용자" + i));
        }
    }

    // ───────────────────────────────────────────────────────────────────────────
    //  읽음 표시 동시성 : 10개 메시지를 5개 스레드가 동시에 읽음 처리
    // ───────────────────────────────────────────────────────────────────────────

    @Test
    void markMessagesAsRead_concurrent_multipleThreadsMarkDifferentMessages() throws InterruptedException {
        int messageCount = 10;
        List<ChatMessage> unreadMessages = new ArrayList<>();
        for (int i = 0; i < messageCount; i++) {
            unreadMessages.add(chatMessageRepository.save(ChatMessage.builder()
                    .chatRoom(room)
                    .sender(applicant)
                    .content("메시지" + i)
                    .messageType(ChatMessageType.TEXT)
                    .build()));
        }

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    chatMessageService.markMessagesAsRead(room.getId(), requester.getId());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        List<ChatMessage> allMessages = chatMessageRepository.findByChatRoomIdOrderBySentAtDesc(
                room.getId(), PageRequest.of(0, 100));
        long readCount = allMessages.stream().filter(m -> m.getReadAt() != null).count();

        assertThat(readCount).isEqualTo(messageCount);
        assertThat(allMessages).noneMatch(m -> m.getReadAt() == null);
    }

    @Test
    void markMessagesAsRead_concurrent_idempotent() throws InterruptedException {
        ChatMessage msg1 = chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room)
                .sender(applicant)
                .content("메시지1")
                .messageType(ChatMessageType.TEXT)
                .build());
        ChatMessage msg2 = chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room)
                .sender(applicant)
                .content("메시지2")
                .messageType(ChatMessageType.TEXT)
                .build());

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    barrier.await();
                    chatMessageService.markMessagesAsRead(room.getId(), requester.getId());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        ChatMessage rereadMsg1 = chatMessageRepository.findById(msg1.getId()).orElseThrow();
        ChatMessage rereadMsg2 = chatMessageRepository.findById(msg2.getId()).orElseThrow();

        assertThat(rereadMsg1.getReadAt()).isNotNull();
        assertThat(rereadMsg2.getReadAt()).isNotNull();
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 메시지 전송 동시성 : requester/applicant가 번갈아 메시지 전송, CyclicBarrier로 정확히 동일 시점에 전송
    // ───────────────────────────────────────────────────────────────────────────

    @Test
    void sendMessage_concurrent_multipleSendersInSequence() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    User sender = index % 2 == 0 ? requester : applicant;
                    chatMessageService.sendMessage(
                            room.getId(),
                            sender.getId(),
                            new ChatMessageSendRequest(ChatMessageType.TEXT, "메시지" + index, null));
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        List<ChatMessage> allMessages = chatMessageRepository.findByChatRoomIdOrderBySentAtDesc(
                room.getId(), PageRequest.of(0, 100));

        assertThat(successCount.get()).isEqualTo(THREAD_COUNT);
        assertThat(allMessages).hasSize(THREAD_COUNT);
        assertThat(allMessages).allMatch(m -> m.getContent().contains("메시지"));
    }

    @Test
    void sendMessage_concurrent_withBarrierToExactlySimultaneous() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    barrier.await();
                    User sender = index % 2 == 0 ? requester : applicant;
                    chatMessageService.sendMessage(
                            room.getId(),
                            sender.getId(),
                            new ChatMessageSendRequest(ChatMessageType.TEXT, "동시메시지" + index, null));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        List<ChatMessage> allMessages = chatMessageRepository.findByChatRoomIdOrderBySentAtDesc(
                room.getId(), PageRequest.of(0, 100));

        assertThat(successCount.get()).isEqualTo(THREAD_COUNT);
        assertThat(allMessages).hasSize(THREAD_COUNT);
        assertThat(allMessages)
                .allMatch(m -> m.getReadAt() == null)
                .allMatch(m -> m.getMessageType() == ChatMessageType.TEXT);
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 채팅방 폐쇄 동시성 : 폐쇄 작업이 멱등성(idempotent) 만족하는지 검증
    // ───────────────────────────────────────────────────────────────────────────

    @Test
    void closeRoom_concurrent_multipleThreadsAttemptClose() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    User actor = index % 2 == 0 ? requester : applicant;
                    chatRoomService.closeRoom(room.getId(), actor.getId());
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        ChatRoom closedRoom = chatRoomRepository.findById(room.getId()).orElseThrow();

        assertThat(closedRoom.getStatus().name()).isEqualTo("CLOSED");
        assertThat(closedRoom.getClosedAt()).isNotNull();
        assertThat(exceptionCount.get()).isZero();
    }

    @Test
    void closeRoom_concurrent_idempotent() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    barrier.await();
                    chatRoomService.closeRoom(room.getId(), requester.getId());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        ChatRoom closedRoom = chatRoomRepository.findById(room.getId()).orElseThrow();

        assertThat(closedRoom.getStatus().name()).isEqualTo("CLOSED");
        assertThat(closedRoom.getClosedAt()).isNotNull();
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 혼합 시나리오 : 15개 메시지 전송 → 2개 스레드가 읽음 → 채팅방 폐쇄
    // ───────────────────────────────────────────────────────────────────────────

    @Test
    void mixedOperations_concurrent_sendMarkAndClose() throws InterruptedException {
        int messagesPerThread = 3;
        int totalMessages = THREAD_COUNT * messagesPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch sendLatch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    User sender = index % 2 == 0 ? requester : applicant;
                    for (int j = 0; j < messagesPerThread; j++) {
                        chatMessageService.sendMessage(
                                room.getId(),
                                sender.getId(),
                                new ChatMessageSendRequest(
                                        ChatMessageType.TEXT,
                                        "메시지-" + index + "-" + j,
                                        null));
                    }
                } finally {
                    sendLatch.countDown();
                }
            });
        }

        sendLatch.await();

        int markThreads = 2;
        ExecutorService markExecutor = Executors.newFixedThreadPool(markThreads);
        CountDownLatch markLatch = new CountDownLatch(markThreads);

        markExecutor.submit(() -> {
            try {
                chatMessageService.markMessagesAsRead(room.getId(), requester.getId());
            } finally {
                markLatch.countDown();
            }
        });

        markExecutor.submit(() -> {
            try {
                chatMessageService.markMessagesAsRead(room.getId(), applicant.getId());
            } finally {
                markLatch.countDown();
            }
        });

        markLatch.await();
        markExecutor.shutdown();

        chatRoomService.closeRoom(room.getId(), requester.getId());

        List<ChatMessage> allMessages = chatMessageRepository.findByChatRoomIdOrderBySentAtDesc(
                room.getId(), PageRequest.of(0, 1000));
        assertThat(allMessages).hasSize(totalMessages);
        assertThat(chatRoomRepository.findById(room.getId()).orElseThrow().getStatus().name()).isEqualTo("CLOSED");

        executor.shutdown();
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
