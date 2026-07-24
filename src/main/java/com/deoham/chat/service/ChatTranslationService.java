package com.deoham.chat.service;

import com.deoham.chat.dto.ChatTranslationResponse;
import com.deoham.chat.entity.ChatMessage;
import com.deoham.chat.entity.ChatMessageTranslation;
import com.deoham.chat.entity.ChatMessageType;
import com.deoham.chat.repository.ChatMessageTranslationRepository;
import com.deoham.chat.translation.TranslationProvider;
import com.deoham.chat.translation.TranslationResult;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatTranslationService {

    private final ChatMessageTranslationRepository translationRepository;
    private final ChatAccessGuard chatAccessGuard;
    private final TranslationProvider translationProvider;

    public ChatTranslationResponse translate(UUID requesterId, UUID messageId, String targetLanguage) {
        ChatMessage message = chatAccessGuard.findMessageOrThrow(messageId);
        chatAccessGuard.requireParticipant(message.getChatRoom().getCard(), requesterId);
        requireTextMessage(message);

        Optional<ChatMessageTranslation> cached =
                translationRepository.findByChatMessageIdAndTargetLanguage(messageId, targetLanguage);
        if (cached.isPresent()) {
            return toResponse(cached.get(), true);
        }

        TranslationResult result = translationProvider.translate(message.getContent(), targetLanguage);
        ChatMessageTranslation saved = translationRepository.save(ChatMessageTranslation.builder()
                .chatMessage(message)
                .targetLanguage(targetLanguage)
                .translatedText(result.translatedText())
                .providerName(result.providerName())
                .modelVersion(result.modelVersion())
                .build());

        return toResponse(saved, false);
    }

    private void requireTextMessage(ChatMessage message) {
        if (message.getMessageType() != ChatMessageType.TEXT) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "텍스트 메시지만 번역할 수 있습니다");
        }
    }

    private ChatTranslationResponse toResponse(ChatMessageTranslation translation, boolean cached) {
        return new ChatTranslationResponse(
                translation.getChatMessage().getId(),
                translation.getTargetLanguage(),
                translation.getTranslatedText(),
                cached,
                translation.getTranslatedAt());
    }
}
