package com.deoham.notification.service.impl;

import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.notification.dto.FcmTokenRegisterRequest;
import com.deoham.notification.entity.FcmToken;
import com.deoham.notification.repository.FcmTokenRepository;
import com.deoham.notification.service.FcmTokenService;
import com.deoham.user.entity.User;
import com.deoham.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FcmTokenServiceImpl implements FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    /**
     * FCM 토큰 upsert.
     * <ol>
     *   <li>같은 토큰이 이미 존재하면(계정 전환/토큰 재사용) 소유자·기기 정보를 재할당한다.</li>
     *   <li>같은 기기(user_id, device_id)의 기존 토큰이 있으면(rotation) 토큰 값만 교체한다.</li>
     *   <li>둘 다 없으면 신규 등록한다.</li>
     * </ol>
     */
    @Override
    @Transactional
    public void register(UUID userId, FcmTokenRegisterRequest request) {
        String token = request.token();
        String deviceId = request.deviceId();

        Optional<FcmToken> byToken = fcmTokenRepository.findByToken(token);
        if (byToken.isPresent()) {
            FcmToken existing = byToken.get();
            // 대상 유저가 같은 기기 슬롯에 다른 토큰 행을 이미 갖고 있으면 유니크(user_id, device_id) 충돌을 막기 위해 제거
            fcmTokenRepository.findByUser_IdAndDeviceId(userId, deviceId)
                    .filter(other -> !other.getId().equals(existing.getId()))
                    .ifPresent(other -> {
                        fcmTokenRepository.delete(other);
                        fcmTokenRepository.flush();
                    });
            existing.reassignTo(requireUser(userId), deviceId, request.platform());
            log.debug("FCM 토큰 재할당 [userId={}, deviceId={}]", userId, deviceId);
            return;
        }

        Optional<FcmToken> byDevice = fcmTokenRepository.findByUser_IdAndDeviceId(userId, deviceId);
        if (byDevice.isPresent()) {
            byDevice.get().updateToken(token, request.platform());
            log.debug("FCM 토큰 갱신 [userId={}, deviceId={}]", userId, deviceId);
            return;
        }

        fcmTokenRepository.save(FcmToken.builder()
                .user(requireUser(userId))
                .token(token)
                .deviceId(deviceId)
                .platform(request.platform())
                .build());
        log.debug("FCM 토큰 신규 등록 [userId={}, deviceId={}]", userId, deviceId);
    }

    @Override
    @Transactional
    public void delete(UUID userId, String token) {
        fcmTokenRepository.findByToken(token)
                .filter(fcmToken -> fcmToken.getUser().getId().equals(userId))
                .ifPresent(fcmTokenRepository::delete);
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다"));
    }
}
