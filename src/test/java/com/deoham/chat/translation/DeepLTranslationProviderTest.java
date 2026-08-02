package com.deoham.chat.translation;

import com.deoham.chat.translation.dto.TranslationResult;
import com.deoham.global.config.DeepLProperties;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * MockRestServiceServer로 실제 DeepL API 호출 없이 DeepLTranslationProvider를 검증한다.
 */
class DeepLTranslationProviderTest {

    private static final DeepLProperties PROPERTIES =
            new DeepLProperties("test-api-key", "https://api-free.deepl.com");
    private static final String BASE_URL = "https://api-free.deepl.com";
    private static final String EXPECTED_URI = BASE_URL + "/v2/translate";

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
                .andExpect(header("Authorization", "DeepL-Auth-Key test-api-key"))
                .andExpect(jsonPath("$.target_lang").value("EN"))
                .andExpect(jsonPath("$.text[0]").value("안녕하세요"))
                .andRespond(withSuccess("""
                        {
                          "translations": [
                            {"detected_source_language": "KO", "text": "Hello"}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        DeepLTranslationProvider provider = new DeepLTranslationProvider(client, PROPERTIES);

        TranslationResult result = provider.translate("안녕하세요", "en");

        assertThat(result.translatedText()).isEqualTo("Hello");
        assertThat(result.modelVersion()).isEqualTo("DEEPL");
        server.verify();
    }

    @Test
    void translate_stripsUnsupportedRegionSubtag_koKR_toKO() {
        server.expect(requestTo(EXPECTED_URI))
                .andExpect(jsonPath("$.target_lang").value("KO"))
                .andRespond(withSuccess("""
                        {"translations": [{"detected_source_language": "EN", "text": "안녕"}]}
                        """, MediaType.APPLICATION_JSON));

        DeepLTranslationProvider provider = new DeepLTranslationProvider(client, PROPERTIES);

        assertThat(provider.translate("hello", "ko-KR").translatedText()).isEqualTo("안녕");
        server.verify();
    }

    @Test
    void translate_keepsSupportedRegionalVariant_enUS() {
        server.expect(requestTo(EXPECTED_URI))
                .andExpect(jsonPath("$.target_lang").value("EN-US"))
                .andRespond(withSuccess("""
                        {"translations": [{"detected_source_language": "KO", "text": "Hi"}]}
                        """, MediaType.APPLICATION_JSON));

        DeepLTranslationProvider provider = new DeepLTranslationProvider(client, PROPERTIES);

        assertThat(provider.translate("안녕", "en-US").translatedText()).isEqualTo("Hi");
        server.verify();
    }

    @Test
    void translate_throwsInvalidRequest_onDeepL400() {
        server.expect(requestTo(containsString("/v2/translate")))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .body("{\"message\":\"Bad request. Reason: Value for 'target_lang' not supported.\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        DeepLTranslationProvider provider = new DeepLTranslationProvider(client, PROPERTIES);

        assertThatThrownBy(() -> provider.translate("안녕하세요", "xx"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void translate_throwsBusinessException_whenApiCallFails() {
        server.expect(requestTo(containsString("/v2/translate")))
                .andRespond(withServerError());

        DeepLTranslationProvider provider = new DeepLTranslationProvider(client, PROPERTIES);

        assertThatThrownBy(() -> provider.translate("안녕하세요", "en"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));
    }

    @Test
    void translate_throwsBusinessException_whenNoTranslationsReturned() {
        server.expect(requestTo(containsString("/v2/translate")))
                .andRespond(withSuccess("""
                        {"translations": []}
                        """, MediaType.APPLICATION_JSON));

        DeepLTranslationProvider provider = new DeepLTranslationProvider(client, PROPERTIES);

        assertThatThrownBy(() -> provider.translate("안녕하세요", "en"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));
    }

    @Test
    void translate_throwsBusinessException_whenTranslationTextIsBlank() {
        server.expect(requestTo(containsString("/v2/translate")))
                .andRespond(withSuccess("""
                        {"translations": [{"detected_source_language": "KO", "text": "   "}]}
                        """, MediaType.APPLICATION_JSON));

        DeepLTranslationProvider provider = new DeepLTranslationProvider(client, PROPERTIES);

        assertThatThrownBy(() -> provider.translate("안녕하세요", "en"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));
    }

    @Test
    void getProviderName_returnsDeepL() {
        DeepLTranslationProvider provider = new DeepLTranslationProvider(client, PROPERTIES);

        assertThat(provider.getProviderName()).isEqualTo("DEEPL");
    }
}
