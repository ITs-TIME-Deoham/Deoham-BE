package com.deoham.user.service;

import com.deoham.TestcontainersConfiguration;
import com.deoham.global.config.S3Properties;
import com.deoham.global.security.JwtTokenProvider;
import com.deoham.user.entity.User;
import com.deoham.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("UserWriteService S3 통합 테스트 (실제 AWS)")
class UserWriteServiceS3Test {

	private static final String TEST_JWT_SECRET = "test-jwt-secret-key-for-s3-integration-tests-minimum-32!";
	private static final String REAL_BUCKET = "ondo-2026-itstime";
	private static final String REAL_REGION = "ap-northeast-2";

	@DynamicPropertySource
	static void overrideProperties(DynamicPropertyRegistry registry) {
		registry.add("jwt.secret", () -> TEST_JWT_SECRET);
		registry.add("deoham.s3.bucket", () -> REAL_BUCKET);
		registry.add("deoham.s3.region", () -> REAL_REGION);
	}

	@Autowired
	private UserWriteService userWriteService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private S3Client s3Client;

	@Autowired
	private S3Properties s3Properties;

	private User testUser;

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();
		testUser = userRepository.save(User.builder()
				.firebaseUid("test-uid-" + UUID.randomUUID())
				.nickname("테스트사용자")
				.build());
	}

	@Test
	@DisplayName("[S3 실제] JPG 이미지 업로드 및 URL 생성")
	void testUploadProfileImageJpg() throws Exception {
		// given
		String nickname = "이미지테스트";
		MockMultipartFile imageFile = new MockMultipartFile(
				"profileImage",
				"profile.jpg",
				"image/jpeg",
				"fake jpeg image data".getBytes()
		);

		// when
		User updatedUser = userWriteService.updateProfile(
				testUser.getId(),
				nickname,
				imageFile
		);

		// then
		assertThat(updatedUser.getNickname()).isEqualTo(nickname);
		assertThat(updatedUser.getProfileImageUrl()).isNotNull();
		assertThat(updatedUser.getProfileImageUrl()).contains("s3.");
		assertThat(updatedUser.getProfileImageUrl()).contains("ap-northeast-2");
		assertThat(updatedUser.getProfileImageUrl()).contains(REAL_BUCKET);
	}

	@Test
	@DisplayName("[S3 실제] PNG 이미지 업로드 및 URL 생성")
	void testUploadProfileImagePng() throws Exception {
		// given
		String nickname = "PNG테스트";
		MockMultipartFile imageFile = new MockMultipartFile(
				"profileImage",
				"profile.png",
				"image/png",
				"fake png image data".getBytes()
		);

		// when
		User updatedUser = userWriteService.updateProfile(
				testUser.getId(),
				nickname,
				imageFile
		);

		// then
		assertThat(updatedUser.getNickname()).isEqualTo(nickname);
		assertThat(updatedUser.getProfileImageUrl()).isNotNull();
		assertThat(updatedUser.getProfileImageUrl()).contains(".png");
	}

	@Test
	@DisplayName("[S3 실제] 여러 이미지 업로드 - 파일명 중복 방지")
	void testUploadMultipleProfileImages() throws Exception {
		// given
		String nickname1 = "첫번째";
		String nickname2 = "두번째";
		MockMultipartFile imageFile = new MockMultipartFile(
				"profileImage",
				"profile.jpg",
				"image/jpeg",
				"fake jpeg data".getBytes()
		);

		// when
		User user1 = userWriteService.updateProfile(testUser.getId(), nickname1, imageFile);
		String url1 = user1.getProfileImageUrl();

		User user2 = userWriteService.updateProfile(testUser.getId(), nickname2, imageFile);
		String url2 = user2.getProfileImageUrl();

		// then - URL이 다른지 확인 (UUID로 구분)
		assertThat(url1).isNotEqualTo(url2);
		assertThat(url1).contains("profiles/");
		assertThat(url2).contains("profiles/");
	}

	@Test
	@DisplayName("[S3 실제] S3 경로 형식 검증")
	void testS3UrlFormat() throws Exception {
		// given
		MockMultipartFile imageFile = new MockMultipartFile(
				"profileImage",
				"test.jpg",
				"image/jpeg",
				"image data".getBytes()
		);

		// when
		User updatedUser = userWriteService.updateProfile(
				testUser.getId(),
				"경로테스트",
				imageFile
		);

		String url = updatedUser.getProfileImageUrl();

		// then - S3 URL 형식: https://{bucket}.s3.{region}.amazonaws.com/{key}
		assertThat(url).startsWith("https://");
		assertThat(url).contains(s3Properties.bucket());
		assertThat(url).contains(".s3.");
		assertThat(url).contains(s3Properties.region());
		assertThat(url).contains("amazonaws.com");
		assertThat(url).contains("profiles/");
		assertThat(url).contains(testUser.getId().toString());
	}

	@Test
	@DisplayName("[S3 실제] 이미지 없이 닉네임만 업로드")
	void testUpdateProfileWithoutImage() throws Exception {
		// given
		String nickname = "이미지없음";

		// when
		User updatedUser = userWriteService.updateProfile(
				testUser.getId(),
				nickname,
				null
		);

		// then
		assertThat(updatedUser.getNickname()).isEqualTo(nickname);
		assertThat(updatedUser.getProfileImageUrl()).isNull();
	}
}
