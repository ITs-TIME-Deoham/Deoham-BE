-- =============================================================
-- V4: ONDO — ask_posts + ask_applies
-- =============================================================

-- -------------------------------------------------------------
-- ask_posts
-- -------------------------------------------------------------
CREATE TABLE ask_posts (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id   UUID          NOT NULL REFERENCES users(id),
    category    ask_category  NOT NULL,
    title       VARCHAR(100)  NOT NULL,
    description TEXT,
    location    GEOGRAPHY(POINT, 4326) NOT NULL,
    status      ask_status    NOT NULL DEFAULT 'OPEN',
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX ask_posts_location_idx ON ask_posts USING GIST (location);
CREATE INDEX idx_ask_posts_author_id ON ask_posts(author_id);
CREATE INDEX idx_ask_posts_status    ON ask_posts(status);
CREATE TRIGGER ask_posts_set_updated_at
    BEFORE UPDATE ON ask_posts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- -------------------------------------------------------------
-- ask_applies
-- -------------------------------------------------------------
CREATE TABLE ask_applies (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    ask_id        UUID          NOT NULL REFERENCES ask_posts(id),
    applicant_id  UUID          NOT NULL REFERENCES users(id),
    status        apply_status  NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    UNIQUE (ask_id, applicant_id)
);
CREATE INDEX idx_ask_applies_ask_id       ON ask_applies(ask_id);
CREATE INDEX idx_ask_applies_applicant_id ON ask_applies(applicant_id);
