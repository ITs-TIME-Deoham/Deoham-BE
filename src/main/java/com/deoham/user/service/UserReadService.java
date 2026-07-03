package com.deoham.user.service;

import com.deoham.auth.dto.ProfileResponse;
import java.util.UUID;

public interface UserReadService {
	ProfileResponse getProfile(UUID userId);
}
