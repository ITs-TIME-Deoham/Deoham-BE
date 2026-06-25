package com.deoham.chat.controller;

import com.deoham.chat.dto.ChatTranslationRequest;
import com.deoham.chat.dto.ChatTranslationResponse;
import com.deoham.chat.service.ChatTranslationService;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.global.response.ApiResponse;
import com.deoham.global.security.SupabaseAuthenticationUtils;
import com.deoham.global.security.SupabasePrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat/messages/{messageId}/translations")
@RequiredArgsConstructor
public class ChatTranslationController {

    private final ChatTranslationService chatTranslationService;

    @PostMapping
    public ApiResponse<ChatTranslationResponse> translate(
            @PathVariable UUID messageId, @Valid @RequestBody ChatTranslationRequest request) {
        SupabasePrincipal principal = SupabaseAuthenticationUtils.currentPrincipal()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return ApiResponse.ok(chatTranslationService.translate(principal.userId(), messageId, request.targetLanguage()));
    }
}
