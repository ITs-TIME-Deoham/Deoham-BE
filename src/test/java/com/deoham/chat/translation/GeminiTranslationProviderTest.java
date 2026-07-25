package com.deoham.chat.translation;

import com.deoham.chat.translation.dto.TranslationResult;
import com.deoham.global.config.GeminiProperties;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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

    private MockRestServiceServer server;
    private RestClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = builder.build();
    }

    @Test
    void translate_returnsTranslatedText_onSuccess() {
        server.expect(requestTo(EXPECTED_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-api-key"))
                .andRespond(withSuccess("""
                        {
                          "candidates": [
                            {"content": {"parts": [{"text": "Hello"}]}}
                          ],
                          "modelVersion": "gemini-3.5-flash"
                        }
                        """, MediaType.APPLICATION_JSON));

        GeminiTranslationProvider provider = new GeminiTranslationProvider(client, PROPERTIES);

        TranslationResult result = provider.translate("안녕하세요", "en");

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

        TranslationResult result = provider.translate("안녕하세요", "en");

        assertThat(result.modelVersion()).isEqualTo("gemini-3.5-flash");
    }

    @Test
    void translate_throwsBusinessException_whenApiCallFails() {
        server.expect(requestTo(containsString("generateContent")))
                .andRespond(withServerError());

        GeminiTranslationProvider provider = new GeminiTranslationProvider(client, PROPERTIES);

        assertThatThrownBy(() -> provider.translate("안녕하세요", "en"))
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

        assertThatThrownBy(() -> provider.translate("안녕하세요", "en"))
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

        assertThatThrownBy(() -> provider.translate("안녕하세요", "en"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));
    }

    @Test
    void getProviderName_returnsGemini() {
        GeminiTranslationProvider provider = new GeminiTranslationProvider(client, PROPERTIES);

        assertThat(provider.getProviderName()).isEqualTo("GEMINI");
    }
}
