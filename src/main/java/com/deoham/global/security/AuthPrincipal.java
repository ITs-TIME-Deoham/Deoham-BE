package com.deoham.global.security;

import java.util.UUID;

public record AuthPrincipal(UUID userId, String email, String role, String provider, String name, String providerId) {
}
