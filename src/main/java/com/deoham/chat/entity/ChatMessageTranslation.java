package com.deoham.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(
        name = "chat_message_translation",
        uniqueConstraints = @UniqueConstraint(
                name = "chat_message_translation_message_lang_unique",
                columnNames = {"chat_message_id", "target_language"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ChatMessageTranslation {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_message_id", nullable = false)
    private ChatMessage chatMessage;

    @Column(name = "target_language", nullable = false, length = 10)
    private String targetLanguage;

    @Column(name = "translated_text", nullable = false, columnDefinition = "TEXT")
    private String translatedText;

    @Column(name = "provider_name", nullable = false, length = 50)
    private String providerName;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @CreatedDate
    @Column(name = "translated_at", nullable = false, updatable = false)
    private Instant translatedAt;

    @Builder
    private ChatMessageTranslation(ChatMessage chatMessage, String targetLanguage,
                                    String translatedText, String providerName, String modelVersion) {
        this.chatMessage = chatMessage;
        this.targetLanguage = targetLanguage;
        this.translatedText = translatedText;
        this.providerName = providerName;
        this.modelVersion = modelVersion;
    }
}
