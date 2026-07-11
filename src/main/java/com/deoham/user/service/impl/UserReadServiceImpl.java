package com.deoham.user.service.impl;

import com.deoham.auth.dto.ProfileResponse;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.global.metrics.MetricsRegistry;
import com.deoham.user.entity.User;
import com.deoham.user.repository.UserRepository;
import com.deoham.user.service.UserReadService;
import io.micrometer.core.instrument.Timer;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserReadServiceImpl implements UserReadService {

    private final UserRepository userRepository;
    private final MetricsRegistry metricsRegistry;

	@Override
	public ProfileResponse getProfile(UUID userId) {
		Timer.Sample sample = metricsRegistry.startUserProfileTimer();
		try {
			User user = userRepository.findById(userId)
					.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

			ProfileResponse response = ProfileResponse.from(user);
			metricsRegistry.recordUserProfileSuccess(sample);
			return response;
		} catch (Exception exception) {
			metricsRegistry.recordUserProfileFailure(sample, exception);
			throw exception;
		}
	}
}
