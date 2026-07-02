-- =============================================================
-- V15: partial index to support unread-message-count queries
--   (chat_room_id, sender_id) where read_at IS NULL — matches the
--   WHERE clause used to count/list a user's unread messages per room
-- =============================================================

CREATE INDEX idx_chat_messages_unread
    ON chat_messages(chat_room_id, sender_id)
    WHERE read_at IS NULL;
