package com.deoham.user.service;

import com.deoham.global.security.SupabasePrincipal;
import com.deoham.user.dto.UserMeResponse;
import com.deoham.user.entity.AuthProvider;
import com.deoham.user.entity.User;
import com.deoham.user.entity.UserAuth;
import com.deoham.user.repository.UserAuthRepository;
import com.deoham.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;

    @Transactional
    public UserMeResponse getOrCreateMe(SupabasePrincipal principal) {
        AuthProvider authProvider = resolveProvider(principal.provider());
        String providerUid = principal.userId().toString();

        UserAuth userAuth = userAuthRepository
                .findByProviderAndProviderUid(authProvider, providerUid)
                .orElseGet(() -> createUser(principal, authProvider, providerUid));

        return toResponse(userAuth.getUser());
    }

    private UserAuth createUser(SupabasePrincipal principal, AuthProvider provider, String providerUid) {
        User user = userRepository.save(User.builder()
                .email(principal.email())
                .name(principal.name())
                .build());

        return userAuthRepository.save(UserAuth.builder()
                .user(user)
                .provider(provider)
                .providerUid(providerUid)
                .build());
    }

    private AuthProvider resolveProvider(String supabaseProvider) {
        return switch (supabaseProvider != null ? supabaseProvider.toUpperCase() : "") {
            case "KAKAO" -> AuthProvider.KAKAO;
            case "GOOGLE" -> AuthProvider.GOOGLE;
            case "APPLE" -> AuthProvider.APPLE;
            default -> AuthProvider.EMAIL;
        };
    }

    private UserMeResponse toResponse(User user) {
        return new UserMeResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getJobType(),
                user.getPhone(),
                user.getPlanType(),
                user.isNotiNewCard(),
                user.isNotiLinkViewed(),
                user.isNotiCounterpartConfirmed()
        );
    }
}
