package com.deoham.chat.repository;

import com.deoham.chat.entity.ChatMessageTranslation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageTranslationRepository extends JpaRepository<ChatMessageTranslation, UUID> {

    Optional<ChatMessageTranslation> findByChatMessageIdAndTargetLanguage(UUID chatMessageId, String targetLanguage);
}
