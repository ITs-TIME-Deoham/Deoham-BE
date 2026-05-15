package com.deoham.auth.controller;

import com.deoham.auth.dto.LoginRequest;
import com.deoham.auth.dto.SignupRequest;
import com.deoham.auth.dto.TokenResponse;
import com.deoham.global.response.ApiResponse;
import com.deoham.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "인증 API")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ----------------------------------------------------------------
    // Email / Password
    // ----------------------------------------------------------------

    @PostMapping("/signup")
    @Operation(summary = "이메일 회원가입", description = "이메일과 비밀번호로 회원가입합니다. 성공 시 토큰을 즉시 발급합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "입력값 검증 실패 (이메일 형식 오류, 비밀번호 8자 미만 등)",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 가입된 이메일",
                    content = @Content
            )
    })
    public ResponseEntity<ApiResponse<TokenResponse>> signup(
            @RequestBody @Valid SignupRequest request
    ) {
        TokenResponse token = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(token));
    }

    @PostMapping("/login")
    @Operation(summary = "이메일 로그인", description = "이메일과 비밀번호로 로그인합니다. 성공 시 액세스 토큰과 리프레시 토큰을 발급합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "입력값 검증 실패",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "이메일 또는 비밀번호 불일치",
                    content = @Content
            )
    })
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @RequestBody @Valid LoginRequest request
    ) {
        TokenResponse token = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(token));
    }

    // ----------------------------------------------------------------
    // Kakao OAuth
    // ----------------------------------------------------------------

    @GetMapping("/kakao")
    @Operation(
            summary = "카카오 로그인 시작",
            description = "Supabase OAuth URL(provider=kakao)로 리다이렉트합니다. " +
                    "redirect_to는 서버의 SUPABASE_REDIRECT_URL 환경변수 값을 사용합니다. " +
                    "브라우저에서 직접 호출해야 합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "302",
                    description = "Supabase Kakao OAuth URL로 리다이렉트",
                    headers = @Header(
                            name = "Location",
                            description = "https://<project>.supabase.co/auth/v1/authorize?provider=kakao&redirect_to={SUPABASE_REDIRECT_URL}",
                            schema = @Schema(type = "string")
                    )
            )
    })
    public ResponseEntity<Void> kakaoLogin() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(authService.kakaoRedirectUri())
                .build();
    }

    // ----------------------------------------------------------------
    // Logout
    // ----------------------------------------------------------------

    @PostMapping("/logout")
    @Operation(
            summary = "로그아웃",
            description = "현재 세션을 무효화합니다. Authorization 헤더의 액세스 토큰이 필요합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (토큰 없음 또는 만료)",
                    content = @Content
            )
    })
    public ResponseEntity<ApiResponse<Void>> logout() {
        // TODO: implement
        return null;
    }
}
