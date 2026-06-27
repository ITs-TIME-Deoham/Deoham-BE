-- =============================================================
-- V7: ONDO — reports + user_blocks
-- =============================================================

-- -------------------------------------------------------------
-- reports
-- -------------------------------------------------------------
CREATE TABLE reports (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id   UUID          NOT NULL REFERENCES users(id),
    target_type   report_target NOT NULL,
    target_id     UUID          NOT NULL,
    reason        report_reason NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    UNIQUE (reporter_id, target_type, target_id)
);
CREATE INDEX idx_reports_reporter_id ON reports(reporter_id);

-- -------------------------------------------------------------
-- user_blocks
-- -------------------------------------------------------------
CREATE TABLE user_blocks (
    blocker_id  UUID        NOT NULL REFERENCES users(id),
    blocked_id  UUID        NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (blocker_id, blocked_id),
    CHECK (blocker_id <> blocked_id)
);
CREATE INDEX idx_user_blocks_blocker_id ON user_blocks(blocker_id);
