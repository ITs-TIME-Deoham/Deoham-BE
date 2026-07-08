package com.deoham.user.service;

import com.deoham.user.entity.User;
import java.util.UUID;

public interface UserWriteService {
	User updateProfile(UUID userId, String nickname, String profileImageUrl);

	void updateHelpCounts(UUID requesterId, UUID applicantId);

	void deleteUser(UUID userId);

	void deletePermanentlyExpiredAccounts();
}
