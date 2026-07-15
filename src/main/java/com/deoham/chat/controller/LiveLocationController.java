package com.deoham.chat.controller;

import com.deoham.chat.controller.docs.LiveLocationControllerDocs;
import com.deoham.chat.dto.LiveLocationEvent;
import com.deoham.chat.service.LiveLocationService;
import com.deoham.global.response.ApiResponse;
import com.deoham.global.security.AuthenticationUtils;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 실시간 위치 공유의 현재 스냅샷 조회 REST 엔드포인트.
 * 채팅 화면 진입 시(늦은 입장 포함) 상대의 마지막 위치를 즉시 표시하는 데 사용한다.
 * Swagger 문서는 {@link LiveLocationControllerDocs} 로 분리한다.
 */
@RestController
@RequestMapping("/api/chat/rooms/{roomId}")
@RequiredArgsConstructor
public class LiveLocationController implements LiveLocationControllerDocs {

    private final LiveLocationService liveLocationService;

    @Override
    @GetMapping("/live-location")
    public ApiResponse<List<LiveLocationEvent>> getSnapshot(@PathVariable UUID roomId) {
        return ApiResponse.ok(liveLocationService.snapshot(roomId, AuthenticationUtils.requiredUserId()));
    }
}
