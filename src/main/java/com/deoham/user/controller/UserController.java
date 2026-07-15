package com.deoham.user.controller;

import com.deoham.user.dto.ProfileResponse;
import com.deoham.user.dto.ProfileUpdateRequest;
import com.deoham.global.metrics.MetricEndpoint;
import com.deoham.global.security.AuthenticationUtils;
import com.deoham.user.service.UserReadService;
import com.deoham.user.service.UserWriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
	@MetricEndpoint("user.profile.get")
	public ResponseEntity<ProfileResponse> getProfile(Authentication authentication) {
		var profile = userReadService.getProfile(AuthenticationUtils.requiredUserId(authentication));
		return ResponseEntity.ok(profile);
	}

	@PostMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(
			summary = "프로필 생성(최초 설정)",
			description = "회원가입(카카오 최초 로그인) 직후 현재 로그인한 사용자의 프로필(닉네임, 프로필 이미지)을 설정합니다. " +
					"유저 레코드 자체는 카카오 로그인 시점에 이미 생성되어 있으며, 이 API는 초기 프로필 정보를 채웁니다. " +
					"닉네임과 프로필 이미지 중 최소 하나는 필수입니다. " +
					"multipart/form-data로 전송하며, S3에 저장됩니다.\n\n" +
					"**Request Body 예시:**\n" +
					"- request (JSON part): `{\"nickname\":\"홍길동\"}`\n" +
					"- profileImage (파일): image.jpg"
	)
	@RequestBody(
		description = "multipart/form-data 형식. " +
			"- request: ProfileUpdateRequest JSON (닉네임 선택사항) " +
			"- profileImage: 이미지 파일 (선택사항)",
			content = @Content(
				mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
				schemaProperties = {
						@SchemaProperty(
								name = "request",
								schema = @Schema(implementation = ProfileUpdateRequest.class)
						),
						@SchemaProperty(
								name = "profileImage",
								schema = @Schema(type = "string", format = "binary")
						)
				}
		)
	)
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "201",
					description = "프로필 생성 성공",
					content = @Content(schema = @Schema(implementation = ProfileResponse.class))
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
					responseCode = "404",
					description = "사용자 정보를 찾을 수 없음",
					content = @Content
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "409",
					description = "닉네임 중복",
					content = @Content
			)
	})
	@MetricEndpoint("user.profile.create")
	public ResponseEntity<ProfileResponse> createProfile(
			Authentication authentication,
			@RequestPart(name = "request")
			@Valid ProfileUpdateRequest request,
			@RequestPart(name = "profileImage", required = false)
			MultipartFile profileImage
	) {
		var updatedUser = userWriteService.updateProfile(
				AuthenticationUtils.requiredUserId(authentication),
				request.nickname(),
				profileImage
		);
		return ResponseEntity.status(HttpStatus.CREATED).body(ProfileResponse.from(updatedUser));
	}

	@PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "프로필 업데이트",
			description = "현재 로그인한 사용자의 닉네임과 프로필 이미지를 업데이트합니다. " +
					"닉네임과 프로필 이미지 중 최소 하나는 필수입니다. " +
					"multipart/form-data로 전송하며, S3에 저장됩니다. " +
					"성공 시 응답 본문 없이 204(No Content)를 반환합니다.\n\n" +
					"**Request Body 예시:**\n" +
					"- request (JSON part): `{\"nickname\":\"새로운닉네임\"}`\n" +
					"- profileImage (파일): image.jpg"
	)
	@RequestBody(
			description = "multipart/form-data 형식. " +
					"- request: ProfileUpdateRequest JSON (닉네임 선택사항) " +
					"- profileImage: 이미지 파일 (선택사항)",
			    content = @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schemaProperties = {
                    @SchemaProperty(
                            name = "request",
                            schema = @Schema(implementation = ProfileUpdateRequest.class)
                    ),
                    @SchemaProperty(
                            name = "profileImage",
                            schema = @Schema(type = "string", format = "binary")
                    )
            }
    	)
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
					responseCode = "404",
					description = "사용자 정보를 찾을 수 없음",
					content = @Content
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "409",
					description = "닉네임 중복",
					content = @Content
			)
	})
	@MetricEndpoint("user.profile.update")
	public ResponseEntity<Void> updateProfile(
			Authentication authentication,
			@RequestPart(name = "request")
			@Valid ProfileUpdateRequest request,
			@RequestPart(name = "profileImage", required = false)
			MultipartFile profileImage
	) {
		userWriteService.updateProfile(
				AuthenticationUtils.requiredUserId(authentication),
				request.nickname(),
				profileImage
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
	@MetricEndpoint("user.delete")
	public ResponseEntity<Void> deleteUser(Authentication authentication) {
		userWriteService.deleteUser(AuthenticationUtils.requiredUserId(authentication));
		return ResponseEntity.noContent().build();
	}
}
