package com.deoham.chat.controller.docs;

import com.deoham.chat.dto.ChatTranslationRequest;
import com.deoham.chat.dto.ChatTranslationResponse;
import com.deoham.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;

@Tag(name = "Chat Translation", description = "Chat message translation APIs")
public interface ChatTranslationControllerDocs {

    @Operation(
            summary = "Translate message",
            description = """
                    Translates a chat message into the requested language.

                    Translation cache is stored by `(messageId, targetLanguage)`.
                    Only `TEXT` messages can be translated. Requests for `IMAGE` or `LOCATION`
                    messages return 400, matching the `chat_message_type` database enum.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Translated")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not a chat room member")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Message not found")
    ApiResponse<ChatTranslationResponse> translate(
            @Parameter(description = "Message UUID") UUID messageId,
            @Valid ChatTranslationRequest request);
}
