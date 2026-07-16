package com.deoham.chat.service;

import com.deoham.chat.dto.LiveLocationEvent;
import com.deoham.chat.dto.LiveLocationRequest;
import com.deoham.chat.service.ChatRoomAccessService.CardTargetLocation;
import com.deoham.global.util.GeoUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 채팅방 참여자 간 실시간 위치 공유의 핵심 로직.
 * 위치는 DB에 저장하지 않고 Redis에 짧은 TTL로만 캐시하며(늦게 입장한 상대의 즉시 표시용),
 * STOMP 토픽으로 브로드캐스트한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveLocationService {

    /** 참여자별 최소 브로드캐스트 간격. 이보다 촘촘히 들어온 갱신은 드롭한다. */
    private static final long MIN_INTERVAL_MS = 1_000L;
    /** 마지막 위치 캐시 TTL(공유 중단/이탈 후 잔여 데이터 자동 만료). */
    private static final Duration LOCATION_TTL = Duration.ofSeconds(30);
    private static final String KEY_PREFIX = "chat:live-loc:";

    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatRoomAccessService chatRoomAccessService;
    private final ObjectMapper objectMapper;

    /** roomId:userId -> 마지막 브로드캐스트 시각(epoch ms). 서버측 throttle. */
    private final ConcurrentHashMap<String, Long> lastBroadcastAt = new ConcurrentHashMap<>();

    /** roomId -> 카드 목표 지점 좌표. 카드 위치는 불변이므로 핑마다 DB 조회하지 않도록 캐시한다. */
    private final ConcurrentHashMap<UUID, CardTargetLocation> targetLocationCache = new ConcurrentHashMap<>();

    public void update(UUID roomId, UUID userId, LiveLocationRequest request) {
        chatRoomAccessService.verifySubscribeAccess(roomId, userId);

        long now = System.currentTimeMillis();
        // 인바운드 채널이 멀티스레드이므로 검사+갱신을 원자적으로 수행해 동시 프레임의 통과를 막는다.
        Long accepted = lastBroadcastAt.compute(throttleKey(roomId, userId), (k, previous) ->
                (previous != null && now - previous < MIN_INTERVAL_MS) ? previous : now);
        if (accepted != now) {
            log.debug("실시간 위치 throttle 드롭 [roomId={}, userId={}]", roomId, userId);
            return;
        }

        // throttle 통과 후에만 계산해 드롭 프레임에는 비용이 들지 않는다.
        CardTargetLocation target = targetLocationCache.computeIfAbsent(
                roomId, chatRoomAccessService::cardTargetLocation);
        double distance = GeoUtils.haversineMeters(
                request.latitude(), request.longitude(), target.latitude(), target.longitude());

        LiveLocationEvent event = LiveLocationEvent.update(userId, request, distance, Instant.ofEpochMilli(now));
        redisTemplate.opsForValue().set(redisKey(roomId, userId), event, LOCATION_TTL);
        messagingTemplate.convertAndSend(topic(roomId), event);
        log.debug("실시간 위치 브로드캐스트 [roomId={}, userId={}]", roomId, userId);
    }

    public void stop(UUID roomId, UUID userId) {
        redisTemplate.delete(redisKey(roomId, userId));
        lastBroadcastAt.remove(throttleKey(roomId, userId));
        targetLocationCache.remove(roomId);
        messagingTemplate.convertAndSend(topic(roomId), LiveLocationEvent.stop(userId, Instant.now()));
        log.info("실시간 위치 공유 종료 [roomId={}, userId={}]", roomId, userId);
    }

    /** 늦게 입장한 참여자를 위한 현재 스냅샷(참여자들의 마지막 위치). */
    public List<LiveLocationEvent> snapshot(UUID roomId, UUID userId) {
        chatRoomAccessService.verifySubscribeAccess(roomId, userId);
        List<LiveLocationEvent> result = new ArrayList<>();
        for (UUID participantId : chatRoomAccessService.participantIds(roomId)) {
            Object cached = redisTemplate.opsForValue().get(redisKey(roomId, participantId));
            if (cached != null) {
                // RedisTemplate 값 직렬화기가 타입 정보를 저장하지 않아 Map 으로 역직렬화될 수 있으므로 명시 변환.
                result.add(objectMapper.convertValue(cached, LiveLocationEvent.class));
            }
        }
        return result;
    }

    private static String topic(UUID roomId) {
        return "/sub/chat/rooms/" + roomId + "/live-location";
    }

    private static String redisKey(UUID roomId, UUID userId) {
        return KEY_PREFIX + roomId + ":" + userId;
    }

    private static String throttleKey(UUID roomId, UUID userId) {
        return roomId + ":" + userId;
    }
}
