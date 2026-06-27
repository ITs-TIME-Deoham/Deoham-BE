package com.deoham.chat.controller;

import com.deoham.chat.controller.docs.ChatMessageControllerDocs;
import com.deoham.chat.dto.ChatAttachmentPresignRequest;
import com.deoham.chat.dto.ChatAttachmentPresignResponse;
import com.deoham.chat.dto.ChatMessagePageResponse;
import com.deoham.chat.dto.ChatMessageResponse;
import com.deoham.chat.dto.ChatMessageSendRequest;
import com.deoham.chat.service.ChatAttachmentService;
import com.deoham.chat.service.ChatMessageService;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.global.response.ApiResponse;
import com.deoham.global.security.SupabaseAuthenticationUtils;
import com.deoham.global.security.SupabasePrincipal;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat/rooms/{roomId}")
@RequiredArgsConstructor
public class ChatMessageController implements ChatMessageControllerDocs {

    private final ChatMessageService chatMessageService;
    private final ChatAttachmentService chatAttachmentService;

    @Override
    @PostMapping("/messages")
    public ApiResponse<ChatMessageResponse> sendMessage(
            @PathVariable UUID roomId,
            @Valid @RequestBody ChatMessageSendRequest request) {
        UUID userId = currentUserId();
        return ApiResponse.ok(chatMessageService.sendMessage(roomId, userId, request));
    }

    @Override
    @GetMapping("/messages")
    public ApiResponse<ChatMessagePageResponse> getMessages(
            @PathVariable UUID roomId,
            @RequestParam(required = false) Instant before,
            @RequestParam(defaultValue = "30") int size) {
        UUID userId = currentUserId();
        return ApiResponse.ok(chatMessageService.getMessages(userId, roomId, before, size));
    }

    @Override
    @PostMapping("/attachments/presign")
    public ApiResponse<ChatAttachmentPresignResponse> presignAttachment(
            @PathVariable UUID roomId,
            @Valid @RequestBody ChatAttachmentPresignRequest request) {
        UUID userId = currentUserId();
        return ApiResponse.ok(chatAttachmentService.createUploadUrl(roomId, userId, request));
    }

    private UUID currentUserId() {
        SupabasePrincipal principal = SupabaseAuthenticationUtils.currentPrincipal()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return principal.userId();
    }
}
