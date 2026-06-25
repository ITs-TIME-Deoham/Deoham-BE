-- =============================================================
-- V4: Chat message translation cache + chat notification linkage
-- =============================================================

CREATE TABLE chat_message_translation (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_message_id  UUID         NOT NULL REFERENCES chat_message(id) ON DELETE CASCADE,
    target_language  VARCHAR(10)  NOT NULL,
    translated_text  TEXT         NOT NULL,
    provider_name    VARCHAR(50)  NOT NULL,
    model_version    VARCHAR(50),
    translated_at    TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT chat_message_translation_message_lang_unique
        UNIQUE (chat_message_id, target_language)
);
CREATE INDEX idx_chat_message_translation_message_id ON chat_message_translation(chat_message_id);

ALTER TABLE notification
    ADD COLUMN chat_message_id UUID REFERENCES chat_message(id) ON DELETE SET NULL;
CREATE INDEX idx_notification_chat_message_id ON notification(chat_message_id);

ALTER TABLE notification DROP CONSTRAINT notification_type_check;
ALTER TABLE notification ADD CONSTRAINT notification_type_check
    CHECK (type IN ('CARD_CREATED', 'LINK_VIEWED', 'COUNTERPART_CONFIRMED', 'CHAT_MESSAGE_RECEIVED'));
