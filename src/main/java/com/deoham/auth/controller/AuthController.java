package com.deoham.auth.controller;

import com.deoham.auth.dto.KakaoCallbackRequest;
import com.deoham.auth.dto.KakaoCallbackResponse;
import com.deoham.auth.dto.RefreshTokenRequest;
import com.deoham.auth.dto.TokenResponse;
import com.deoham.auth.service.AuthService;
import com.deoham.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    // Kakao OAuth
    // ----------------------------------------------------------------

    @GetMapping("/kakao")
    @Operation(
            summary = "카카오 로그인 시작",
            description = "카카오 OAuth 인가 URL로 302 리다이렉트합니다. " +
                    "redirect_uri는 서버의 KAKAO_REDIRECT_URI 환경변수 값을 사용합니다. " +
                    "서버가 OAuth state를 생성하고 저장하므로 이 엔드포인트를 통해 로그인을 시작해야 합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "302",
                    description = "카카오 OAuth 인가 URL로 리다이렉트",
                    headers = @Header(
                            name = "Location",
                            description = "https://kauth.kakao.com/oauth/authorize?client_id={KAKAO_REST_API_KEY}&redirect_uri={KAKAO_REDIRECT_URI}&response_type=code",
                            schema = @Schema(type = "string")
                    )
            )
    })
    public ResponseEntity<Void> kakaoAuthRedirect() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(authService.kakaoAuthorizationUri())
                .build();
    }

    @PostMapping("/kakao/callback")
    @Operation(
            summary = "카카오 로그인 콜백",
            description = "프론트엔드가 카카오로부터 받은 인가 코드를 백엔드로 전달합니다. " +
                    "요청에는 카카오 리다이렉트 쿼리의 code와 state를 모두 포함해야 합니다. " +
                    "백엔드가 카카오 API와 직접 토큰 교환 및 유저 정보 조회를 수행하고, " +
                    "우리 서버의 JWT를 발급합니다. 최초 로그인이면 isNewUser=true를 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인/회원가입 성공, JWT 반환",
                    content = @Content(schema = @Schema(implementation = KakaoCallbackResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "입력값 검증 실패",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않은 인가 코드",
                    content = @Content
            )
    })
    public ResponseEntity<ApiResponse<KakaoCallbackResponse>> kakaoCallback(
            @RequestBody @Valid KakaoCallbackRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(authService.kakaoLogin(request.code(), request.state())));
    }

    // ----------------------------------------------------------------
    // Token
    // ----------------------------------------------------------------

    @PostMapping("/refresh")
    @Operation(
            summary = "액세스 토큰 갱신",
            description = "만료된 액세스 토큰을 리프레시 토큰으로 갱신합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "토큰 갱신 성공",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않거나 만료된 리프레시 토큰",
                    content = @Content
            )
    })
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @RequestBody @Valid RefreshTokenRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request.refreshToken())));
    }

    // ----------------------------------------------------------------
    // Logout
    // ----------------------------------------------------------------

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 사용자에게 저장된 리프레시 토큰을 폐기합니다.")
    public ResponseEntity<ApiResponse<Void>> logout(org.springframework.security.core.Authentication authentication) {
        authService.logout(authentication);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
