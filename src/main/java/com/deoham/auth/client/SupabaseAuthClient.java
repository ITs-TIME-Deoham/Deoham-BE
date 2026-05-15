package com.deoham.auth.client;

import com.deoham.global.config.SupabaseAuthProperties;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class SupabaseAuthClient {

    private final RestClient restClient;

    public SupabaseAuthClient(SupabaseAuthProperties props, RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl(props.url())
                .defaultHeader("apikey", props.anonKey())
                .build();
    }

    public SupabaseTokenResponse signup(String email, String password) {
        return restClient.post()
                .uri("/auth/v1/signup")
                .body(Map.of("email", email, "password", password))
                .retrieve()
                .onStatus(
                        status -> status.value() == 422,
                        (req, res) -> {
                            throw new BusinessException(ErrorCode.CONFLICT, "이미 가입된 이메일입니다.");
                        })
                .onStatus(
                        status -> status.is4xxClientError(),
                        (req, res) -> {
                            log.warn("Supabase signup error: {} {}", res.getStatusCode(), email);
                            throw new BusinessException(ErrorCode.INVALID_REQUEST, "회원가입에 실패했습니다.");
                        })
                .body(SupabaseTokenResponse.class);
    }

    public SupabaseTokenResponse login(String email, String password) {
        return restClient.post()
                .uri("/auth/v1/token?grant_type=password")
                .body(Map.of("email", email, "password", password))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError(),
                        (req, res) -> {
                            throw new BusinessException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
                        })
                .body(SupabaseTokenResponse.class);
    }
}
