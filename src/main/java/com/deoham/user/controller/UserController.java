package com.deoham.user.controller;

import com.deoham.user.controller.docs.UserControllerDocs;
import com.deoham.user.dto.ProfileResponse;
import com.deoham.user.dto.ProfileUpdateRequest;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.global.metrics.MetricEndpoint;
import com.deoham.global.security.AuthenticationUtils;
import com.deoham.user.service.UserReadService;
import com.deoham.user.service.UserWriteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController implements UserControllerDocs {

	private final UserReadService userReadService;
	private final UserWriteService userWriteService;
	private final ObjectMapper objectMapper;

	@Override
	@GetMapping("/profile")
	@MetricEndpoint("user.profile.get")
	public ResponseEntity<ProfileResponse> getProfile(Authentication authentication) {
		var profile = userReadService.getProfile(AuthenticationUtils.requiredUserId(authentication));
		return ResponseEntity.ok(profile);
	}

	@Override
	@PostMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@MetricEndpoint("user.profile.create")
	public ResponseEntity<ProfileResponse> createProfile(
			Authentication authentication,
			@RequestPart(name = "request", required = false) String requestJson,
			@RequestPart(name = "profileImage", required = false) MultipartFile profileImage
	) throws Exception {
		ProfileUpdateRequest request = null;
		if (requestJson != null && !requestJson.isBlank()) {
			request = objectMapper.readValue(requestJson, ProfileUpdateRequest.class);
		}

		if (request == null && (profileImage == null || profileImage.isEmpty())) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "닉네임 또는 프로필 이미지 중 최소 하나는 필수입니다");
		}

		String nickname = request != null ? request.nickname() : null;
		var updatedUser = userWriteService.updateProfile(
				AuthenticationUtils.requiredUserId(authentication),
				nickname,
				profileImage
		);
		return ResponseEntity.status(HttpStatus.CREATED).body(ProfileResponse.from(updatedUser));
	}

	@Override
	@PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@MetricEndpoint("user.profile.update")
	public ResponseEntity<Void> updateProfile(
			Authentication authentication,
			@RequestPart(name = "request", required = false) String requestJson,
			@RequestPart(name = "profileImage", required = false) MultipartFile profileImage
	) throws Exception {
		ProfileUpdateRequest request = null;
		if (requestJson != null && !requestJson.isBlank()) {
			request = objectMapper.readValue(requestJson, ProfileUpdateRequest.class);
		}

		if (request == null && (profileImage == null || profileImage.isEmpty())) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "닉네임 또는 프로필 이미지 중 최소 하나는 필수입니다");
		}

		String nickname = request != null ? request.nickname() : null;
		userWriteService.updateProfile(
				AuthenticationUtils.requiredUserId(authentication),
				nickname,
				profileImage
		);
		return ResponseEntity.noContent().build();
	}

	@Override
	@DeleteMapping("/profile")
	@MetricEndpoint("user.profile.image.delete")
	public ResponseEntity<Void> deleteProfileImage(Authentication authentication) {
		userWriteService.deleteProfileImage(AuthenticationUtils.requiredUserId(authentication));
		return ResponseEntity.noContent().build();
	}

	@Override
	@DeleteMapping
	@MetricEndpoint("user.delete")
	public ResponseEntity<Void> deleteUser(Authentication authentication) {
		userWriteService.deleteUser(AuthenticationUtils.requiredUserId(authentication));
		return ResponseEntity.noContent().build();
	}
}
