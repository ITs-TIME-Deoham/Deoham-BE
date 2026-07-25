package com.deoham.auth.controller.docs;

import com.deoham.auth.dto.KakaoCallbackRequest;
import com.deoham.auth.dto.KakaoCallbackResponse;
import com.deoham.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
					"우리 서버의 JWT를 발급합니다. " +
					"AccessToken은 Authorization 헤더, RefreshToken은 HttpOnly 쿠키로 반환됩니다. " +
					"응답: { \"success\": true, \"data\": { \"isNewUser\": boolean }, \"error\": null }"
	)
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "로그인/회원가입 성공, AccessToken(헤더) + RefreshToken(쿠키) + 신규사용자 여부(body) 반환",
					headers = @Header(name = "Authorization", description = "Bearer {accessToken}", schema = @Schema(type = "string")),
					content = @Content(
						mediaType = "application/json",
						schema = @Schema(
							example = "{\"success\": true, \"data\": {\"isNewUser\": false}, \"error\": null}",
							type = "object",
							implementation = ApiResponse.class
						)
					)
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "400",
					description = "입력값 검증 실패 (code 또는 state 누락)",
					content = @Content(
						mediaType = "application/json",
						schema = @Schema(example = "{\"success\": false, \"data\": null, \"error\": {\"code\": \"INVALID_REQUEST\", \"message\": \"...\"}}")
					)
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "401",
					description = "유효하지 않은 인가 코드 또는 expired OAuth state",
					content = @Content(
						mediaType = "application/json",
						schema = @Schema(example = "{\"success\": false, \"data\": null, \"error\": {\"code\": \"UNAUTHORIZED\", \"message\": \"Invalid OAuth state\"}}")
					)
			)
	})
	ResponseEntity<ApiResponse<KakaoCallbackResponse>> kakaoCallback(@Valid KakaoCallbackRequest request, HttpServletResponse response);

	@Operation(
			summary = "액세스 토큰 갱신",
			description = "만료된 액세스 토큰을 리프레시 토큰으로 갱신합니다. " +
					"RefreshToken은 자동으로 쿠키에서 추출되므로 요청 바디에 포함할 필요가 없습니다. " +
					"갱신 시 만료 임박한 리프레시 토큰은 자동 회전되며, 새 RefreshToken이 발급되면 쿠키에 설정됩니다. " +
					"AccessToken은 Authorization 헤더, RefreshToken은 HttpOnly 쿠키로만 반환됩니다."
	)
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "토큰 갱신 성공, 새 AccessToken(헤더) + 갱신된 RefreshToken(쿠키, 필요시) 반환",
					headers = @Header(name = "Authorization", description = "Bearer {newAccessToken}", schema = @Schema(type = "string")),
					content = @Content(
						mediaType = "application/json",
						schema = @Schema(
							example = "{\"success\": true, \"data\": null, \"error\": null}",
							type = "object",
							implementation = ApiResponse.class
						)
					)
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "401",
					description = "유효하지 않거나 만료된 리프레시 토큰, 또는 토큰 없음",
					content = @Content(
						mediaType = "application/json",
						schema = @Schema(example = "{\"success\": false, \"data\": null, \"error\": {\"code\": \"UNAUTHORIZED\", \"message\": \"Refresh token has expired\"}}")
					)
			)
	})
	ResponseEntity<ApiResponse<Void>> refresh(HttpServletRequest request, HttpServletResponse response);

	@Operation(summary = "로그아웃", description = "현재 사용자에게 저장된 리프레시 토큰을 모두 폐기하고 쿠키를 삭제합니다. " +
			"발급된 액세스 토큰은 만료 시까지 유효하므로, 클라이언트에서도 저장된 토큰을 삭제해야 합니다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "로그아웃 성공 (RefreshToken 쿠키 삭제됨)"
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "401",
					description = "인증 필요",
					content = @Content
			)
	})
	ResponseEntity<ApiResponse<Void>> logout(Authentication authentication, HttpServletResponse response);
}
