package com.deoham.auth.service;

import com.deoham.auth.client.SupabaseAuthClient;
import com.deoham.auth.client.SupabaseTokenResponse;
import com.deoham.auth.dto.LoginRequest;
import com.deoham.auth.dto.SignupRequest;
import com.deoham.auth.dto.TokenResponse;
import com.deoham.global.config.SupabaseAuthProperties;
import com.deoham.user.entity.AuthProvider;
import com.deoham.user.entity.User;
import com.deoham.user.entity.UserAuth;
import com.deoham.user.repository.UserAuthRepository;
import com.deoham.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SupabaseAuthClient supabaseAuthClient;
    private final SupabaseAuthProperties supabaseAuthProperties;
    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;

    @Transactional
    public TokenResponse signup(SignupRequest request) {
        SupabaseTokenResponse supabaseResponse = supabaseAuthClient.signup(request.email(), request.password());

        User user = userRepository.save(User.builder()
                .email(request.email())
                .name(request.name())
                .build());

        userAuthRepository.save(UserAuth.builder()
                .user(user)
                .provider(AuthProvider.EMAIL)
                .providerUid(supabaseResponse.user().id())
                .build());

        return toTokenResponse(supabaseResponse);
    }

    public TokenResponse login(LoginRequest request) {
        SupabaseTokenResponse supabaseResponse = supabaseAuthClient.login(request.email(), request.password());
        return toTokenResponse(supabaseResponse);
    }

    public URI kakaoRedirectUri() {
        return UriComponentsBuilder.fromUriString(supabaseAuthProperties.url())
                .path("/auth/v1/authorize")
                .queryParam("provider", "kakao")
                .queryParam("redirect_to", supabaseAuthProperties.redirectUrl())
                .build()
                .toUri();
    }

    private TokenResponse toTokenResponse(SupabaseTokenResponse r) {
        return new TokenResponse(r.accessToken(), r.refreshToken(), r.tokenType(), r.expiresIn());
    }
}
