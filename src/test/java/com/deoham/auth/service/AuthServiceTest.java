package com.deoham.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deoham.auth.client.KakaoOAuthClient;
import com.deoham.auth.client.KakaoTokenResponse;
import com.deoham.auth.client.KakaoUserInfo;
import com.deoham.auth.client.KakaoUserInfo.KakaoAccount;
import com.deoham.auth.dto.KakaoCallbackResponse;
import com.deoham.auth.dto.TokenResponse;
import com.deoham.auth.entity.OAuthState;
import com.deoham.auth.repository.OAuthStateRepository;
import com.deoham.global.config.KakaoOAuthProperties;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.security.jwt.JwtProperties;
import com.deoham.global.security.jwt.JwtTokenProvider;
import com.deoham.user.entity.OauthProvider;
import com.deoham.user.entity.User;
import com.deoham.user.entity.UserSocialAccount;
import com.deoham.user.repository.UserRepository;
import com.deoham.user.repository.UserSocialAccountRepository;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock KakaoOAuthClient kakaoOAuthClient;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock UserRepository userRepository;
    @Mock UserSocialAccountRepository userSocialAccountRepository;
    @Mock OAuthStateRepository oauthStateRepository;

    AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                kakaoOAuthClient,
                new KakaoOAuthProperties("rest-key", "http://localhost:3000/api/auth/kakao/callback"),
                jwtTokenProvider,
                new JwtProperties("01234567890123456789012345678901", 3600, 1209600),
                userRepository,
                userSocialAccountRepository,
                oauthStateRepository);
    }

    @Test
    void kakaoAuthorizationUri_savesState_andAddsStateParameter() {
        ArgumentCaptor<OAuthState> stateCaptor = ArgumentCaptor.forClass(OAuthState.class);

        URI uri = authService.kakaoAuthorizationUri();

        verify(oauthStateRepository).save(stateCaptor.capture());
        String state = stateCaptor.getValue().getState();
        assertThat(state).isNotBlank();
        assertThat(uri.toString()).contains("state=" + state);
        assertThat(uri.toString()).contains("client_id=rest-key");
    }

    @Test
    void kakaoLogin_usesExistingSocialAccount_andStoresRefreshToken() {
        User user = user(UUID.randomUUID(), "old-user");
        UserSocialAccount socialAccount = socialAccount(user, "123456", "old@example.com");
        KakaoUserInfo userInfo = kakaoUserInfo(123456L, "new@example.com");

        when(oauthStateRepository.findByStateAndProvider("state-1", OauthProvider.KAKAO))
                .thenReturn(Optional.of(new OAuthState("state-1", OauthProvider.KAKAO, Instant.now().plusSeconds(60))));
        when(kakaoOAuthClient.exchangeCode("code-1"))
                .thenReturn(new KakaoTokenResponse("bearer", "kakao-access", 3600, null, 0));
        when(kakaoOAuthClient.getUserInfo("kakao-access")).thenReturn(userInfo);
        when(userSocialAccountRepository.findByProviderAndProviderUid(OauthProvider.KAKAO, "123456"))
                .thenReturn(Optional.of(socialAccount));
        when(jwtTokenProvider.generateAccessToken(user.getId(), "new@example.com", "USER")).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(user.getId())).thenReturn("refresh-token");

        KakaoCallbackResponse response = authService.kakaoLogin("code-1", "state-1");

        assertThat(response.isNewUser()).isFalse();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(socialAccount.getProviderEmail()).isEqualTo("new@example.com");
        assertThat(socialAccount.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(socialAccount.getTokenExpiresAt()).isAfter(Instant.now());
        verify(oauthStateRepository).delete(any(OAuthState.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void kakaoLogin_createsNewUserWithDefaultNickname_withoutKakaoProfileScope() {
        KakaoUserInfo userInfo = kakaoUserInfo(123456L, "new@example.com");
        User savedUser = user(UUID.randomUUID(), "kakao_123456");

        when(oauthStateRepository.findByStateAndProvider("state-1", OauthProvider.KAKAO))
                .thenReturn(Optional.of(new OAuthState("state-1", OauthProvider.KAKAO, Instant.now().plusSeconds(60))));
        when(kakaoOAuthClient.exchangeCode("code-1"))
                .thenReturn(new KakaoTokenResponse("bearer", "kakao-access", 3600, null, 0));
        when(kakaoOAuthClient.getUserInfo("kakao-access")).thenReturn(userInfo);
        when(userSocialAccountRepository.findByProviderAndProviderUid(OauthProvider.KAKAO, "123456"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByNickname("kakao_123456")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userSocialAccountRepository.save(any(UserSocialAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.generateAccessToken(savedUser.getId(), "new@example.com", "USER")).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(savedUser.getId())).thenReturn("refresh-token");

        KakaoCallbackResponse response = authService.kakaoLogin("code-1", "state-1");

        assertThat(response.isNewUser()).isTrue();
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getNickname()).isEqualTo("kakao_123456");
        assertThat(userCaptor.getValue().getProfileImageUrl()).isNull();
    }

    @Test
    void kakaoLogin_rejectsUnknownState_beforeCallingKakao() {
        when(oauthStateRepository.findByStateAndProvider("bad-state", OauthProvider.KAKAO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.kakaoLogin("code-1", "bad-state"))
                .isInstanceOf(BusinessException.class);

        verify(kakaoOAuthClient, never()).exchangeCode(any());
    }

    @Test
    void refresh_requiresStoredRefreshToken_andRotatesIt() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, "refresh-user");
        UserSocialAccount socialAccount = socialAccount(user, "123456", "user@example.com");
        socialAccount.updateRefreshToken("old-refresh", Instant.now().plusSeconds(60));
        Jwt jwt = refreshJwt("old-refresh", userId);

        when(jwtTokenProvider.parseToken("old-refresh")).thenReturn(jwt);
        when(userSocialAccountRepository.findByRefreshToken("old-refresh")).thenReturn(Optional.of(socialAccount));
        when(jwtTokenProvider.generateAccessToken(userId, "user@example.com", "USER")).thenReturn("new-access");
        when(jwtTokenProvider.generateRefreshToken(userId)).thenReturn("new-refresh");

        TokenResponse response = authService.refresh("old-refresh");

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        assertThat(socialAccount.getRefreshToken()).isEqualTo("new-refresh");
        verify(jwtTokenProvider).generateAccessToken(eq(userId), eq("user@example.com"), eq("USER"));
    }

    @Test
    void logout_revokesStoredRefreshTokensForCurrentUser() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, "logout-user");
        UserSocialAccount socialAccount = socialAccount(user, "123456", "user@example.com");
        socialAccount.updateRefreshToken("refresh-token", Instant.now().plusSeconds(60));
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(accessJwt("access-token", userId));

        when(userSocialAccountRepository.findAllByUser_Id(userId)).thenReturn(List.of(socialAccount));

        authService.logout(authentication);

        assertThat(socialAccount.getRefreshToken()).isNull();
        assertThat(socialAccount.getTokenExpiresAt()).isNull();
    }

    private static User user(UUID id, String nickname) {
        User user = User.builder()
                .nickname(nickname)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static UserSocialAccount socialAccount(User user, String providerUid, String email) {
        return UserSocialAccount.builder()
                .user(user)
                .provider(OauthProvider.KAKAO)
                .providerUid(providerUid)
                .providerEmail(email)
                .build();
    }

    private static KakaoUserInfo kakaoUserInfo(Long id, String email) {
        return new KakaoUserInfo(id, new KakaoAccount(email, true));
    }

    private static Jwt refreshJwt(String tokenValue, UUID userId) {
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "HS256")
                .subject(userId.toString())
                .claim("type", "refresh")
                .issuedAt(Instant.now().minusSeconds(10))
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }

    private static Jwt accessJwt(String tokenValue, UUID userId) {
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "HS256")
                .subject(userId.toString())
                .claim("type", "access")
                .claim("role", "USER")
                .issuedAt(Instant.now().minusSeconds(10))
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }
}
