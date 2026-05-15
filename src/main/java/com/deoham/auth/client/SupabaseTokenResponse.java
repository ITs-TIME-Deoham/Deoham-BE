package com.deoham.auth.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SupabaseTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("user") SupabaseUser user
) {
    public record SupabaseUser(
            @JsonProperty("id") String id, @JsonProperty("email") String email) {
    }
}
