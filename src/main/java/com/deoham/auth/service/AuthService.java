package com.deoham.auth.service;

import com.deoham.auth.client.KakaoOAuthClient;
import com.deoham.auth.client.KakaoTokenResponse;
import com.deoham.auth.client.KakaoUserInfo;
import com.deoham.auth.dto.KakaoCallbackResponse;
import com.deoham.auth.dto.TokenResponse;
import com.deoham.auth.entity.OAuthState;
import com.deoham.auth.repository.OAuthStateRepository;
import com.deoham.global.config.KakaoOAuthProperties;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.global.security.AuthenticationUtils;
import com.deoham.global.security.jwt.JwtProperties;
import com.deoham.global.security.jwt.JwtTokenProvider;
import com.deoham.user.entity.OauthProvider;
import com.deoham.user.entity.User;
import com.deoham.user.entity.UserSocialAccount;
import com.deoham.user.repository.UserRepository;
import com.deoham.user.repository.UserSocialAccountRepository;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long OAUTH_STATE_EXPIRY_SECONDS = 300;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final KakaoOAuthClient kakaoOAuthClient;
    private final KakaoOAuthProperties kakaoOAuthProperties;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;
    private final UserSocialAccountRepository userSocialAccountRepository;
    private final OAuthStateRepository oauthStateRepository;

    @Transactional
    public URI kakaoAuthorizationUri() {
        oauthStateRepository.deleteByExpiresAtBefore(Instant.now());

        String state = generateState();
        oauthStateRepository.save(new OAuthState(
                state,
                OauthProvider.KAKAO,
                Instant.now().plusSeconds(OAUTH_STATE_EXPIRY_SECONDS)));

        return UriComponentsBuilder.fromUriString("https://kauth.kakao.com/oauth/authorize")
                .queryParam("client_id", kakaoOAuthProperties.restApiKey())
                .queryParam("redirect_uri", kakaoOAuthProperties.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", "account_email")
                .queryParam("state", state)
                .build()
                .toUri();
    }

    @Transactional
    public KakaoCallbackResponse kakaoLogin(String code, String state) {
        consumeOAuthState(state, OauthProvider.KAKAO);

        KakaoTokenResponse kakaoToken = kakaoOAuthClient.exchangeCode(code);
        KakaoUserInfo userInfo = kakaoOAuthClient.getUserInfo(kakaoToken.accessToken());
        String kakaoId = userInfo.id().toString();

        UserSocialAccount socialAccount = userSocialAccountRepository
                .findByProviderAndProviderUid(OauthProvider.KAKAO, kakaoId)
                .orElse(null);

        boolean isNewUser = socialAccount == null;
        User user;
        if (isNewUser) {
            user = userRepository.save(User.builder()
                    .nickname(generateDefaultNickname(kakaoId))
                    .build());

            socialAccount = userSocialAccountRepository.save(UserSocialAccount.builder()
                    .user(user)
                    .provider(OauthProvider.KAKAO)
                    .providerUid(kakaoId)
                    .providerEmail(userInfo.email())
                    .build());
        } else {
            user = socialAccount.getUser();
            socialAccount.updateProviderEmail(userInfo.email());
        }

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), userInfo.email(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        socialAccount.updateRefreshToken(
                refreshToken,
                Instant.now().plusSeconds(jwtProperties.refreshTokenExpirySeconds()));

        return new KakaoCallbackResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtProperties.accessTokenExpirySeconds(),
                isNewUser);
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        Jwt jwt;
        try {
            jwt = jwtTokenProvider.parseToken(refreshToken);
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid refresh token.");
        }
        if (!"refresh".equals(jwt.getClaimAsString("type"))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Token is not a refresh token.");
        }

        UUID userId = UUID.fromString(jwt.getSubject());
        UserSocialAccount socialAccount = userSocialAccountRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token is not registered."));
        if (!socialAccount.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token user does not match.");
        }
        if (socialAccount.getTokenExpiresAt() == null || !socialAccount.getTokenExpiresAt().isAfter(Instant.now())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token has expired.");
        }

        User user = socialAccount.getUser();
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), socialAccount.getProviderEmail(), user.getRole().name());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        socialAccount.updateRefreshToken(
                newRefreshToken,
                Instant.now().plusSeconds(jwtProperties.refreshTokenExpirySeconds()));

        return new TokenResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                jwtProperties.accessTokenExpirySeconds());
    }

    @Transactional
    public void logout(Authentication authentication) {
        UUID userId = AuthenticationUtils.fromAuthentication(authentication)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Authentication required."))
                .userId();
        userSocialAccountRepository.findAllByUser_Id(userId)
                .forEach(UserSocialAccount::revokeRefreshToken);
    }

    private void consumeOAuthState(String state, OauthProvider provider) {
        OAuthState oauthState = oauthStateRepository.findByStateAndProvider(state, provider)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid OAuth state."));
        oauthStateRepository.delete(oauthState);
        if (oauthState.isExpired(Instant.now())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "OAuth state has expired.");
        }
    }

    private static String generateState() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateDefaultNickname(String kakaoId) {
        String base = "kakao_" + kakaoId;
        if (!userRepository.existsByNickname(base)) {
            return base;
        }
        for (int suffix = 1; suffix < 1000; suffix++) {
            String candidate = base + "_" + suffix;
            if (!userRepository.existsByNickname(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException(ErrorCode.CONFLICT, "Could not generate a default nickname.");
    }
}
