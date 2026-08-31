package com.deoham.user.service.impl;

import com.deoham.card.entity.CardApplyStatus;
import com.deoham.card.entity.CardStatus;
import com.deoham.card.repository.CardApplyRepository;
import com.deoham.card.repository.CardRepository;
import com.deoham.global.config.S3Properties;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.global.metrics.MetricsRegistry;
import com.deoham.notification.repository.FcmTokenRepository;
import com.deoham.notification.repository.NotificationRepository;
import com.deoham.user.entity.User;
import com.deoham.user.entity.UserStatus;
import com.deoham.user.event.ProfileImageDeletedEvent;
import com.deoham.user.repository.UserRepository;
import com.deoham.user.repository.UserSocialAccountRepository;
import com.deoham.user.service.UserWriteService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserWriteServiceImpl implements UserWriteService {

	private final UserRepository userRepository;
	private final CardRepository cardRepository;
	private final CardApplyRepository cardApplyRepository;
	private final UserSocialAccountRepository userSocialAccountRepository;
	private final FcmTokenRepository fcmTokenRepository;
	private final NotificationRepository notificationRepository;
	private final MetricsRegistry metricsRegistry;
	private final S3Client s3Client;
	private final S3Properties s3Properties;
	private final ApplicationEventPublisher eventPublisher;

    @Override
    public User updateProfile(UUID userId, String nickname, MultipartFile profileImage) {
        User user = getUser(userId, "User not found.");

        if (nickname == null && (profileImage == null || profileImage.isEmpty())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Nickname or profile image must be provided.");
        }

        if (nickname != null && !nickname.equals(user.getNickname())) {
            if (userRepository.existsByNickname(nickname)) {
                throw new BusinessException(ErrorCode.CONFLICT, "Nickname already exists.");
            }
        }

        String profileImageUrl = null;
        if (profileImage != null && !profileImage.isEmpty()) {
            validateProfileImage(profileImage);
            try {
                profileImageUrl = uploadProfileImageToS3(userId, profileImage);
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Failed to upload image to S3: " + e.getMessage());
            }
        }

        user.updateProfile(nickname, profileImageUrl);
        if (nickname != null) {
            user.completeOnboarding();
        }
        return userRepository.save(user);
    }

    private void validateProfileImage(MultipartFile file) {
        if (file.getSize() > s3Properties.maxImageSizeBytes()) {
            long maxSizeMB = s3Properties.maxImageSizeBytes() / (1024 * 1024);
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                "Image size exceeds maximum allowed size of " + maxSizeMB + "MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                "Invalid file type. Only image files are allowed");
        }
    }

    private String uploadProfileImageToS3(UUID userId, MultipartFile file) throws Exception {
        String key = "profiles/" + userId + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
        byte[] bytes = file.getBytes();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(key)
                .contentType(file.getContentType())
                .contentLength((long) bytes.length)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));

        return "https://" + s3Properties.bucket() + ".s3." + s3Properties.region() + ".amazonaws.com/" + key;
    }

	@Override
	public void updateHelpCounts(UUID requesterId, UUID applicantId) {
		User requester = getUser(requesterId, "Requester not found.");
		User applicant = getUser(applicantId, "Applicant not found.");

		long requesterHelpRequestCount = cardRepository.countByRequesterIdAndStatus(requesterId, CardStatus.COMPLETED);
		long applicantHelpCount = cardApplyRepository.countByApplicantIdAndStatus(applicantId, CardApplyStatus.ACCEPTED);

		int requesterCount = safeCastToInt(requesterHelpRequestCount);
		int applicantCount = safeCastToInt(applicantHelpCount);

		requester.setHelpRequestCount(requesterCount);
		applicant.setHelpCount(applicantCount);

		userRepository.saveAll(java.util.List.of(requester, applicant));
	}

	@Override
	public void deleteUser(UUID userId) {
		metricsRegistry.recordTimedRun("user.delete", () -> {
			User user = getUser(userId, "User not found.");

			if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isBlank()) {
				try {
					deleteProfileImage(user);
				} catch (Exception e) {
					log.error("Failed to delete profile image for user: {}", userId, e);
				}
			}

			user.delete();
			userRepository.save(user);
			userSocialAccountRepository.deleteAllByUser_Id(userId);
		});
	}

	@Override
	public void deleteProfileImage(UUID userId) {
		User user = getUser(userId, "User not found.");
		deleteProfileImage(user);
	}

	/**
	 * S3 삭제는 직접 호출하지 않고 이벤트로 위임한다.
	 * 커밋 전에 지우면 이후 롤백 시 DB에는 URL이 남고 실물 파일만 사라지므로,
	 * {@link com.deoham.user.event.ProfileImageDeletionListener}가 커밋 이후 별도 스레드에서 처리한다.
	 */
	private void deleteProfileImage(User user) {
		String profileImageUrl = user.getProfileImageUrl();

		user.clearProfileImage();
		userRepository.save(user);

		if (profileImageUrl != null && !profileImageUrl.isBlank()) {
			eventPublisher.publishEvent(new ProfileImageDeletedEvent(profileImageUrl));
		}
	}

	public void deleteExpiredDeletedUsers() {
		Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
		List<User> expiredUsers = userRepository.findByStatusAndDeletedAtBefore(
			UserStatus.DELETED.name(), thirtyDaysAgo);

		if (expiredUsers.isEmpty()) {
			log.debug("No expired deleted users found");
			return;
		}

		log.info("Found {} users to permanently delete (deleted 30+ days ago)", expiredUsers.size());

		for (User user : expiredUsers) {
			try {
				permanentlyDeleteUser(user);
			} catch (Exception e) {
				log.error("Error permanently deleting user {}: {}", user.getId(), e.getMessage(), e);
			}
		}

		log.info("Permanently deleted {} users", expiredUsers.size());
	}

	@Transactional
	private void permanentlyDeleteUser(User user) {
		UUID userId = user.getId();

		fcmTokenRepository.deleteAllByUser_Id(userId);
		notificationRepository.deleteAllByUser_Id(userId);

		userRepository.delete(user);
		log.debug("Permanently deleted user: {}", userId);
	}

	private int safeCastToInt(long value) {
		if (value > Integer.MAX_VALUE) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "Help count exceeds maximum limit");
		}
		return (int) value;
	}

	private User getUser(UUID userId, String errorMessage) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, errorMessage));
	}
}
