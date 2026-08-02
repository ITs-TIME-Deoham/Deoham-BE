package com.deoham.notification.service;

import com.deoham.notification.dto.FcmTokenRegisterRequest;
import java.util.UUID;

public interface FcmTokenService {

    void register(UUID userId, FcmTokenRegisterRequest request);

    void delete(UUID userId, String token);
}
