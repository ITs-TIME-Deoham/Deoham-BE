package com.deoham.chat.controller;

import com.deoham.chat.dto.ChatTranslationRequest;
import com.deoham.chat.dto.ChatTranslationResponse;
import com.deoham.chat.service.ChatTranslationService;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.global.response.ApiResponse;
import com.deoham.global.security.SupabaseAuthenticationUtils;
import com.deoham.global.security.SupabasePrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "채팅 번역", description = "채팅 메시지 번역 API")
@RestController
@RequestMapping("/api/chat/messages/{messageId}/translations")
@RequiredArgsConstructor
public class ChatTranslationController {

    private final ChatTranslationService chatTranslationService;

    @Operation(
            summary = "메시지 번역",
            description = """
                    특정 채팅 메시지를 지정한 언어로 번역합니다.

                    **캐싱**: 동일한 `(messageId, targetLanguage)` 조합은 DB에 캐시되어, 이후 요청은 번역 제공자를 재호출하지 않고 즉시 반환합니다.
                    응답의 `cached` 필드로 캐시 적중 여부를 확인할 수 있습니다.

                    **번역 대상**: `TEXT` 타입 메시지만 번역 가능합니다. `IMAGE`, `FILE` 타입 메시지에 요청하면 400을 반환합니다.

                    **접근 권한**: 해당 메시지가 속한 채팅방의 활성 멤버만 번역을 요청할 수 있습니다.

                    **`targetLanguage` 값**: 번역 제공자가 지원하는 언어 코드를 사용합니다 (예: `ko`, `en`, `ja`, `zh`).
                    현재 번역 제공자는 설정에 따라 다를 수 있으며, 지원 언어 코드는 제공자 문서를 참고하세요.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "번역 성공 (캐시 적중 또는 신규 번역)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "번역 불가 — `TEXT` 타입이 아닌 메시지이거나, `targetLanguage`가 비어 있음",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": { "code": "INVALID_REQUEST", "message": "텍스트 메시지만 번역할 수 있습니다" }
                                    }
                                    """
                    )
            )
    )
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
            description = "메시지가 속한 채팅방의 멤버가 아님",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": { "code": "FORBIDDEN", "message": "채팅방 멤버가 아닙니다" }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "`messageId`에 해당하는 메시지를 찾을 수 없음",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": { "code": "NOT_FOUND", "message": "메시지를 찾을 수 없습니다" }
                                    }
                                    """
                    )
            )
    )
    @PostMapping
    public ApiResponse<ChatTranslationResponse> translate(
            @Parameter(description = "번역할 메시지 UUID") @PathVariable UUID messageId,
            @Valid @RequestBody ChatTranslationRequest request) {
        SupabasePrincipal principal = SupabaseAuthenticationUtils.currentPrincipal()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return ApiResponse.ok(chatTranslationService.translate(principal.userId(), messageId, request.targetLanguage()));
    }
}
