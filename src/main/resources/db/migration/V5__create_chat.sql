-- =============================================================
-- V5: ONDO — chat_rooms + chat_room_members + chat_messages
-- =============================================================

-- -------------------------------------------------------------
-- chat_rooms
-- -------------------------------------------------------------
CREATE TABLE chat_rooms (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    ask_id      UUID        NOT NULL REFERENCES ask_posts(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_chat_rooms_ask_id ON chat_rooms(ask_id);

-- -------------------------------------------------------------
-- chat_room_members
-- -------------------------------------------------------------
CREATE TABLE chat_room_members (
    room_id    UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (room_id, user_id)
);
CREATE INDEX idx_chat_room_members_user_id ON chat_room_members(user_id);

-- -------------------------------------------------------------
-- chat_messages
-- -------------------------------------------------------------
CREATE TABLE chat_messages (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id     UUID        NOT NULL REFERENCES chat_rooms(id),
    sender_id   UUID        NOT NULL REFERENCES users(id),
    content     TEXT        NOT NULL,
    is_read     BOOLEAN     NOT NULL DEFAULT false,
    sent_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_chat_messages_room_id ON chat_messages(room_id, sent_at DESC);
