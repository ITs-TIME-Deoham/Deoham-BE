package com.deoham.chat.translation;

import com.deoham.global.config.DeepLProperties;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Primary
@Profile("!test")
@Component
public class DeepLTranslationProvider implements TranslationProvider {

	private static final String PROVIDER_NAME = "DEEPL";

	private final RestClient restClient;
	private final DeepLProperties properties;

	public DeepLTranslationProvider(@Qualifier("deepl") RestClient restClient, DeepLProperties properties) {
		this.restClient = restClient;
		this.properties = properties;
	}

	@Override
	public TranslationResult translate(String text, String targetLanguage) {
		// DeepL 은 target_lang 에 대문자 언어 코드(EN, KO, JA ...)를 요구한다.
		String targetLang = targetLanguage.toUpperCase(Locale.ROOT);

		DeepLTranslateResponse response;
		try {
			response = restClient.post()
					.uri("/v2/translate")
					.header("Authorization", "DeepL-Auth-Key " + properties.apiKey())
					.contentType(MediaType.APPLICATION_JSON)
					.body(DeepLTranslateRequest.of(text, targetLang))
					.retrieve()
					.body(DeepLTranslateResponse.class);
		} catch (RestClientResponseException e) {
			log.warn("DeepL translation request failed. Status: {}, Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "번역 요청 처리 중 오류가 발생했습니다.");
		}

		String translatedText = response != null ? response.firstText() : null;
		if (translatedText == null || translatedText.isBlank()) {
			log.warn("DeepL returned no translation for target language {}", targetLang);
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "번역 결과를 받지 못했습니다.");
		}

		return new TranslationResult(translatedText.trim(), PROVIDER_NAME);
	}

	@Override
	public String getProviderName() {
		return PROVIDER_NAME;
	}
}
