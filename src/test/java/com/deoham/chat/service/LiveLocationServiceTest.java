package com.deoham.chat.service;

import com.deoham.chat.dto.LiveLocationEvent;
import com.deoham.chat.dto.LiveLocationRequest;
import com.deoham.chat.service.ChatRoomAccessService.CardTargetLocation;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.global.util.GeoUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LiveLocationService 테스트")
class LiveLocationServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private ChatRoomAccessService chatRoomAccessService;

    @Captor private ArgumentCaptor<LiveLocationEvent> eventCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private LiveLocationService liveLocationService;

    private UUID roomId;
    private UUID userId;

    /** 카드 목표 지점(강남역). update() 시 남은 거리 계산에 사용된다. */
    private static final CardTargetLocation TARGET = new CardTargetLocation(37.4979, 127.0276);

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        liveLocationService = new LiveLocationService(messagingTemplate, redisTemplate, chatRoomAccessService, objectMapper);
        roomId = UUID.randomUUID();
        userId = UUID.randomUUID();
        lenient().when(chatRoomAccessService.cardTargetLocation(roomId)).thenReturn(TARGET);
    }

    private String cacheKey(UUID roomId, UUID userId) {
        return "chat:live-loc:" + roomId + ":" + userId;
    }

    private String topic(UUID roomId) {
        return "/sub/chat/rooms/" + roomId + "/live-location";
    }

    // ───────────────────────────────────────────────────────────────────────────
    // update()
    // ───────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("최초 위치 갱신 시 참여자 검증 후 Redis 캐시와 STOMP 브로드캐스트를 수행한다")
    void update_cachesAndBroadcasts_onFirstUpdate() {
        LiveLocationRequest request = new LiveLocationRequest(37.5665, 126.9780, 5.0);

        liveLocationService.update(roomId, userId, request);

        verify(chatRoomAccessService).verifySubscribeAccess(roomId, userId);
        verify(valueOperations).set(eq(cacheKey(roomId, userId)), any(LiveLocationEvent.class), eq(Duration.ofSeconds(30)));
        verify(messagingTemplate).convertAndSend(eq(topic(roomId)), eventCaptor.capture());

        LiveLocationEvent event = eventCaptor.getValue();
        assertThat(event.type()).isEqualTo(LiveLocationEvent.Type.UPDATE);
        assertThat(event.senderId()).isEqualTo(userId);
        assertThat(event.latitude()).isEqualTo(37.5665);
        assertThat(event.longitude()).isEqualTo(126.9780);
        assertThat(event.accuracy()).isEqualTo(5.0);
        assertThat(event.distanceToTargetMeters())
                .isEqualTo(GeoUtils.haversineMeters(37.5665, 126.9780, TARGET.latitude(), TARGET.longitude()));
    }

    @Test
    @DisplayName("카드 목표 좌표는 방 단위로 캐시되어 같은 방의 후속 갱신에서 재조회하지 않는다")
    void update_cachesCardTargetLocation_perRoom() {
        UUID otherUserId = UUID.randomUUID();
        LiveLocationRequest request = new LiveLocationRequest(37.0, 127.0, null);

        // 서로 다른 사용자라 throttle에 걸리지 않고 둘 다 브로드캐스트된다
        liveLocationService.update(roomId, userId, request);
        liveLocationService.update(roomId, otherUserId, request);

        verify(chatRoomAccessService, times(1)).cardTargetLocation(roomId);
    }

    @Test
    @DisplayName("공유 종료 시 목표 좌표 캐시를 비우고 다음 갱신에서 재조회한다")
    void stop_evictsTargetLocationCache() {
        LiveLocationRequest request = new LiveLocationRequest(37.0, 127.0, null);

        liveLocationService.update(roomId, userId, request);
        liveLocationService.stop(roomId, userId);
        liveLocationService.update(roomId, userId, request);

        verify(chatRoomAccessService, times(2)).cardTargetLocation(roomId);
    }

    @Test
    @DisplayName("참여자가 아니면 예외를 던지고 캐시/브로드캐스트를 수행하지 않는다")
    void update_throwsAndSkipsSideEffects_whenNotParticipant() {
        LiveLocationRequest request = new LiveLocationRequest(37.5665, 126.9780, null);
        doThrow(new BusinessException(ErrorCode.FORBIDDEN, "채팅방 참여자가 아닙니다"))
                .when(chatRoomAccessService).verifySubscribeAccess(roomId, userId);

        assertThatThrownBy(() -> liveLocationService.update(roomId, userId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);

        verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("1초 이내 연속 갱신은 두 번째 요청부터 드롭한다")
    void update_dropsSubsequentUpdate_withinThrottleWindow() {
        LiveLocationRequest first = new LiveLocationRequest(37.0, 127.0, null);
        LiveLocationRequest second = new LiveLocationRequest(38.0, 128.0, null);

        liveLocationService.update(roomId, userId, first);
        liveLocationService.update(roomId, userId, second);

        // 참여자 검증은 throttle 여부와 무관하게 매 호출마다 수행된다
        verify(chatRoomAccessService, times(2)).verifySubscribeAccess(roomId, userId);
        verify(valueOperations, times(1)).set(eq(cacheKey(roomId, userId)), any(LiveLocationEvent.class), eq(Duration.ofSeconds(30)));
        verify(messagingTemplate, times(1)).convertAndSend(eq(topic(roomId)), any(LiveLocationEvent.class));
    }

    @Test
    @DisplayName("서로 다른 사용자의 갱신은 서로의 throttle에 영향을 주지 않는다")
    void update_throttlesPerUserIndependently() {
        UUID otherUserId = UUID.randomUUID();
        LiveLocationRequest request = new LiveLocationRequest(37.0, 127.0, null);

        liveLocationService.update(roomId, userId, request);
        liveLocationService.update(roomId, otherUserId, request);

        verify(valueOperations).set(eq(cacheKey(roomId, userId)), any(LiveLocationEvent.class), eq(Duration.ofSeconds(30)));
        verify(valueOperations).set(eq(cacheKey(roomId, otherUserId)), any(LiveLocationEvent.class), eq(Duration.ofSeconds(30)));
        verify(messagingTemplate, times(2)).convertAndSend(eq(topic(roomId)), any(LiveLocationEvent.class));
    }

    // ───────────────────────────────────────────────────────────────────────────
    // stop()
    // ───────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("공유 종료 시 Redis 캐시를 삭제하고 STOP 이벤트를 브로드캐스트한다")
    void stop_deletesCacheAndBroadcastsStopEvent() {
        liveLocationService.stop(roomId, userId);

        verify(redisTemplate).delete(cacheKey(roomId, userId));
        verify(messagingTemplate).convertAndSend(eq(topic(roomId)), eventCaptor.capture());

        LiveLocationEvent event = eventCaptor.getValue();
        assertThat(event.type()).isEqualTo(LiveLocationEvent.Type.STOP);
        assertThat(event.senderId()).isEqualTo(userId);
        assertThat(event.latitude()).isNull();
        assertThat(event.longitude()).isNull();
        assertThat(event.accuracy()).isNull();
        assertThat(event.distanceToTargetMeters()).isNull();
    }

    @Test
    @DisplayName("공유 종료 후에는 throttle 윈도우 이내라도 즉시 다음 갱신이 허용된다")
    void stop_resetsThrottle_allowingImmediateNextUpdate() {
        LiveLocationRequest request = new LiveLocationRequest(37.0, 127.0, null);

        liveLocationService.update(roomId, userId, request);
        liveLocationService.stop(roomId, userId);
        liveLocationService.update(roomId, userId, request);

        // update, update 각각 캐시 저장 성공 (stop 은 delete 만 수행)
        verify(valueOperations, times(2)).set(eq(cacheKey(roomId, userId)), any(LiveLocationEvent.class), eq(Duration.ofSeconds(30)));
        // update -> stop -> update 순서로 총 3건 브로드캐스트
        verify(messagingTemplate, times(3)).convertAndSend(eq(topic(roomId)), any(LiveLocationEvent.class));
    }

    // ───────────────────────────────────────────────────────────────────────────
    // snapshot()
    // ───────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("참여자 중 위치 캐시가 남아있는 사람만 스냅샷에 포함한다")
    void snapshot_returnsOnlyParticipantsWithCachedLocation() {
        UUID requester = UUID.randomUUID();
        UUID withLocation = UUID.randomUUID();
        UUID withoutLocation = UUID.randomUUID();

        when(chatRoomAccessService.participantIds(roomId))
                .thenReturn(List.of(withLocation, withoutLocation));

        LiveLocationEvent cachedEvent = LiveLocationEvent.update(
                withLocation, new LiveLocationRequest(37.1, 127.1, null), 100.0, Instant.now());

        when(valueOperations.get(cacheKey(roomId, withLocation))).thenReturn(cachedEvent);
        when(valueOperations.get(cacheKey(roomId, withoutLocation))).thenReturn(null);

        List<LiveLocationEvent> result = liveLocationService.snapshot(roomId, requester);

        verify(chatRoomAccessService).verifySubscribeAccess(roomId, requester);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).senderId()).isEqualTo(withLocation);
    }

    @Test
    @DisplayName("아무도 위치를 공유하고 있지 않으면 빈 목록을 반환한다")
    void snapshot_returnsEmptyList_whenNoOneSharing() {
        when(chatRoomAccessService.participantIds(roomId)).thenReturn(List.of(userId));
        when(valueOperations.get(cacheKey(roomId, userId))).thenReturn(null);

        List<LiveLocationEvent> result = liveLocationService.snapshot(roomId, userId);

        assertThat(result).isEmpty();
    }
}
