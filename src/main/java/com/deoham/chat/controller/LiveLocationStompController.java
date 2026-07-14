package com.deoham.chat.controller;

import com.deoham.chat.dto.LiveLocationRequest;
import com.deoham.chat.service.LiveLocationService;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.global.security.AuthPrincipal;
import com.deoham.global.security.AuthenticationUtils;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

/**
 * 실시간 위치 공유 STOMP 엔드포인트.
 * 발신은 {@code /pub/**} 로만 허용되며(StompAuthChannelInterceptor), 방별 참여자 검증은
 * {@link LiveLocationService} 에서 수행한다.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class LiveLocationStompController {

    private final LiveLocationService liveLocationService;

    @MessageMapping("/chat/rooms/{roomId}/live-location")
    public void updateLocation(@DestinationVariable UUID roomId,
                               @Payload @Valid LiveLocationRequest request,
                               Principal principal) {
        liveLocationService.update(roomId, resolveUserId(principal), request);
    }

    @MessageMapping("/chat/rooms/{roomId}/live-location/stop")
    public void stopLocation(@DestinationVariable UUID roomId, Principal principal) {
        liveLocationService.stop(roomId, resolveUserId(principal));
    }

    @MessageExceptionHandler(BusinessException.class)
    public void handleBusinessException(BusinessException ex) {
        log.warn("실시간 위치 STOMP BusinessException [{}]: {}", ex.getErrorCode().name(), ex.getMessage());
    }

    @MessageExceptionHandler(Exception.class)
    public void handleUnexpectedException(Exception ex) {
        log.error("실시간 위치 STOMP 처리 중 예기치 않은 예외 발생", ex);
    }

    private UUID resolveUserId(Principal principal) {
        if (!(principal instanceof Authentication authentication)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        AuthPrincipal authPrincipal = AuthenticationUtils.fromAuthentication(authentication)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return authPrincipal.userId();
    }
}
