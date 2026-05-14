-- =============================================================
-- V2: 더함(Deoham) domain schema
-- =============================================================
-- enum: VARCHAR + CHECK constraint (migration ergonomics over PG ENUM type).
-- timestamps: NOT NULL DEFAULT now(); set_updated_at() trigger for UPDATE.
-- ids: UUID PRIMARY KEY DEFAULT gen_random_uuid() (pgcrypto from V1).
-- =============================================================

-- -------------------------------------------------------------
-- 0. updated_at auto-touch trigger function
-- -------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- -------------------------------------------------------------
-- 1. users / user_auth
-- -------------------------------------------------------------
CREATE TABLE users (
    id                          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email                       VARCHAR(255) NOT NULL UNIQUE,
    name                        VARCHAR(100) NOT NULL,
    job_type                    VARCHAR(100),
    phone                       VARCHAR(20),
    plan_type                   VARCHAR(20)  NOT NULL DEFAULT 'FREE',
    noti_new_card               BOOLEAN      NOT NULL DEFAULT TRUE,
    noti_link_viewed            BOOLEAN      NOT NULL DEFAULT TRUE,
    noti_counterpart_confirmed  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT users_plan_type_check CHECK (plan_type IN ('FREE', 'PRO'))
);
CREATE TRIGGER users_set_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE user_auth (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider      VARCHAR(20)  NOT NULL,
    provider_uid  VARCHAR(255),
    password_hash VARCHAR(255),
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT user_auth_provider_check
        CHECK (provider IN ('EMAIL', 'KAKAO', 'GOOGLE', 'APPLE')),
    CONSTRAINT user_auth_provider_uid_unique UNIQUE (provider, provider_uid),
    CONSTRAINT user_auth_user_provider_unique UNIQUE (user_id, provider)
);
CREATE INDEX idx_user_auth_user_id ON user_auth(user_id);
CREATE TRIGGER user_auth_set_updated_at
    BEFORE UPDATE ON user_auth
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- -------------------------------------------------------------
-- 2. project / contact
-- -------------------------------------------------------------
CREATE TABLE project (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    collab_type     VARCHAR(20)  NOT NULL,
    name            VARCHAR(200) NOT NULL,
    contract_amount DECIMAL(15,2),
    contract_start  DATE,
    contract_end    DATE,
    file_url        TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT project_collab_type_check
        CHECK (collab_type IN ('FREELANCE', 'INTERNAL', 'TEAM', 'ETC')),
    CONSTRAINT project_contract_dates_check
        CHECK (contract_start IS NULL OR contract_end IS NULL OR contract_start <= contract_end),
    CONSTRAINT project_contract_amount_nonneg
        CHECK (contract_amount IS NULL OR contract_amount >= 0)
);
CREATE INDEX idx_project_user_id ON project(user_id);
CREATE INDEX idx_project_user_created ON project(user_id, created_at DESC);
CREATE TRIGGER project_set_updated_at
    BEFORE UPDATE ON project
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE contact (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID         NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(255),
    role       VARCHAR(20)  NOT NULL DEFAULT 'CLIENT',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT contact_role_check
        CHECK (role IN ('CLIENT', 'PM', 'MEMBER', 'ETC')),
    CONSTRAINT contact_project_email_unique UNIQUE (project_id, email)
);
CREATE INDEX idx_contact_project_id ON contact(project_id);
CREATE TRIGGER contact_set_updated_at
    BEFORE UPDATE ON contact
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- -------------------------------------------------------------
-- 3. card / ai_analysis / card_impact / card_share / card_view_log
-- -------------------------------------------------------------
CREATE TABLE card (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id       UUID        NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    contact_id       UUID        REFERENCES contact(id) ON DELETE SET NULL,
    category         VARCHAR(30),
    original_message TEXT,
    source_type      VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    memo             TEXT,
    occurred_at      TIMESTAMP,
    created_at       TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT card_category_check
        CHECK (category IS NULL OR category IN
            ('SCOPE_CHANGE', 'SCHEDULE_CHANGE', 'ADDITIONAL_REQUEST', 'ETC')),
    CONSTRAINT card_source_type_check
        CHECK (source_type IN ('KAKAO', 'FILE', 'EMAIL', 'MANUAL'))
);
CREATE INDEX idx_card_project_id ON card(project_id);
CREATE INDEX idx_card_contact_id ON card(contact_id);
CREATE INDEX idx_card_project_created ON card(project_id, created_at DESC);
CREATE INDEX idx_card_occurred_at ON card(occurred_at DESC NULLS LAST);
CREATE TRIGGER card_set_updated_at
    BEFORE UPDATE ON card
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE ai_analysis (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    card_id          UUID         NOT NULL REFERENCES card(id) ON DELETE CASCADE,
    classification   VARCHAR(100),
    summary          TEXT,
    confidence_score REAL,
    model_version    VARCHAR(50)  NOT NULL,
    is_latest        BOOLEAN      NOT NULL DEFAULT TRUE,
    analyzed_at      TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT ai_analysis_confidence_range
        CHECK (confidence_score IS NULL OR (confidence_score >= 0 AND confidence_score <= 1))
);
CREATE INDEX idx_ai_analysis_card_latest ON ai_analysis(card_id, is_latest);
-- Only one "latest" analysis per card.
CREATE UNIQUE INDEX uq_ai_analysis_one_latest_per_card
    ON ai_analysis(card_id) WHERE is_latest = TRUE;

CREATE TABLE card_impact (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    card_id     UUID          NOT NULL REFERENCES card(id) ON DELETE CASCADE,
    impact_type VARCHAR(20)   NOT NULL,
    description TEXT,
    amount      DECIMAL(15,2),
    created_at  TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT card_impact_type_check
        CHECK (impact_type IN ('SCHEDULE', 'COST'))
);
CREATE INDEX idx_card_impact_card_id ON card_impact(card_id);
CREATE TRIGGER card_impact_set_updated_at
    BEFORE UPDATE ON card_impact
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE card_share (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    card_id    UUID         NOT NULL REFERENCES card(id) ON DELETE CASCADE,
    contact_id UUID         NOT NULL REFERENCES contact(id) ON DELETE CASCADE,
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP,
    revoked_at TIMESTAMP,
    revoked_by UUID         REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX idx_card_share_card_id ON card_share(card_id);
CREATE INDEX idx_card_share_contact_id ON card_share(contact_id);
-- Active (non-revoked) shares lookup.
CREATE INDEX idx_card_share_active ON card_share(card_id) WHERE revoked_at IS NULL;
CREATE TRIGGER card_share_set_updated_at
    BEFORE UPDATE ON card_share
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE card_view_log (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    card_share_id UUID        NOT NULL REFERENCES card_share(id) ON DELETE CASCADE,
    viewed_at     TIMESTAMP   NOT NULL DEFAULT now(),
    ip_address    VARCHAR(45),
    user_agent    TEXT
);
CREATE INDEX idx_card_view_log_share_id ON card_view_log(card_share_id);
CREATE INDEX idx_card_view_log_viewed_at ON card_view_log(viewed_at DESC);

-- -------------------------------------------------------------
-- 4. notification
-- -------------------------------------------------------------
CREATE TABLE notification (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type       VARCHAR(30)  NOT NULL,
    card_id    UUID         REFERENCES card(id) ON DELETE SET NULL,
    message    VARCHAR(500) NOT NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT notification_type_check
        CHECK (type IN ('CARD_CREATED', 'LINK_VIEWED', 'COUNTERPART_CONFIRMED'))
);
-- Unread-feed query: user_id + is_read + ordered by created_at DESC.
CREATE INDEX idx_notification_user_unread ON notification(user_id, is_read, created_at DESC);
CREATE INDEX idx_notification_card_id ON notification(card_id);
