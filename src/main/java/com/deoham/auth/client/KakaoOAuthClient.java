package com.deoham.auth.client;

import com.deoham.global.config.KakaoOAuthProperties;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
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
		body.add("client_secret", props.clientSecret());
		body.add("redirect_uri", props.redirectUri());
		body.add("code", code);

		try {
			return tokenClient.post()
					.uri("/oauth/token")
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(body)
					.retrieve()
					.body(KakaoTokenResponse.class);
		} catch (RestClientResponseException e) {
			log.warn("Kakao token exchange failed. Status: {}, Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "카카오 인가 코드가 유효하지 않습니다. (redirect_uri 불일치, 코드 만료 또는 재사용)");
		}
	}

	public KakaoUserInfo getUserInfo(String kakaoAccessToken) {
		try {
			return apiClient.get()
					.uri("/v2/user/me")
					.header("Authorization", "Bearer " + kakaoAccessToken)
					.retrieve()
					.body(KakaoUserInfo.class);
		} catch (RestClientResponseException e) {
			log.warn("Kakao user info failed. Status: {}, Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "카카오 사용자 정보 조회에 실패했습니다.");
		}
	}
}
