package com.deoham.chat.translation;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DeepLTranslateResponse(
		List<Translation> translations
) {

	public record Translation(
			@JsonProperty("detected_source_language") String detectedSourceLanguage,
			String text
	) {
	}

	public String firstText() {
		if (translations == null || translations.isEmpty()) {
			return null;
		}
		return translations.get(0).text();
	}
}
