-- =============================================================
-- V15: Kakao OAuth direct login cleanup
--   1. users: remove unused firebase_uid
--   2. oauth_states: server-side OAuth state storage
-- =============================================================

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_firebase_uid_key;
ALTER TABLE users DROP COLUMN IF EXISTS firebase_uid;

CREATE TABLE oauth_states (
    state      VARCHAR(128) PRIMARY KEY,
    provider   VARCHAR(20)  NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_oauth_states_expires_at ON oauth_states(expires_at);
