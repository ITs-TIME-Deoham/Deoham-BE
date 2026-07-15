package com.deoham.user.controller;

import com.deoham.auth.dto.ProfileResponse;
import com.deoham.auth.dto.ProfileUpdateRequest;
import com.deoham.user.entity.User;
import com.deoham.user.service.UserReadService;
import com.deoham.user.service.UserWriteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@DisplayName("사용자 컨트롤러 테스트")
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private UserReadService userReadService;

	@MockBean
	private UserWriteService userWriteService;

	@Test
	@DisplayName("프로필 조회 - 성공")
	void testGetProfileSuccess() throws Exception {
		// given
		UUID userId = UUID.randomUUID();
		ProfileResponse response = ProfileResponse.builder()
				.nickname("testNickname")
				.profileImageUrl("https://example.com/profile.png")
				.helpRequestCount(5)
				.helpCount(3)
				.build();

		when(userReadService.getProfile(userId))
				.thenReturn(response);

		// when & then
		mockMvc.perform(get("/api/user/profile")
				.with(jwt().jwt(jwt -> jwt
						.subject(userId.toString())
						.claim("email", "test@example.com")
						.claim("role", "USER"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nickname").value("testNickname"))
				.andExpect(jsonPath("$.profileImageUrl").value("https://example.com/profile.png"))
				.andExpect(jsonPath("$.helpRequestCount").value(5))
				.andExpect(jsonPath("$.helpCount").value(3));
	}

	@Test
	@DisplayName("프로필 업데이트 - 닉네임과 이미지 함께")
	void testUpdateProfileWithNicknameAndImage() throws Exception {
		// given
		UUID userId = UUID.randomUUID();
		String nickname = "updatedNickname";
		ProfileUpdateRequest request = new ProfileUpdateRequest(nickname);
		User updatedUser = User.builder()
				.nickname(nickname)
				.profileImageUrl("https://s3.example.com/image.jpg")
				.build();

		when(userWriteService.updateProfile(eq(userId), eq(nickname), any()))
				.thenReturn(updatedUser);

		MockMultipartFile profileImage = new MockMultipartFile(
				"profileImage", "test.jpg", "image/jpeg", "fake image content".getBytes());
		MockMultipartFile requestPart = new MockMultipartFile(
				"request", null, "application/json", objectMapper.writeValueAsBytes(request));

		// when & then
		mockMvc.perform(multipart(PUT, "/api/user/profile")
				.file(requestPart)
				.file(profileImage)
				.with(jwt().jwt(jwt -> jwt
						.subject(userId.toString())
						.claim("email", "test@example.com")
						.claim("role", "USER"))))
				.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("프로필 업데이트 - 닉네임만")
	void testUpdateProfileWithNicknameOnly() throws Exception {
		// given
		UUID userId = UUID.randomUUID();
		String nickname = "updatedNickname";
		ProfileUpdateRequest request = new ProfileUpdateRequest(nickname);
		User updatedUser = User.builder()
				.nickname(nickname)
				.profileImageUrl(null)
				.build();

		when(userWriteService.updateProfile(eq(userId), eq(nickname), any()))
				.thenReturn(updatedUser);

		MockMultipartFile requestPart = new MockMultipartFile(
				"request", null, "application/json", objectMapper.writeValueAsBytes(request));

		// when & then
		mockMvc.perform(multipart(PUT, "/api/user/profile")
				.file(requestPart)
				.with(jwt().jwt(jwt -> jwt
						.subject(userId.toString())
						.claim("email", "test@example.com")
						.claim("role", "USER"))))
				.andExpect(status().isNoContent());
	}

	@Test
	@Disabled("TODO: multipart POST validation test")
	@DisplayName("프로필 생성 - 닉네임과 이미지 모두 없음 실패")
	void testCreateProfileBothEmpty() throws Exception {
		// given
		UUID userId = UUID.randomUUID();
		ProfileUpdateRequest request = new ProfileUpdateRequest(null);
		MockMultipartFile requestPart = new MockMultipartFile(
				"request", null, "application/json", objectMapper.writeValueAsBytes(request));

		// when & then
		mockMvc.perform(multipart(POST, "/api/user/profile")
				.file(requestPart)
				.with(jwt().jwt(jwt -> jwt.subject(userId.toString()))))
				.andExpect(status().isBadRequest());
	}

	@TestConfiguration
	@EnableWebSecurity
	static class TestSecurityConfig {

		@Bean
		SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
			http
					.csrf(csrf -> csrf.disable())
					.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
			return http.build();
		}
	}
}
