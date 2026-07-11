package com.deoham.global.security;

import com.deoham.user.entity.UserRole;
import java.util.UUID;

public record AuthPrincipal(UUID userId, String email, UserRole role) {
}
