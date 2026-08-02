package com.deoham.chat.translation.dto;

import java.util.List;

public record GeminiGenerateContentResponse(
		List<Candidate> candidates,
		String modelVersion
) {

	public record Candidate(Content content) {
	}

	public record Content(List<Part> parts) {
	}

	public record Part(String text) {
	}

	public String firstText() {
		if (candidates == null || candidates.isEmpty()) {
			return null;
		}
		Content content = candidates.get(0).content();
		if (content == null || content.parts() == null || content.parts().isEmpty()) {
			return null;
		}
		return content.parts().get(0).text();
	}
}
