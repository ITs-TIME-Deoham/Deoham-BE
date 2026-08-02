package com.deoham.notification.controller;

import com.deoham.global.metrics.MetricEndpoint;
import com.deoham.global.response.ApiResponse;
import com.deoham.global.security.AuthenticationUtils;
import com.deoham.notification.controller.docs.NotificationControllerDocs;
import com.deoham.notification.dto.FcmTokenRegisterRequest;
import com.deoham.notification.dto.NotificationResponse;
import com.deoham.notification.service.FcmTokenService;
import com.deoham.notification.service.NotificationReadService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController implements NotificationControllerDocs {

    private final NotificationReadService notificationReadService;
    private final FcmTokenService fcmTokenService;

    @Override
    @MetricEndpoint("notification.list")
    @GetMapping
    public ApiResponse<Page<NotificationResponse>> getNotifications(Pageable pageable) {
        return ApiResponse.ok(notificationReadService.getNotifications(AuthenticationUtils.requiredUserId(), pageable));
    }

    @Override
    @MetricEndpoint("notification.read")
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable UUID notificationId) {
        notificationReadService.markAsRead(AuthenticationUtils.requiredUserId(), notificationId);
        return ApiResponse.ok();
    }

    @Override
    @MetricEndpoint("notification.read_all")
    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllAsRead() {
        notificationReadService.markAllAsRead(AuthenticationUtils.requiredUserId());
        return ApiResponse.ok();
    }

    @Override
    @MetricEndpoint("notification.fcm.register")
    @PostMapping("/fcm-tokens")
    public ApiResponse<Void> registerFcmToken(@Valid @RequestBody FcmTokenRegisterRequest request) {
        fcmTokenService.register(AuthenticationUtils.requiredUserId(), request);
        return ApiResponse.ok();
    }

    @Override
    @MetricEndpoint("notification.fcm.delete")
    @DeleteMapping("/fcm-tokens")
    public ApiResponse<Void> deleteFcmToken(@RequestParam("token") String token) {
        fcmTokenService.delete(AuthenticationUtils.requiredUserId(), token);
        return ApiResponse.ok();
    }
}
