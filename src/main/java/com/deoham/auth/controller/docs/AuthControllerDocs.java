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
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Auth", description = "인증 API")
public interface AuthControllerDocs {

	class KakaoCallbackApiResponse {
		@Schema(description = "요청 성공 여부", example = "true")
		public boolean success;

		@Schema(description = "카카오 로그인 콜백 응답")
		public KakaoCallbackResponse data;

		@Schema(description = "오류 정보. 성공 시 null", nullable = true)
		public ApiResponse.ErrorBody error;
	}

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
					"AccessToken은 Authorization 헤더, RefreshToken은 HttpOnly 쿠키로 반환됩니다. "
	)
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "로그인/회원가입 성공, AccessToken(헤더) + RefreshToken(쿠키) + 신규사용자 여부(body) 반환",
					headers = {
							@Header(name = "Authorization", description = "Bearer {accessToken}", schema = @Schema(type = "string")),
							@Header(name = "Set-Cookie", description = "refreshToken={refreshToken}; Path=/api/auth; Max-Age=604800; HttpOnly; SameSite=Strict; Secure는 app.auth.httponly.secure-cookie=true일 때 포함됩니다.", schema = @Schema(type = "string"))
					},
					content = @Content(
						mediaType = "application/json",
						schema = @Schema(
							example = "{\"success\": true, \"data\": {\"isNewUser\": false}, \"error\": null}",
							type = "object",
							implementation = KakaoCallbackApiResponse.class
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
	ResponseEntity<ApiResponse<KakaoCallbackResponse>> kakaoCallback(@RequestBody @Valid KakaoCallbackRequest request, HttpServletResponse response);

	@Operation(
			summary = "액세스 토큰 갱신",
			description = "만료된 액세스 토큰을 리프레시 토큰으로 갱신합니다. " +
					"RefreshToken은 자동으로 쿠키에서 추출되므로 요청 바디에 포함할 필요가 없습니다.\n\n" +
					"**토큰 갱신 정책 (변경됨)**\n" +
					"매 요청마다 새로운 RefreshToken을 발급합니다(이전: 24시간 이내 만료시에만 발급).\n" +
					"이를 통해 토큰 탈취 후 재사용 기간을 최소화합니다.\n\n" +
					"**응답 정보**\n" +
					"- 새 AccessToken: Authorization 응답 헤더에 `Bearer {token}` 형식으로 반환\n" +
					"- 새 RefreshToken: HttpOnly 쿠키(Set-Cookie 헤더)로 자동 설정, 브라우저가 관리\n" +
					"- ResponseBody: 추가 정보 없음 (성공 여부만 반환)\n\n" +
					"**프론트엔드 구현**\n" +
					"```javascript\n" +
					"const response = await fetch('/api/auth/refresh', {\n" +
					"  credentials: 'include' // HttpOnly 쿠키 자동 포함\n" +
					"});\n" +
					"if (response.ok) {\n" +
					"  const newAccessToken = response.headers.get('Authorization').replace('Bearer ', '');\n" +
					"  sessionStorage.setItem('accessToken', newAccessToken);\n" +
					"  // RefreshToken은 HttpOnly 쿠키에 자동 저장됨 (별도 처리 불필요)\n" +
					"}\n" +
					"```"
	)
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "토큰 갱신 성공, 새 AccessToken(헤더) + 새 RefreshToken(쿠키) 반환",
					headers = {
							@Header(name = "Authorization", description = "Bearer {newAccessToken} (항상 새로 발급됨)", schema = @Schema(type = "string")),
							@Header(name = "Set-Cookie", description = "refreshToken={newRefreshToken}; Path=/api/auth; Max-Age=604800; HttpOnly; SameSite=Strict; Secure는 app.auth.httponly.secure-cookie=true일 때 포함됩니다.", schema = @Schema(type = "string"))
					},
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
					description = "로그아웃 성공 (RefreshToken 쿠키 삭제됨)",
					headers = @Header(name = "Set-Cookie", description = "refreshToken=; Path=/api/auth; Max-Age=0; HttpOnly; SameSite=Strict; Secure는 app.auth.httponly.secure-cookie=true일 때 포함됩니다.", schema = @Schema(type = "string"))
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "401",
					description = "인증 필요",
					content = @Content
			)
	})
	ResponseEntity<ApiResponse<Void>> logout(Authentication authentication, HttpServletResponse response);
}
