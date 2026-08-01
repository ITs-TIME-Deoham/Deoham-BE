package com.deoham.chat.service;

import com.deoham.chat.dto.ChatTranslationResponse;
import com.deoham.chat.service.ChatTranslationStore.TranslationLookup;
import com.deoham.chat.translation.TranslationProvider;
import com.deoham.chat.translation.TranslationResult;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * 메시지 번역 흐름을 조율한다. 이 클래스 자체는 트랜잭션을 열지 않는다.
 *
 * <p>DB 트랜잭션은 {@link ChatTranslationStore}의 짧은 읽기/쓰기 구간에만 존재하고,
 * 느린 외부 번역 API 호출은 두 트랜잭션 <b>사이</b>(트랜잭션 밖)에서 수행한다.
 * 예전처럼 외부 호출 전 구간을 한 트랜잭션으로 감싸면 그 동안 DB 커넥션을 붙잡아,
 * 번역 지연 시 HikariCP 풀이 고갈되고 번역과 무관한 요청까지 막힌다.
 */
@Service
@RequiredArgsConstructor
public class ChatTranslationService {

    private final ChatTranslationStore store;
    private final TranslationProvider translationProvider;

    public ChatTranslationResponse translate(UUID requesterId, UUID messageId, String targetLanguage) {
        // 1) 권한 검증 + 캐시 조회 (짧은 읽기 트랜잭션)
        TranslationLookup lookup = store.lookup(requesterId, messageId, targetLanguage);
        if (lookup.isCached()) {
            return lookup.cachedResponse();
        }

        // 2) 외부 번역 API 호출 (트랜잭션 밖 — DB 커넥션을 점유하지 않는다)
        TranslationResult result = translationProvider.translate(lookup.sourceText(), targetLanguage);

        // 3) 결과 저장 (짧은 쓰기 트랜잭션)
        try {
            return store.save(messageId, targetLanguage, result);
        } catch (DataIntegrityViolationException raced) {
            // 동시 요청이 같은 (메시지, 언어)를 먼저 저장함 → 승자의 결과를 캐시로 반환
            return store.getCached(messageId, targetLanguage);
        }
    }
}
