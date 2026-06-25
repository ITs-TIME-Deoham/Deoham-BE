package com.deoham.chat.service;

import com.deoham.chat.dto.ChatTranslationResponse;
import com.deoham.chat.entity.ChatMessage;
import com.deoham.chat.entity.ChatMessageTranslation;
import com.deoham.chat.entity.ChatMessageType;
import com.deoham.chat.repository.ChatMessageRepository;
import com.deoham.chat.repository.ChatMessageTranslationRepository;
import com.deoham.chat.repository.ChatRoomMemberRepository;
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

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageTranslationRepository translationRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final TranslationProvider translationProvider;

    public ChatTranslationResponse translate(UUID requesterId, UUID messageId, String targetLanguage) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "메시지를 찾을 수 없습니다"));

        boolean isMember = chatRoomMemberRepository.existsByChatRoomIdAndUserIdAndLeftAtIsNull(
                message.getChatRoom().getId(), requesterId);
        if (!isMember) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "채팅방 멤버가 아닙니다");
        }

        if (message.getMessageType() != ChatMessageType.TEXT) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "텍스트 메시지만 번역할 수 있습니다");
        }

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
                .providerName(translationProvider.getProviderName())
                .modelVersion(result.modelVersion())
                .build());

        return toResponse(saved, false);
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
