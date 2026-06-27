package com.deoham.notification.controller.docs;

import com.deoham.global.response.ApiResponse;
import com.deoham.notification.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Tag(name = "알림", description = "알림 조회·읽음 처리 API")
public interface NotificationControllerDocs {

    @Operation(
            summary = "내 알림 목록 조회",
            description = """
                    현재 로그인한 사용자의 알림을 최신순으로 페이지네이션 조회합니다.

                    **알림 타입 (`type`)**
                    | 값 | 설명 |
                    |---|---|
                    | `CHAT_MESSAGE_RECEIVED` | 채팅 메시지 수신 |
                    | `CARD_CREATED` | 카드 생성 관련 알림 |
                    | `LINK_VIEWED` | 링크 열람 알림 |
                    | `COUNTERPART_CONFIRMED` | 상대방 확정 알림 |

                    **`referenceId`**: 알림과 연결된 리소스 ID입니다. `CHAT_MESSAGE_RECEIVED` 타입이면 메시지 UUID, 그 외 타입은 `null`일 수 있습니다.

                    **페이지네이션 파라미터 (Spring Pageable)**
                    | 파라미터 | 기본값 | 설명 |
                    |---|---|---|
                    | `page` | 0 | 0-indexed 페이지 번호 |
                    | `size` | 20 | 페이지당 항목 수 |

                    예시: `GET /api/notifications?page=0&size=20`
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 목록 조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": { "code": "UNAUTHORIZED", "message": "Authentication required" }
                                    }
                                    """
                    )
            )
    )
    ApiResponse<Page<NotificationResponse>> getNotifications(Pageable pageable);

    @Operation(
            summary = "알림 단건 읽음 처리",
            description = """
                    특정 알림을 읽음(`isRead = true`) 상태로 변경합니다.
                    본인의 알림만 읽음 처리할 수 있으며, 다른 사용자의 알림 ID를 요청하면 403을 반환합니다.
                    이미 읽은 알림에 대해 다시 요청해도 정상 응답(`200`)을 반환합니다.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "읽음 처리 성공 (data: null)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": { "code": "UNAUTHORIZED", "message": "Authentication required" }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "본인의 알림이 아님",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": { "code": "FORBIDDEN", "message": "Access denied" }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "알림을 찾을 수 없음",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": { "code": "NOT_FOUND", "message": "알림을 찾을 수 없습니다" }
                                    }
                                    """
                    )
            )
    )
    ApiResponse<Void> markAsRead(@Parameter(description = "읽음 처리할 알림 UUID") UUID notificationId);

    @Operation(
            summary = "알림 전체 읽음 처리",
            description = """
                    현재 로그인한 사용자의 읽지 않은 알림을 모두 읽음 상태로 변경합니다.
                    읽지 않은 알림이 없어도 정상 응답(`200`)을 반환합니다.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "전체 읽음 처리 성공 (data: null)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": { "code": "UNAUTHORIZED", "message": "Authentication required" }
                                    }
                                    """
                    )
            )
    )
    ApiResponse<Void> markAllAsRead();
}
