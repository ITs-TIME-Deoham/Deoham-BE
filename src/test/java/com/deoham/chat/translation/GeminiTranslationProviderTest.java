package com.deoham.chat.translation;

import com.deoham.chat.translation.dto.TranslationResult;
import com.deoham.global.config.GeminiProperties;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * MockRestServiceServer로 실제 Gemini API 호출 없이 GeminiTranslationProvider를 검증한다.
 */
class GeminiTranslationProviderTest {

    private static final GeminiProperties PROPERTIES = new GeminiProperties("test-api-key", "gemini-3.5-flash");
    private static final String BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String EXPECTED_URI = BASE_URL + "/v1beta/models/gemini-3.5-flash:generateContent";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String OK_RESPONSE = """
            {
              "candidates": [
                {"content": {"parts": [{"text": "Hello"}]}}
              ],
              "modelVersion": "gemini-3.5-flash"
            }
            """;

    private MockRestServiceServer server;
    private RestClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = builder.build();
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 응답 처리
    // ───────────────────────────────────────────────────────────────────────────

    @Test
    void translate_returnsTranslatedText_onSuccess() {
        server.expect(requestTo(EXPECTED_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-api-key"))
                .andRespond(withSuccess(OK_RESPONSE, MediaType.APPLICATION_JSON));

        GeminiTranslationProvider provider = new GeminiTranslationProvider(client, PROPERTIES);

        TranslationResult result = provider.translate("안녕하세요", TargetLanguage.EN);

        assertThat(result.translatedText()).isEqualTo("Hello");
        assertThat(result.modelVersion()).isEqualTo("gemini-3.5-flash");
        server.verify();
    }

    @Test
    void translate_fallsBackToConfiguredModel_whenResponseOmitsModelVersion() {
        server.expect(requestTo(EXPECTED_URI))
                .andRespond(withSuccess("""
                        {"candidates": [{"content": {"parts": [{"text": "Hello"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        GeminiTranslationProvider provider = new GeminiTranslationProvider(client, PROPERTIES);

        TranslationResult result = provider.translate("안녕하세요", TargetLanguage.EN);

        assertThat(result.modelVersion()).isEqualTo("gemini-3.5-flash");
    }

    @Test
    void translate_throwsBusinessException_whenApiCallFails() {
        server.expect(requestTo(containsString("generateContent")))
                .andRespond(withServerError());

        GeminiTranslationProvider provider = new GeminiTranslationProvider(client, PROPERTIES);

        assertThatThrownBy(() -> provider.translate("안녕하세요", TargetLanguage.EN))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));
    }

    @Test
    void translate_throwsBusinessException_whenNoCandidatesReturned() {
        server.expect(requestTo(containsString("generateContent")))
                .andRespond(withSuccess("""
                        {"candidates": []}
                        """, MediaType.APPLICATION_JSON));

        GeminiTranslationProvider provider = new GeminiTranslationProvider(client, PROPERTIES);

        assertThatThrownBy(() -> provider.translate("안녕하세요", TargetLanguage.EN))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));
    }

    @Test
    void translate_throwsBusinessException_whenCandidateHasBlankText() {
        server.expect(requestTo(containsString("generateContent")))
                .andRespond(withSuccess("""
                        {"candidates": [{"content": {"parts": [{"text": "   "}]}}]}
                        """, MediaType.APPLICATION_JSON));

        GeminiTranslationProvider provider = new GeminiTranslationProvider(client, PROPERTIES);

        assertThatThrownBy(() -> provider.translate("안녕하세요", TargetLanguage.EN))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));
    }

    @Test
    void getProviderName_returnsGemini() {
        GeminiTranslationProvider provider = new GeminiTranslationProvider(client, PROPERTIES);

        assertThat(provider.getProviderName()).isEqualTo("GEMINI");
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 프롬프트에는 화이트리스트 표시명만 들어간다
    // ───────────────────────────────────────────────────────────────────────────

    @Test
    void translate_putsDisplayNameInPrompt_notRawLanguageCode() {
        // 클라이언트가 "en-US"를 보내도 화이트리스트를 거치면 EN 이 되고,
        // 프롬프트에는 코드가 아니라 코드 상수인 표시명이 들어간다.
        TargetLanguage language = TargetLanguage.from("en-US");

        JsonNode body = capturedRequestBody(provider -> provider.translate("안녕하세요", language));

        String prompt = body.at("/contents/0/parts/0/text").asText();

        assertThat(prompt).contains("English");
        assertThat(prompt).doesNotContain("en-US");
        assertThat(prompt).doesNotContain("EN-US");
    }

    @Test
    void translate_usesDisplayNameForScriptVariants() {
        JsonNode body = capturedRequestBody(provider -> provider.translate("안녕하세요", TargetLanguage.ZH_HANS));

        String prompt = body.at("/contents/0/parts/0/text").asText();

        assertThat(prompt).contains("Simplified Chinese");
        assertThat(prompt).doesNotContain("zh-hans");
        assertThat(prompt).doesNotContain("ZH_HANS");
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 헬퍼
    // ───────────────────────────────────────────────────────────────────────────

    /** 성공 응답을 돌려주면서, provider가 실제로 보낸 요청 바디를 JSON으로 캡처한다. */
    private JsonNode capturedRequestBody(java.util.function.Consumer<GeminiTranslationProvider> invocation) {
        AtomicReference<String> captured = new AtomicReference<>();
        server.expect(requestTo(EXPECTED_URI))
                .andExpect(request -> captured.set(((MockClientHttpRequest) request).getBodyAsString()))
                .andRespond(withSuccess(OK_RESPONSE, MediaType.APPLICATION_JSON));

        invocation.accept(new GeminiTranslationProvider(client, PROPERTIES));
        server.verify();

        try {
            return MAPPER.readTree(captured.get());
        } catch (Exception e) {
            throw new IllegalStateException("요청 바디 파싱 실패: " + captured.get(), e);
        }
    }
}
