package com.deoham.auth.controller;

import com.deoham.auth.dto.KakaoCallbackRequest;
import com.deoham.auth.dto.KakaoCallbackResponse;
import com.deoham.auth.dto.KakaoLoginResult;
import com.deoham.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
@DisplayName("인증 컨트롤러 테스트")
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private AuthService authService;

	@Test
	@DisplayName("카카오 로그인 시작 - 카카오 OAuth 인가 URL로 리다이렉트")
	void testKakaoAuthRedirect() throws Exception {
		// given
		String expectedAuthUrl = "https://kauth.kakao.com/oauth/authorize?" +
				"client_id=test_client_id&" +
				"redirect_uri=http%3A%2F%2Flocalhost%3A3000%2Fauth%2Fcallback&" +
				"response_type=code&" +
				"scope=account_email%2Cprofile&" +
				"state=test_state_value";

		when(authService.kakaoAuthorizationUri())
				.thenReturn(java.net.URI.create(expectedAuthUrl));

		// when & then
		mockMvc.perform(get("/api/auth/kakao"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl(expectedAuthUrl));
	}

	@Test
	@DisplayName("카카오 로그인 콜백 - 신규 사용자 로그인 성공")
	void testKakaoCallbackNewUser() throws Exception {
		// given
		String code = "test_auth_code_12345";
		String state = "test_state_value";
		KakaoCallbackRequest request = new KakaoCallbackRequest(code, state);

		KakaoLoginResult mockLoginResult = new KakaoLoginResult(
				"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test_access_token",
				"refresh_token_test_12345",
				true
		);

		when(authService.kakaoLogin(code, state))
				.thenReturn(mockLoginResult);

		// when & then
		mockMvc.perform(post("/api/auth/kakao/callback")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.isNewUser").value(true));
	}

	@Test
	@DisplayName("카카오 로그인 콜백 - 기존 사용자 로그인 성공")
	void testKakaoCallbackExistingUser() throws Exception {
		// given
		String code = "test_auth_code_67890";
		String state = "test_state_value";
		KakaoCallbackRequest request = new KakaoCallbackRequest(code, state);

		KakaoLoginResult mockLoginResult = new KakaoLoginResult(
				"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test_access_token_existing",
				"refresh_token_existing_12345",
				false
		);

		when(authService.kakaoLogin(code, state))
				.thenReturn(mockLoginResult);

		// when & then
		mockMvc.perform(post("/api/auth/kakao/callback")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.isNewUser").value(false));
	}

	@Test
	@DisplayName("카카오 로그인 콜백 - code 없음 검증 실패")
	void testKakaoCallbackMissingCode() throws Exception {
		// given
		String jsonBody = "{\"state\": \"test_state_value\"}";

		// when & then
		mockMvc.perform(post("/api/auth/kakao/callback")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonBody))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("카카오 로그인 콜백 - 빈 code 검증 실패")
	void testKakaoCallbackEmptyCode() throws Exception {
		// given
		String jsonBody = "{\"code\": \"\", \"state\": \"test_state_value\"}";

		// when & then
		mockMvc.perform(post("/api/auth/kakao/callback")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonBody))
				.andExpect(status().isBadRequest());
	}
}
