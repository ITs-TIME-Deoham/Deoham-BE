package com.deoham.notification.controller;

import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.global.response.ApiResponse;
import com.deoham.global.security.SupabaseAuthenticationUtils;
import com.deoham.global.security.SupabasePrincipal;
import com.deoham.notification.controller.docs.NotificationControllerDocs;
import com.deoham.notification.dto.NotificationResponse;
import com.deoham.notification.service.NotificationReadService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController implements NotificationControllerDocs {

    private final NotificationReadService notificationReadService;

    @Override
    @GetMapping
    public ApiResponse<Page<NotificationResponse>> getNotifications(Pageable pageable) {
        return ApiResponse.ok(notificationReadService.getNotifications(currentUserId(), pageable));
    }

    @Override
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable UUID notificationId) {
        notificationReadService.markAsRead(currentUserId(), notificationId);
        return ApiResponse.ok();
    }

    @Override
    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllAsRead() {
        notificationReadService.markAllAsRead(currentUserId());
        return ApiResponse.ok();
    }

    private UUID currentUserId() {
        SupabasePrincipal principal = SupabaseAuthenticationUtils.currentPrincipal()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return principal.userId();
    }
}
