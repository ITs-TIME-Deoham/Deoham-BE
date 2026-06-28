-- =============================================================
-- V13: Schema extension
--   1. New enums: user_role, user_status, notice_type
--   2. users: add is_verified/role/status, remove is_active
--   3. user_social_accounts: add token fields + updated_at
--   4. cards: add city, radius_m, expires_at
--   5. notices: add author_id, type, is_published, published_at
--   6. push_logs: new table
-- =============================================================

-- 1. New enum types
CREATE TYPE user_role   AS ENUM ('USER', 'ADMIN');
CREATE TYPE user_status AS ENUM ('ACTIVE', 'SUSPENDED', 'DELETED');
CREATE TYPE notice_type AS ENUM ('SYSTEM', 'EVENT', 'UPDATE', 'POLICY', 'GENERAL');

-- 2. users
ALTER TABLE users
    ADD COLUMN is_verified BOOLEAN     NOT NULL DEFAULT false,
    ADD COLUMN role        user_role   NOT NULL DEFAULT 'USER',
    ADD COLUMN status      user_status NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE users DROP COLUMN is_active;
ALTER TABLE users ALTER COLUMN is_verified DROP DEFAULT;
ALTER TABLE users ALTER COLUMN role        DROP DEFAULT;
ALTER TABLE users ALTER COLUMN status      DROP DEFAULT;

-- 3. user_social_accounts: token fields + updated_at + trigger
ALTER TABLE user_social_accounts
    ADD COLUMN provider_email   VARCHAR(255),
    ADD COLUMN access_token     TEXT,
    ADD COLUMN refresh_token    TEXT,
    ADD COLUMN token_expires_at TIMESTAMPTZ,
    ADD COLUMN updated_at       TIMESTAMPTZ NOT NULL DEFAULT now();
CREATE TRIGGER user_social_accounts_set_updated_at
    BEFORE UPDATE ON user_social_accounts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 4. cards
ALTER TABLE cards
    ADD COLUMN city       VARCHAR(100),
    ADD COLUMN radius_m   INT,
    ADD COLUMN expires_at TIMESTAMPTZ NOT NULL DEFAULT (now() + INTERVAL '24 hours');
ALTER TABLE cards ALTER COLUMN expires_at DROP DEFAULT;

-- 5. notices
ALTER TABLE notices
    ADD COLUMN author_id    UUID        REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN type         notice_type NOT NULL DEFAULT 'GENERAL',
    ADD COLUMN is_published BOOLEAN     NOT NULL DEFAULT false,
    ADD COLUMN published_at TIMESTAMPTZ;
ALTER TABLE notices ALTER COLUMN type DROP DEFAULT;
CREATE INDEX idx_notices_author_id ON notices(author_id);

-- 6. push_logs
CREATE TABLE push_logs (
    id       UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    card_id  UUID        NOT NULL REFERENCES cards(id)  ON DELETE CASCADE,
    user_id  UUID        NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    sent_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (card_id, user_id)
);
CREATE INDEX idx_push_logs_user_id ON push_logs(user_id);
