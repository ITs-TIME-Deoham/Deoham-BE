-- =============================================================
-- V16: Drop title column from cards table
--   The Card entity no longer has a title field (removed in feat/7/card).
--   This migration removes the now-unused column from the database schema.
-- =============================================================

ALTER TABLE cards DROP COLUMN title;
