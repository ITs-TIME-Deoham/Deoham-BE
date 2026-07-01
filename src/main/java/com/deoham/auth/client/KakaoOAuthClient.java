package com.deoham.auth.client;

import com.deoham.global.config.KakaoOAuthProperties;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KakaoOAuthClient {

    private final RestClient tokenClient;
    private final RestClient apiClient;
    private final KakaoOAuthProperties props;

    public KakaoOAuthClient(KakaoOAuthProperties props) {
        this.props = props;
        this.tokenClient = RestClient.builder()
                .baseUrl("https://kauth.kakao.com")
                .build();
        this.apiClient = RestClient.builder()
                .baseUrl("https://kapi.kakao.com")
                .build();
    }

    public KakaoTokenResponse exchangeCode(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", props.restApiKey());
        body.add("redirect_uri", props.redirectUri());
        body.add("code", code);

        return tokenClient.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError(),
                        (req, res) -> {
                            throw new BusinessException(ErrorCode.UNAUTHORIZED, "카카오 인가 코드가 유효하지 않습니다.");
                        })
                .body(KakaoTokenResponse.class);
    }

    public KakaoUserInfo getUserInfo(String kakaoAccessToken) {
        return apiClient.get()
                .uri("/v2/user/me")
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError(),
                        (req, res) -> {
                            throw new BusinessException(ErrorCode.UNAUTHORIZED, "카카오 사용자 정보 조회에 실패했습니다.");
                        })
                .body(KakaoUserInfo.class);
    }
}
