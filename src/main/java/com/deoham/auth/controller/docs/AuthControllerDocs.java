package com.deoham.auth.controller.docs;

import com.deoham.auth.dto.KakaoCallbackRequest;
import com.deoham.auth.dto.KakaoCallbackResponse;
import com.deoham.auth.dto.RefreshTokenRequest;
import com.deoham.auth.dto.TokenResponse;
import com.deoham.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@Tag(name = "Auth", description = "인증 API")
public interface AuthControllerDocs {

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
	ResponseEntity<Void> kakaoAuthRedirect();

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
	ResponseEntity<KakaoCallbackResponse> kakaoCallback(@Valid KakaoCallbackRequest request);

	@Operation(
			summary = "액세스 토큰 갱신",
			description = "만료된 액세스 토큰을 리프레시 토큰으로 갱신합니다. " +
					"갱신 시 리프레시 토큰도 함께 회전(rotation)되어 새 리프레시 토큰이 발급되며, " +
					"기존 리프레시 토큰은 더 이상 사용할 수 없습니다. 응답의 refreshToken을 반드시 교체 저장하세요."
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
	ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid RefreshTokenRequest request);

	@Operation(summary = "로그아웃", description = "현재 사용자에게 저장된 리프레시 토큰을 모두 폐기합니다. " +
			"발급된 액세스 토큰은 만료 시까지 유효하므로, 클라이언트에서도 저장된 토큰을 삭제해야 합니다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "로그아웃 성공 (data: null)"
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "401",
					description = "인증 필요",
					content = @Content
			)
	})
	ResponseEntity<ApiResponse<Void>> logout(Authentication authentication);
}
