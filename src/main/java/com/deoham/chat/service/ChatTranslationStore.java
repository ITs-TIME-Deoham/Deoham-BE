package com.deoham.chat.service;

import com.deoham.chat.dto.ChatTranslationResponse;
import com.deoham.chat.entity.ChatMessage;
import com.deoham.chat.entity.ChatMessageTranslation;
import com.deoham.chat.entity.ChatMessageType;
import com.deoham.chat.repository.ChatMessageRepository;
import com.deoham.chat.repository.ChatMessageTranslationRepository;
import com.deoham.chat.translation.dto.TranslationResult;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 번역 기능의 DB 작업(권한 검증 + 캐시 조회, 결과 저장)만 담당한다.
 *
 * <p>{@link ChatTranslationService}가 외부 번역 API 호출을 트랜잭션 <b>밖</b>에서 수행하도록,
 * DB 트랜잭션 경계를 이 빈으로 분리했다. 오케스트레이터와 다른 빈이어야 Spring 프록시를 거쳐
 * {@code @Transactional}이 실제로 적용된다(같은 빈 내 자기호출은 프록시를 우회함).
 */
@Component
@RequiredArgsConstructor
class ChatTranslationStore {

    private final ChatMessageTranslationRepository translationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatAccessGuard chatAccessGuard;

    /**
     * 접근 권한 검증과 캐시 조회를 한 읽기 트랜잭션에서 끝낸다. 캐시 히트면 완성된 응답을,
     * 미스면 번역할 원문을 담아 돌려준다. 외부 API 호출이 이 트랜잭션 밖에서 이뤄지므로,
     * 트랜잭션 종료 후 lazy 로딩이 필요 없도록 여기서 필요한 값을 모두 추출한다.
     */
    @Transactional(readOnly = true)
    TranslationLookup lookup(UUID requesterId, UUID messageId, String targetLanguage) {
        ChatMessage message = chatAccessGuard.findMessageOrThrow(messageId);
        chatAccessGuard.requireParticipant(message.getChatRoom().getCard(), requesterId);
        requireTextMessage(message);

        return translationRepository.findByChatMessageIdAndTargetLanguage(messageId, targetLanguage)
                .map(cached -> TranslationLookup.cached(toResponse(cached, true)))
                .orElseGet(() -> TranslationLookup.needsTranslation(message.getContent()));
    }

    /**
     * 번역 결과를 저장한다. 트랜잭션을 짧게 쪼갠 뒤라, 동시 요청이 같은 (메시지, 언어)를 각각
     * 번역해 저장하려 하면 유니크 제약(chat_message_id, target_language)에 걸릴 수 있다.
     * 그 경합은 호출자({@link ChatTranslationService})가 잡아 먼저 저장된 결과로 대체하므로,
     * 여기서는 커밋 지연 없이 제약 위반이 즉시 드러나도록 {@code saveAndFlush}로 강제 flush 한다.
     */
    @Transactional
    ChatTranslationResponse save(UUID messageId, String targetLanguage, TranslationResult result) {
        ChatMessage message = chatMessageRepository.getReferenceById(messageId);
        ChatMessageTranslation saved = translationRepository.saveAndFlush(ChatMessageTranslation.builder()
                .chatMessage(message)
                .targetLanguage(targetLanguage)
                .translatedText(result.translatedText())
                .providerName(result.providerName())
                .modelVersion(result.modelVersion())
                .build());
        return toResponse(saved, false);
    }

    /**
     * 이미 저장된 번역을 캐시로 조회한다. 저장 경합(동시 요청)에서 진 쪽이 승자의 결과를
     * 돌려주기 위해 새 트랜잭션에서 다시 읽을 때 사용한다.
     */
    @Transactional(readOnly = true)
    ChatTranslationResponse getCached(UUID messageId, String targetLanguage) {
        return translationRepository.findByChatMessageIdAndTargetLanguage(messageId, targetLanguage)
                .map(cached -> toResponse(cached, true))
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "번역 결과를 받지 못했습니다."));
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

    /**
     * {@link #lookup} 결과. 캐시 히트면 {@code cachedResponse}가, 미스면 번역할 {@code sourceText}가 채워진다.
     */
    record TranslationLookup(ChatTranslationResponse cachedResponse, String sourceText) {

        static TranslationLookup cached(ChatTranslationResponse response) {
            return new TranslationLookup(response, null);
        }

        static TranslationLookup needsTranslation(String sourceText) {
            return new TranslationLookup(null, sourceText);
        }

        boolean isCached() {
            return cachedResponse != null;
        }
    }
}
