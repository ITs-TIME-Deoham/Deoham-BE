package com.deoham.user.service;

import com.deoham.user.entity.User;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface UserWriteService {
	User updateProfile(UUID userId, String nickname, MultipartFile profileImage);

	void updateHelpCounts(UUID requesterId, UUID applicantId);

	void deleteUser(UUID userId);
}
