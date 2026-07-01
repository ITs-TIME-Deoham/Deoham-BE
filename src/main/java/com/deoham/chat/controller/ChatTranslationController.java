package com.deoham.chat.controller;

import com.deoham.chat.controller.docs.ChatTranslationControllerDocs;
import com.deoham.chat.dto.ChatTranslationRequest;
import com.deoham.chat.dto.ChatTranslationResponse;
import com.deoham.chat.service.ChatTranslationService;
import com.deoham.global.response.ApiResponse;
import com.deoham.global.security.AuthenticationUtils;
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
public class ChatTranslationController implements ChatTranslationControllerDocs {

    private final ChatTranslationService chatTranslationService;

    @Override
    @PostMapping
    public ApiResponse<ChatTranslationResponse> translate(
            @PathVariable UUID messageId,
            @Valid @RequestBody ChatTranslationRequest request) {
        return ApiResponse.ok(chatTranslationService.translate(
                AuthenticationUtils.requireCurrentUserId(),
                messageId,
                request.targetLanguage()
        ));
    }
}
