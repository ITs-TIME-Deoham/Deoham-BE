-- =============================================================
-- V11: Align notifications table with entity
-- =============================================================

ALTER TABLE notifications ADD COLUMN message VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE notifications ALTER COLUMN message DROP DEFAULT;
