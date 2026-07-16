package com.deoham.chat.translation;

import com.deoham.global.config.GeminiProperties;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Primary
@Profile("!test")
@Component
public class GeminiTranslationProvider implements TranslationProvider {

	private static final String PROVIDER_NAME = "GEMINI";
	private static final String BASE_URL = "https://generativelanguage.googleapis.com";

	private final RestClient restClient;
	private final GeminiProperties properties;

	public GeminiTranslationProvider(@Qualifier("gemini") RestClient.Builder restClientBuilder, GeminiProperties properties) {
		this.properties = properties;
		this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
	}

	@Override
	public TranslationResult translate(String text, String targetLanguage) {
		String prompt = """
				Translate the text below into %s.
				Respond with only the translated text, with no explanations, labels or quotes.

				Text: %s
				""".formatted(targetLanguage, text);

		GeminiGenerateContentResponse response;
		try {
			response = restClient.post()
					.uri("/v1beta/models/{model}:generateContent", properties.model())
					.header("x-goog-api-key", properties.apiKey())
					.body(GeminiGenerateContentRequest.ofPrompt(prompt))
					.retrieve()
					.body(GeminiGenerateContentResponse.class);
		} catch (RestClientResponseException e) {
			log.warn("Gemini translation request failed. Status: {}, Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "번역 요청 처리 중 오류가 발생했습니다.");
		}

		String translatedText = response != null ? response.firstText() : null;
		if (translatedText == null || translatedText.isBlank()) {
			log.warn("Gemini returned no translation candidates for target language {}", targetLanguage);
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "번역 결과를 받지 못했습니다.");
		}

		String modelVersion = response.modelVersion() != null ? response.modelVersion() : properties.model();
		return new TranslationResult(translatedText.trim(), modelVersion);
	}

	@Override
	public String getProviderName() {
		return PROVIDER_NAME;
	}
}
