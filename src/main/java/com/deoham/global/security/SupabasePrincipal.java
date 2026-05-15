package com.deoham.global.security;

import java.util.UUID;

public record SupabasePrincipal(UUID userId, String email, String role, String provider, String name) {
}
