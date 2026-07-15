package com.deoham.user.service.impl;

import com.deoham.card.entity.CardApplyStatus;
import com.deoham.card.entity.CardStatus;
import com.deoham.card.repository.CardApplyRepository;
import com.deoham.card.repository.CardRepository;
import com.deoham.global.config.S3Properties;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.global.metrics.MetricsRegistry;
import com.deoham.user.entity.User;
import com.deoham.user.repository.UserRepository;
import com.deoham.user.repository.UserSocialAccountRepository;
import com.deoham.user.service.UserWriteService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
@Transactional
public class UserWriteServiceImpl implements UserWriteService {

    private final UserRepository userRepository;
	private final CardRepository cardRepository;
	private final CardApplyRepository cardApplyRepository;
	private final UserSocialAccountRepository userSocialAccountRepository;
	private final MetricsRegistry metricsRegistry;
	private final S3Client s3Client;
	private final S3Properties s3Properties;

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
            try {
                profileImageUrl = uploadProfileImageToS3(userId, profileImage);
            } catch (Exception e) {
                // S3 업로드 실패해도 계속 진행
                // profileImageUrl은 null로 유지되어 기존 이미지 유지
            }
        }

        user.updateProfile(nickname, profileImageUrl);
        return userRepository.save(user);
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
			user.delete();
			userRepository.save(user);
			userSocialAccountRepository.deleteAllByUser_Id(userId);
		});
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
