package com.deoham.user.service.impl;

import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.user.dto.UserMeResponse;
import com.deoham.user.entity.User;
import com.deoham.user.repository.UserRepository;
import com.deoham.user.service.UserReadService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserReadServiceImpl implements UserReadService {

    private final UserRepository userRepository;

    @Override
    public UserMeResponse getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User not found"));

        return new UserMeResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getGender() == null ? null : user.getGender().name(),
                user.getAge()
        );
    }
}
