package com.deoham.global.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

	private final JwtProperties jwtProperties;

	@PostConstruct
	void validateSecretLength() {
		int secretBytes = jwtProperties.secret().getBytes().length;
		if (secretBytes < 32) {
			throw new IllegalArgumentException(
					"JWT_SECRET must be at least 32 characters (256 bits) for HS256. Current length: " + secretBytes);
		}
	}

	public String generateAccessToken(UUID userId, String email, String role) {
		return generateToken(userId, email, role, "access", jwtProperties.accessTokenExpirySeconds());
	}

	public String generateRefreshToken(UUID userId) {
		return generateToken(userId, null, null, "refresh", jwtProperties.refreshTokenExpirySeconds());
	}

	public org.springframework.security.oauth2.jwt.Jwt parseToken(String token) {
		try {
			com.nimbusds.jwt.SignedJWT signedJWT = com.nimbusds.jwt.SignedJWT.parse(token);
			if (!JWSAlgorithm.HS256.equals(signedJWT.getHeader().getAlgorithm())) {
				throw new org.springframework.security.oauth2.jwt.JwtException("Unsupported JWS algorithm");
			}
			boolean verified = signedJWT.verify(
					new com.nimbusds.jose.crypto.MACVerifier(jwtProperties.secret().getBytes()));
			if (!verified) {
				throw new org.springframework.security.oauth2.jwt.JwtException("Invalid JWT signature");
			}

			JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
			java.util.Date iat = claims.getIssueTime();
			java.util.Date exp = claims.getExpirationTime();
			if (iat == null || exp == null) {
				throw new org.springframework.security.oauth2.jwt.JwtException("Missing required JWT claims (iat or exp)");
			}
			return new org.springframework.security.oauth2.jwt.Jwt(
					token,
					java.time.Instant.ofEpochSecond(iat.getTime() / 1000),
					java.time.Instant.ofEpochSecond(exp.getTime() / 1000),
					signedJWT.getHeader().toJSONObject(),
					claims.getClaims()
			);
		} catch (Exception e) {
			throw new org.springframework.security.oauth2.jwt.JwtException("Failed to parse token", e);
		}
	}

	private String generateToken(UUID userId, String email, String role, String type, long expirySeconds) {
		try {
			Instant now = Instant.now();
			Instant expiresAt = now.plusSeconds(expirySeconds);

			JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
					.subject(userId.toString())
					.issueTime(java.util.Date.from(now))
					.expirationTime(java.util.Date.from(expiresAt))
					.claim("type", type)
					.claim("email", email)
					.claim("role", role)
					.build();

			SignedJWT signedJWT = new SignedJWT(
					new JWSHeader(JWSAlgorithm.HS256),
					claimsSet
			);

			signedJWT.sign(new MACSigner(jwtProperties.secret().getBytes()));
			return signedJWT.serialize();
		} catch (JOSEException e) {
			throw new RuntimeException("JWT 생성 실패", e);
		}
	}
}
