-- =============================================================
-- V12: Schema alignment
--   1. users: supabase_id → firebase_uid
--   2. user_oauth_providers → user_social_accounts (+ column rename)
--   3. cards: author_id → requester_id, title NOT NULL
--   4. card_applies: created_at → applied_at + responded_at
--   5. notices + notice_reads
--   6. report_reason enum: replace values
-- =============================================================

-- 1. users: replace supabase_id with firebase_uid
ALTER TABLE users DROP COLUMN supabase_id;
ALTER TABLE users ADD COLUMN firebase_uid VARCHAR(128);
UPDATE users SET firebase_uid = 'placeholder_' || id::text WHERE firebase_uid IS NULL;
ALTER TABLE users ALTER COLUMN firebase_uid SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT users_firebase_uid_key UNIQUE (firebase_uid);

-- 2. user_oauth_providers → user_social_accounts
ALTER TABLE user_oauth_providers RENAME TO user_social_accounts;
ALTER TABLE user_social_accounts RENAME COLUMN provider_user_id TO provider_uid;
ALTER INDEX idx_user_oauth_providers_user_id RENAME TO idx_user_social_accounts_user_id;

-- 3. cards: rename author_id → requester_id, restore title NOT NULL
ALTER TABLE cards RENAME COLUMN author_id TO requester_id;
ALTER INDEX idx_cards_author_id RENAME TO idx_cards_requester_id;
UPDATE cards SET title = '제목 없음' WHERE title IS NULL;
ALTER TABLE cards ALTER COLUMN title SET NOT NULL;

-- 4. card_applies: replace created_at with applied_at + responded_at
ALTER TABLE card_applies
    ADD COLUMN applied_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN responded_at TIMESTAMPTZ;
UPDATE card_applies SET applied_at = created_at;
ALTER TABLE card_applies ALTER COLUMN applied_at DROP DEFAULT;
ALTER TABLE card_applies DROP COLUMN created_at;

-- 5. notices + notice_reads
CREATE TABLE notices (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    title      VARCHAR(100) NOT NULL,
    content    TEXT         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE TRIGGER notices_set_updated_at
    BEFORE UPDATE ON notices
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE notice_reads (
    user_id   UUID        NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
    notice_id UUID        NOT NULL REFERENCES notices(id) ON DELETE CASCADE,
    read_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, notice_id)
);
CREATE INDEX idx_notice_reads_user_id ON notice_reads(user_id);

-- 6. report_reason enum: replace INAPPROPRIATE/FRAUD/OTHER with new values
ALTER TYPE report_reason RENAME TO report_reason_old;
CREATE TYPE report_reason AS ENUM ('HARASSMENT', 'OBSCENE_CONTENT', 'PRIVACY_VIOLATION');
ALTER TABLE reports ALTER COLUMN reason TYPE report_reason USING 'HARASSMENT'::report_reason;
DROP TYPE report_reason_old;
