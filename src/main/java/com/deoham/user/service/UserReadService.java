package com.deoham.user.service;

import com.deoham.user.dto.UserMeResponse;
import java.util.UUID;

public interface UserReadService {

    UserMeResponse getMe(UUID userId);
}
