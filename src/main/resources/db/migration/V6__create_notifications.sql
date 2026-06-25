-- =============================================================
-- V6: ONDO — fcm_tokens + notifications
-- =============================================================

-- -------------------------------------------------------------
-- fcm_tokens
-- -------------------------------------------------------------
CREATE TABLE fcm_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_fcm_tokens_user_id ON fcm_tokens(user_id);

-- -------------------------------------------------------------
-- notifications
-- -------------------------------------------------------------
CREATE TABLE notifications (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type          notify_type NOT NULL,
    reference_id  UUID,
    is_read       BOOLEAN     NOT NULL DEFAULT false,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read, created_at DESC);
