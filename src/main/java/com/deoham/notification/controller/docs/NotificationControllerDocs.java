package com.deoham.notification.controller.docs;

import com.deoham.global.response.ApiResponse;
import com.deoham.notification.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Tag(name = "Notification", description = "Notification read APIs")
public interface NotificationControllerDocs {

    @Operation(
            summary = "Get my notifications",
            description = """
                    Returns notifications for the current user.

                    Notification type values match the `notify_type` database enum:
                    `NEW_CARD`, `CARD_APPLIED`, `MATCH_ACCEPTED`, `MATCH_REJECTED`, `CHAT_MESSAGE`.

                    `referenceId` is the linked resource ID. For `CHAT_MESSAGE`, it can be the message UUID.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notifications returned")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    ApiResponse<Page<NotificationResponse>> getNotifications(Pageable pageable);

    @Operation(
            summary = "Mark notification as read",
            description = "Marks one notification owned by the current user as read."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Marked as read")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the notification owner")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Notification not found")
    ApiResponse<Void> markAsRead(@Parameter(description = "Notification UUID") UUID notificationId);

    @Operation(
            summary = "Mark all notifications as read",
            description = "Marks every unread notification owned by the current user as read."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Marked as read")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    ApiResponse<Void> markAllAsRead();
}
