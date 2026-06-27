package com.deoham.chat.controller;

import com.deoham.chat.controller.docs.ChatRoomControllerDocs;
import com.deoham.chat.dto.ChatRoomCreateRequest;
import com.deoham.chat.dto.ChatRoomResponse;
import com.deoham.chat.service.ChatRoomService;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.global.response.ApiResponse;
import com.deoham.global.security.SupabaseAuthenticationUtils;
import com.deoham.global.security.SupabasePrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
public class ChatRoomController implements ChatRoomControllerDocs {

    private final ChatRoomService chatRoomService;

    @Override
    @PostMapping
    public ApiResponse<ChatRoomResponse> createRoom(@Valid @RequestBody ChatRoomCreateRequest request) {
        UUID userId = currentUserId();
        return ApiResponse.ok(chatRoomService.createRoom(userId, request));
    }

    @Override
    @GetMapping
    public ApiResponse<Page<ChatRoomResponse>> getMyRooms(Pageable pageable) {
        UUID userId = currentUserId();
        return ApiResponse.ok(chatRoomService.getMyRooms(userId, pageable));
    }

    @Override
    @GetMapping("/{roomId}")
    public ApiResponse<ChatRoomResponse> getRoom(@PathVariable UUID roomId) {
        UUID userId = currentUserId();
        return ApiResponse.ok(chatRoomService.getRoom(userId, roomId));
    }

    @Override
    @PostMapping("/{roomId}/leave")
    public ApiResponse<Void> leaveRoom(@PathVariable UUID roomId) {
        UUID userId = currentUserId();
        chatRoomService.leaveRoom(userId, roomId);
        return ApiResponse.ok();
    }

    private UUID currentUserId() {
        SupabasePrincipal principal = SupabaseAuthenticationUtils.currentPrincipal()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return principal.userId();
    }
}
