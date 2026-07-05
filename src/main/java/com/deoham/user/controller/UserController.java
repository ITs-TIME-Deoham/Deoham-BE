package com.deoham.user.controller;

import com.deoham.auth.dto.ProfileResponse;
import com.deoham.auth.dto.ProfileUpdateRequest;
import com.deoham.global.security.AuthenticationUtils;
import com.deoham.user.service.UserReadService;
import com.deoham.user.service.UserWriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@Tag(name = "User", description = "사용자 API")
@RequiredArgsConstructor
public class UserController {

	private final UserReadService userReadService;
	private final UserWriteService userWriteService;

	@GetMapping("/profile")
	@Operation(
			summary = "프로필 조회",
			description = "현재 로그인한 사용자의 프로필 정보를 조회합니다. " +
					"닉네임, 프로필 이미지 URL, 도움을 받은 횟수, 도움을 준 횟수를 반환합니다."
	)
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "프로필 조회 성공",
					content = @Content(schema = @Schema(implementation = ProfileResponse.class))
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "401",
					description = "인증 필요",
					content = @Content
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "404",
					description = "사용자 정보를 찾을 수 없음",
					content = @Content
			)
	})
	public ResponseEntity<ProfileResponse> getProfile(Authentication authentication) {
		var principal = AuthenticationUtils.fromAuthentication(authentication)
				.orElseThrow(() -> new IllegalStateException("Authentication required."));
		var profile = userReadService.getProfile(principal.userId());
		return ResponseEntity.ok(profile);
	}

	@PutMapping("/profile")
	@Operation(
			summary = "프로필 업데이트",
			description = "로그인 후 사용자의 닉네임과 프로필 사진 URL을 업데이트합니다. " +
					"닉네임은 필수 입력값이며, 프로필 사진 URL은 선택사항입니다. " +
					"업데이트된 프로필 정보를 반환합니다."
	)
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "204",
					description = "프로필 업데이트 성공",
					content = @Content
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "400",
					description = "입력값 검증 실패",
					content = @Content
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "401",
					description = "인증 필요",
					content = @Content
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "409",
					description = "닉네임 중복",
					content = @Content
			)
	})
	public ResponseEntity<Void> updateProfile(
			Authentication authentication,
			@RequestBody @Valid ProfileUpdateRequest request
	) {
		var principal = AuthenticationUtils.fromAuthentication(authentication)
				.orElseThrow(() -> new IllegalStateException("Authentication required."));
		userWriteService.updateProfile(
				principal.userId(),
				request.nickname(),
				request.profileImageUrl()
		);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping
	@Operation(
			summary = "회원 탈퇴",
			description = "현재 로그인한 사용자의 계정을 탈퇴합니다. (soft delete)"
	)
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "204",
					description = "회원 탈퇴 성공",
					content = @Content
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "401",
					description = "인증 필요",
					content = @Content
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "404",
					description = "사용자 정보를 찾을 수 없음",
					content = @Content
			)
	})
	public ResponseEntity<Void> deleteUser(Authentication authentication) {
		var principal = AuthenticationUtils.fromAuthentication(authentication)
				.orElseThrow(() -> new IllegalStateException("Authentication required."));
		userWriteService.deleteUser(principal.userId());
		return ResponseEntity.noContent().build();
	}
}
