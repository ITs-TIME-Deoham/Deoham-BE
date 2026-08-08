package com.deoham.chat.translation.dto;

import java.util.List;

/**
 * Gemini {@code generateContent} 요청 바디.
 *
 * <p>지시(system)와 데이터(user)를 서로 다른 필드로 분리해 보낸다. 하나의 프롬프트 문자열에
 * 지시와 사용자 텍스트를 이어붙이면 모델 입장에서 둘의 경계가 없어, 사용자 텍스트에 심긴 문장이
 * 곧바로 지시로 읽힐 수 있다.
 */
public record GeminiGenerateContentRequest(
		List<Content> contents,
		Content systemInstruction,
		GenerationConfig generationConfig
) {

	/** 번역은 창의성이 필요 없고, 같은 입력에 같은 결과가 나오는 편이 캐시 의미와도 맞는다. */
	private static final double TEMPERATURE = 0.0d;

	public record Content(List<Part> parts) {
	}

	public record Part(String text) {
	}

	/**
	 * 출력 제어. {@code maxOutputTokens}가 없으면 "긴 글을 써라"류 인젝션이 통했을 때
	 * 비용·레이턴시 상한이 사라진다.
	 */
	public record GenerationConfig(double temperature, int maxOutputTokens) {
	}

	public static GeminiGenerateContentRequest of(String systemInstruction, String userPrompt, int maxOutputTokens) {
		return new GeminiGenerateContentRequest(
				List.of(new Content(List.of(new Part(userPrompt)))),
				new Content(List.of(new Part(systemInstruction))),
				new GenerationConfig(TEMPERATURE, maxOutputTokens));
	}
}
