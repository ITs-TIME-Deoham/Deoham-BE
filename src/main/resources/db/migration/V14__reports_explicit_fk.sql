-- =============================================================
-- V14: reports — replace generic target_id with explicit FKs
--   - ADD reported_user_id (FK → users, nullable)
--   - ADD reported_card_id (FK → cards, nullable)
--   - DROP target_id (generic, no FK constraint)
--   - Replace UNIQUE constraint with partial unique indexes
-- =============================================================

ALTER TABLE reports
    ADD COLUMN reported_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN reported_card_id UUID REFERENCES cards(id) ON DELETE SET NULL;

ALTER TABLE reports DROP CONSTRAINT reports_reporter_id_target_type_target_id_key;
ALTER TABLE reports DROP COLUMN target_id;

CREATE UNIQUE INDEX idx_reports_reporter_user
    ON reports(reporter_id, reported_user_id)
    WHERE reported_user_id IS NOT NULL;

CREATE UNIQUE INDEX idx_reports_reporter_card
    ON reports(reporter_id, reported_card_id)
    WHERE reported_card_id IS NOT NULL;
