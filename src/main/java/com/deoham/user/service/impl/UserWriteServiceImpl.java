package com.deoham.user.service.impl;

import com.deoham.card.entity.CardApplyStatus;
import com.deoham.card.entity.CardStatus;
import com.deoham.card.repository.CardApplyRepository;
import com.deoham.card.repository.CardRepository;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.user.entity.User;
import com.deoham.user.repository.UserRepository;
import com.deoham.user.repository.UserSocialAccountRepository;
import com.deoham.user.service.UserWriteService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserWriteServiceImpl implements UserWriteService {

    private final UserRepository userRepository;
	private final CardRepository cardRepository;
	private final CardApplyRepository cardApplyRepository;
	private final UserSocialAccountRepository userSocialAccountRepository;

    @Override
    public User updateProfile(UUID userId, String nickname, String profileImageUrl) {
        User user = getUser(userId, "User not found.");

        if (nickname != null && !nickname.equals(user.getNickname())) {
            if (userRepository.existsByNickname(nickname)) {
                throw new BusinessException(ErrorCode.CONFLICT, "Nickname already exists.");
            }
        }

        user.updateProfile(nickname, profileImageUrl);
        return userRepository.save(user);
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
		User user = getUser(userId, "User not found.");
		user.delete();
		userRepository.save(user);
		userSocialAccountRepository.deleteAllByUser_Id(userId);
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
