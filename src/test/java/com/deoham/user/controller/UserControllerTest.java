package com.deoham.user.controller;

import com.deoham.user.dto.ProfileResponse;
import com.deoham.user.dto.ProfileUpdateRequest;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.user.entity.User;
import com.deoham.user.service.UserReadService;
import com.deoham.user.service.UserWriteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.ArgumentMatchers.isNull;
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

	@Nested
	@DisplayName("POST /api/user/profile - 프로필 생성")
	class CreateProfileTest {

		@Test
		@DisplayName("[Test 1] ✅ 닉네임만 제공 (이미지 없음)")
		void testCreateProfileWithNicknameOnly() throws Exception {
			// given
			UUID userId = UUID.randomUUID();
			String nickname = "홍길동";
			ProfileUpdateRequest request = new ProfileUpdateRequest(nickname);
			User createdUser = User.builder()
					.nickname(nickname)
					.profileImageUrl(null)
					.build();

			when(userWriteService.updateProfile(eq(userId), eq(nickname), any()))
					.thenReturn(createdUser);

			MockMultipartFile requestPart = new MockMultipartFile(
					"request", null, "application/json", objectMapper.writeValueAsBytes(request));

			// when & then
			mockMvc.perform(multipart(POST, "/api/user/profile")
					.file(requestPart)
					.with(jwt().jwt(jwt -> jwt
							.subject(userId.toString())
							.claim("email", "test@example.com")
							.claim("role", "USER"))))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.nickname").value(nickname));
		}

		@Test
		@DisplayName("[Test 2] ✅ 이미지만 제공 (닉네임 없음)")
		void testCreateProfileWithImageOnly() throws Exception {
			// given
			UUID userId = UUID.randomUUID();
			String imageUrl = "https://ondo-2026-itstime.s3.ap-northeast-2.amazonaws.com/profiles/" + userId + "/test-image.jpg";
			ProfileUpdateRequest request = new ProfileUpdateRequest(null);
			User createdUser = User.builder()
					.nickname(null)
					.profileImageUrl(imageUrl)
					.build();

			when(userWriteService.updateProfile(eq(userId), isNull(), any()))
					.thenReturn(createdUser);

			MockMultipartFile requestPart = new MockMultipartFile(
					"request", null, "application/json", objectMapper.writeValueAsBytes(request));
			MockMultipartFile profileImage = new MockMultipartFile(
					"profileImage", "test-image.jpg", "image/jpeg", "fake image content".getBytes());

			// when & then
			mockMvc.perform(multipart(POST, "/api/user/profile")
					.file(requestPart)
					.file(profileImage)
					.with(jwt().jwt(jwt -> jwt
							.subject(userId.toString())
							.claim("email", "test@example.com")
							.claim("role", "USER"))))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.profileImageUrl").isNotEmpty());
		}

		@Test
		@DisplayName("[Test 3] ✅ 닉네임과 이미지 모두 제공")
		void testCreateProfileWithNicknameAndImage() throws Exception {
			// given
			UUID userId = UUID.randomUUID();
			String nickname = "김철수";
			String imageUrl = "https://ondo-2026-itstime.s3.ap-northeast-2.amazonaws.com/profiles/" + userId + "/test-image.jpg";
			ProfileUpdateRequest request = new ProfileUpdateRequest(nickname);
			User createdUser = User.builder()
					.nickname(nickname)
					.profileImageUrl(imageUrl)
					.build();

			when(userWriteService.updateProfile(eq(userId), eq(nickname), any()))
					.thenReturn(createdUser);

			MockMultipartFile requestPart = new MockMultipartFile(
					"request", null, "application/json", objectMapper.writeValueAsBytes(request));
			MockMultipartFile profileImage = new MockMultipartFile(
					"profileImage", "test-image.jpg", "image/jpeg", "fake image content".getBytes());

			// when & then
			mockMvc.perform(multipart(POST, "/api/user/profile")
					.file(requestPart)
					.file(profileImage)
					.with(jwt().jwt(jwt -> jwt
							.subject(userId.toString())
							.claim("email", "test@example.com")
							.claim("role", "USER"))))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.nickname").value(nickname))
					.andExpect(jsonPath("$.profileImageUrl").isNotEmpty());
		}

		@Test
		@DisplayName("[Test 4] ❌ 닉네임과 이미지 모두 미제공")
		void testCreateProfileBothEmpty() throws Exception {
			// given
			UUID userId = UUID.randomUUID();
			ProfileUpdateRequest request = new ProfileUpdateRequest(null);

			when(userWriteService.updateProfile(eq(userId), isNull(), isNull()))
					.thenThrow(new BusinessException(ErrorCode.INVALID_REQUEST,
							"Nickname or profile image must be provided."));

			MockMultipartFile requestPart = new MockMultipartFile(
					"request", null, "application/json", objectMapper.writeValueAsBytes(request));

			// when & then
			mockMvc.perform(multipart(POST, "/api/user/profile")
					.file(requestPart)
					.with(jwt().jwt(jwt -> jwt
							.subject(userId.toString())
							.claim("email", "test@example.com")
							.claim("role", "USER"))))
					.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("[Test 5] ❌ 중복된 닉네임 제공")
		void testCreateProfileWithDuplicateNickname() throws Exception {
			// given
			UUID userId = UUID.randomUUID();
			String nickname = "이미존재하는닉네임";
			ProfileUpdateRequest request = new ProfileUpdateRequest(nickname);

			when(userWriteService.updateProfile(eq(userId), eq(nickname), any()))
					.thenThrow(new BusinessException(ErrorCode.CONFLICT, "Nickname already exists."));

			MockMultipartFile requestPart = new MockMultipartFile(
					"request", null, "application/json", objectMapper.writeValueAsBytes(request));

			// when & then
			mockMvc.perform(multipart(POST, "/api/user/profile")
					.file(requestPart)
					.with(jwt().jwt(jwt -> jwt
							.subject(userId.toString())
							.claim("email", "test@example.com")
							.claim("role", "USER"))))
					.andExpect(status().isConflict());
		}

		@Test
		@DisplayName("[Test 6] ✅ PNG 형식 이미지 업로드")
		void testCreateProfileWithPngImage() throws Exception {
			// given
			UUID userId = UUID.randomUUID();
			String imageUrl = "https://ondo-2026-itstime.s3.ap-northeast-2.amazonaws.com/profiles/" + userId + "/test-image.png";
			ProfileUpdateRequest request = new ProfileUpdateRequest(null);
			User createdUser = User.builder()
					.nickname(null)
					.profileImageUrl(imageUrl)
					.build();

			when(userWriteService.updateProfile(eq(userId), isNull(), any()))
					.thenReturn(createdUser);

			MockMultipartFile requestPart = new MockMultipartFile(
					"request", null, "application/json", objectMapper.writeValueAsBytes(request));
			MockMultipartFile profileImage = new MockMultipartFile(
					"profileImage", "test-image.png", "image/png", "fake png content".getBytes());

			// when & then
			mockMvc.perform(multipart(POST, "/api/user/profile")
					.file(requestPart)
					.file(profileImage)
					.with(jwt().jwt(jwt -> jwt
							.subject(userId.toString())
							.claim("email", "test@example.com")
							.claim("role", "USER"))))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.profileImageUrl").isNotEmpty());
		}
	}

	@Nested
	@DisplayName("PUT /api/user/profile - 프로필 업데이트")
	class UpdateProfileTest {

		@Test
		@DisplayName("[Test 8] ✅ 닉네임만 변경")
		void testUpdateProfileWithNicknameOnly() throws Exception {
			// given
			UUID userId = UUID.randomUUID();
			String nickname = "새로운닉네임";
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
		@DisplayName("[Test 9] ✅ 이미지만 변경 (닉네임 변경 없음)")
		void testUpdateProfileWithImageOnly() throws Exception {
			// given
			UUID userId = UUID.randomUUID();
			String imageUrl = "https://ondo-2026-itstime.s3.ap-northeast-2.amazonaws.com/profiles/" + userId + "/new-image.jpg";
			ProfileUpdateRequest request = new ProfileUpdateRequest(null);
			User updatedUser = User.builder()
					.nickname("기존닉네임")
					.profileImageUrl(imageUrl)
					.build();

			when(userWriteService.updateProfile(eq(userId), isNull(), any()))
					.thenReturn(updatedUser);

			MockMultipartFile requestPart = new MockMultipartFile(
					"request", null, "application/json", objectMapper.writeValueAsBytes(request));
			MockMultipartFile profileImage = new MockMultipartFile(
					"profileImage", "new-image.jpg", "image/jpeg", "fake image content".getBytes());

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
		@DisplayName("[Test 10] ✅ 닉네임과 이미지 모두 변경")
		void testUpdateProfileWithNicknameAndImage() throws Exception {
			// given
			UUID userId = UUID.randomUUID();
			String nickname = "또다른닉네임";
			String imageUrl = "https://ondo-2026-itstime.s3.ap-northeast-2.amazonaws.com/profiles/" + userId + "/new-image.jpg";
			ProfileUpdateRequest request = new ProfileUpdateRequest(nickname);
			User updatedUser = User.builder()
					.nickname(nickname)
					.profileImageUrl(imageUrl)
					.build();

			when(userWriteService.updateProfile(eq(userId), eq(nickname), any()))
					.thenReturn(updatedUser);

			MockMultipartFile profileImage = new MockMultipartFile(
					"profileImage", "new-image.jpg", "image/jpeg", "fake image content".getBytes());
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
