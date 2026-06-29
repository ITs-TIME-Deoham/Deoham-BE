-- =============================================================
-- V10: Align chat schema with original ERD (1:1 chat per card)
-- =============================================================

-- -------------------------------------------------------------
-- New ENUMs
-- -------------------------------------------------------------
CREATE TYPE chat_room_status  AS ENUM ('ACTIVE', 'CLOSED');
CREATE TYPE chat_message_type AS ENUM ('TEXT', 'IMAGE', 'LOCATION');

-- -------------------------------------------------------------
-- chat_rooms: drop many-to-many members table, add status + closed_at + UNIQUE
-- -------------------------------------------------------------
DROP TABLE chat_room_members;

ALTER TABLE chat_rooms
    ADD COLUMN status    chat_room_status NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN closed_at TIMESTAMPTZ;

ALTER TABLE chat_rooms
    ADD CONSTRAINT chat_rooms_card_id_unique UNIQUE (card_id);

-- -------------------------------------------------------------
-- chat_messages: rename room_id → chat_room_id, replace is_read with read_at + message_type
-- -------------------------------------------------------------
ALTER TABLE chat_messages RENAME COLUMN room_id TO chat_room_id;
ALTER INDEX idx_chat_messages_room_id RENAME TO idx_chat_messages_chat_room_id;

ALTER TABLE chat_messages
    DROP COLUMN is_read,
    ADD COLUMN message_type chat_message_type NOT NULL DEFAULT 'TEXT',
    ADD COLUMN read_at      TIMESTAMPTZ;

-- -------------------------------------------------------------
-- chat_message_translation: message-level translation cache
-- -------------------------------------------------------------
CREATE TABLE chat_message_translation (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_message_id  UUID        NOT NULL REFERENCES chat_messages(id) ON DELETE CASCADE,
    target_language  VARCHAR(10) NOT NULL,
    translated_text  TEXT        NOT NULL,
    provider_name    VARCHAR(50) NOT NULL,
    model_version    VARCHAR(50),
    translated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chat_message_translation_message_lang_unique
        UNIQUE (chat_message_id, target_language)
);
CREATE INDEX idx_chat_message_translation_message_id ON chat_message_translation(chat_message_id);
