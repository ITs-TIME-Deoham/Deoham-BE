package com.deoham.chat.translation;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DeepLTranslateRequest(
		List<String> text,
		@JsonProperty("target_lang") String targetLang
) {

	public static DeepLTranslateRequest of(String text, String targetLang) {
		return new DeepLTranslateRequest(List.of(text), targetLang);
	}
}
