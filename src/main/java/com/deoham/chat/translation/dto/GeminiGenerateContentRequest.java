package com.deoham.chat.translation.dto;

import java.util.List;

public record GeminiGenerateContentRequest(
		List<Content> contents
) {

	public record Content(List<Part> parts) {
	}

	public record Part(String text) {
	}

	public static GeminiGenerateContentRequest ofPrompt(String prompt) {
		return new GeminiGenerateContentRequest(List.of(new Content(List.of(new Part(prompt)))));
	}
}
