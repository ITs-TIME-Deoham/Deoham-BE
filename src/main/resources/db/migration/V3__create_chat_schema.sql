-- =============================================================
-- V3: Chat domain schema (chat_room / chat_room_member / chat_message)
-- =============================================================

CREATE TABLE chat_room (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID         REFERENCES project(id) ON DELETE SET NULL,
    name            VARCHAR(200),
    is_direct       BOOLEAN      NOT NULL DEFAULT FALSE,
    last_message_at TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX idx_chat_room_project_id ON chat_room(project_id);
CREATE INDEX idx_chat_room_last_message_at ON chat_room(last_message_at DESC NULLS LAST);
CREATE TRIGGER chat_room_set_updated_at
    BEFORE UPDATE ON chat_room
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE chat_room_member (
    id                    UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_room_id          UUID      NOT NULL REFERENCES chat_room(id) ON DELETE CASCADE,
    user_id               UUID      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role                  VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    last_read_message_at  TIMESTAMP,
    left_at               TIMESTAMP,
    created_at            TIMESTAMP NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chat_room_member_role_check CHECK (role IN ('OWNER', 'MEMBER')),
    CONSTRAINT chat_room_member_room_user_unique UNIQUE (chat_room_id, user_id)
);
CREATE INDEX idx_chat_room_member_room_id ON chat_room_member(chat_room_id);
CREATE INDEX idx_chat_room_member_user_id ON chat_room_member(user_id);
CREATE INDEX idx_chat_room_member_active ON chat_room_member(chat_room_id) WHERE left_at IS NULL;
CREATE TRIGGER chat_room_member_set_updated_at
    BEFORE UPDATE ON chat_room_member
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE chat_message (
    id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_room_id             UUID         NOT NULL REFERENCES chat_room(id) ON DELETE CASCADE,
    sender_id                UUID         NOT NULL REFERENCES users(id) ON DELETE SET NULL,
    message_type             VARCHAR(20)  NOT NULL DEFAULT 'TEXT',
    content                  TEXT,
    attachment_url           TEXT,
    attachment_file_name     VARCHAR(255),
    attachment_content_type  VARCHAR(100),
    attachment_size_bytes    BIGINT,
    deleted_at               TIMESTAMP,
    created_at               TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at               TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT chat_message_type_check
        CHECK (message_type IN ('TEXT', 'IMAGE', 'FILE')),
    CONSTRAINT chat_message_content_consistency_check
        CHECK (
            (message_type = 'TEXT' AND content IS NOT NULL AND attachment_url IS NULL)
            OR
            (message_type IN ('IMAGE', 'FILE') AND attachment_url IS NOT NULL)
        )
);
CREATE INDEX idx_chat_message_room_created ON chat_message(chat_room_id, created_at DESC);
CREATE INDEX idx_chat_message_sender_id ON chat_message(sender_id);
CREATE TRIGGER chat_message_set_updated_at
    BEFORE UPDATE ON chat_message
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
