package com.deoham.chat.translation;

import com.deoham.chat.translation.dto.DeepLTranslateRequest;
import com.deoham.chat.translation.dto.DeepLTranslateResponse;
import com.deoham.chat.translation.dto.TranslationResult;
import com.deoham.global.config.DeepLProperties;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Profile("!test")
@Component
public class DeepLTranslationProvider implements TranslationProvider {

	private static final String PROVIDER_NAME = "DEEPL";

	/**
	 * DeepL 이 target_lang 으로 허용하는 지역 변형 코드. 이 집합에 없는 지역 코드는
	 * 기본 언어로 축약한다. (예: KO/JA 는 지역 변형을 지원하지 않아 KO-KR → KO 로 처리)
	 */
	private static final Set<String> DEEPL_REGIONAL_TARGETS =
			Set.of("EN-GB", "EN-US", "PT-BR", "PT-PT", "ZH-HANS", "ZH-HANT", "ES-419");

	private final RestClient restClient;
	private final DeepLProperties properties;

	public DeepLTranslationProvider(@Qualifier("deepl") RestClient restClient, DeepLProperties properties) {
		this.restClient = restClient;
		this.properties = properties;
	}

	@Override
	public TranslationResult translate(String text, TargetLanguage targetLanguage) {
		String targetLang = normalizeTargetLang(targetLanguage.code());

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
			log.warn("DeepL translation request failed. targetLang={} (요청 언어 {}), Status: {}, Response: {}",
					targetLang, targetLanguage.code(), e.getStatusCode(), e.getResponseBodyAsString());
			// 400 = target_lang 미지원 등 클라이언트 입력 문제 → 인증/서버 오류(401/403/456/5xx)와 구분
			if (e.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
				throw new BusinessException(ErrorCode.INVALID_REQUEST,
						"지원하지 않는 번역 대상 언어입니다: " + targetLanguage.code());
			}
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "번역 요청 처리 중 오류가 발생했습니다.");
		}

		String translatedText = response != null ? response.firstText() : null;
		if (!StringUtils.hasText(translatedText)) {
			log.warn("DeepL returned no translation for target language {}", targetLang);
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "번역 결과를 받지 못했습니다.");
		}

		return new TranslationResult(translatedText.trim(), PROVIDER_NAME, PROVIDER_NAME);
	}

	/**
	 * {@link TargetLanguage#code()}를 DeepL target_lang 형식으로 정규화한다.
	 * 대문자화 후, DeepL 이 지원하는 지역 변형(EN-US 등)은 그대로 두고
	 * 그 외 지역 서브태그는 제거한다. (예: ko-KR → KO, en-US → EN-US, zh-hans → ZH-HANS)
	 *
	 * <p>이제 입력이 화이트리스트를 거친 값뿐이라 이 집합과 {@link TargetLanguage} 사이에
	 * 매핑이 일부 겹친다. 통합 여부는 이슈 #133에서 미결정으로 남겨 두었다.
	 */
	private static String normalizeTargetLang(String targetLanguage) {
		String code = targetLanguage.trim().toUpperCase(Locale.ROOT).replace('_', '-');
		if (DEEPL_REGIONAL_TARGETS.contains(code)) {
			return code;
		}
		int dash = code.indexOf('-');
		return dash > 0 ? code.substring(0, dash) : code;
	}

	@Override
	public String getProviderName() {
		return PROVIDER_NAME;
	}
}
