package com.deoham.user.controller;

import com.deoham.auth.dto.ProfileResponse;
import com.deoham.auth.dto.ProfileUpdateRequest;
import com.deoham.user.entity.User;
import com.deoham.user.service.UserReadService;
import com.deoham.user.service.UserWriteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
	@DisplayName("프로필 업데이트 - 성공")
	void testUpdateProfileSuccess() throws Exception {
		// given
		UUID userId = UUID.randomUUID();
		String nickname = "updatedNickname";
		String profileImageUrl = "https://example.com/profile.png";
		ProfileUpdateRequest request = new ProfileUpdateRequest(nickname, profileImageUrl);
		User updatedUser = User.builder()
				.nickname(nickname)
				.profileImageUrl(profileImageUrl)
				.build();

		when(userWriteService.updateProfile(userId, nickname, profileImageUrl))
				.thenReturn(updatedUser);

		// when & then
		mockMvc.perform(put("/api/user/profile")
				.with(jwt().jwt(jwt -> jwt
						.subject(userId.toString())
						.claim("email", "test@example.com")
						.claim("role", "USER")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("프로필 업데이트 - nickname 없음 검증 실패")
	void testUpdateProfileMissingNickname() throws Exception {
		// given
		UUID userId = UUID.randomUUID();
		String jsonBody = "{\"profileImageUrl\": \"https://example.com/profile.png\"}";

		// when & then
		mockMvc.perform(put("/api/user/profile")
				.with(jwt().jwt(jwt -> jwt.subject(userId.toString())))
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonBody))
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
