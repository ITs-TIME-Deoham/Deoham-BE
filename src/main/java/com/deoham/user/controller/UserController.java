package com.deoham.user.controller;

import com.deoham.global.response.ApiResponse;
import com.deoham.user.dto.UserMeResponse;
import com.deoham.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User", description = "유저 API")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

//     @GetMapping("/me")
//     @Operation(
//             summary = "내 프로필 조회",
//             description = "토큰으로 인증된 유저의 프로필을 반환합니다.",
//             security = @SecurityRequirement(name = "bearerAuth")
//     )
//     @ApiResponses({
//             @io.swagger.v3.oas.annotations.responses.ApiResponse(
//                     responseCode = "200",
//                     description = "성공",
//                     content = @Content(schema = @Schema(implementation = UserMeResponse.class))
//             ),
//             @io.swagger.v3.oas.annotations.responses.ApiResponse(
//                     responseCode = "401",
//                     description = "인증 실패 (토큰 없음 또는 만료)",
//                     content = @Content
//             ),
//             @io.swagger.v3.oas.annotations.responses.ApiResponse(
//                     responseCode = "404",
//                     description = "가입되지 않은 유저",
//                     content = @Content
//             )
//     })
    public ResponseEntity<ApiResponse<UserMeResponse>> getMe() {
        // TODO: implement
        return null;
    }
}
