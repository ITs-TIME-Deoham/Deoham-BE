-- =============================================================
-- V3: ONDO — users + user_oauth_providers
-- =============================================================

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- -------------------------------------------------------------
-- users
-- -------------------------------------------------------------
CREATE TABLE users (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    supabase_id         UUID        NOT NULL UNIQUE,
    nickname            VARCHAR(50) NOT NULL UNIQUE,
    profile_image_url   TEXT,
    phone_number        VARCHAR(20),
    phone_verified      BOOLEAN     NOT NULL DEFAULT false,
    language            VARCHAR(10) NOT NULL DEFAULT 'ko',
    is_active           BOOLEAN     NOT NULL DEFAULT true,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TRIGGER users_set_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- -------------------------------------------------------------
-- user_oauth_providers
-- -------------------------------------------------------------
CREATE TABLE user_oauth_providers (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider          oauth_provider NOT NULL,
    provider_user_id  VARCHAR(100)  NOT NULL,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    UNIQUE (provider, provider_user_id)
);
CREATE INDEX idx_user_oauth_providers_user_id ON user_oauth_providers(user_id);
