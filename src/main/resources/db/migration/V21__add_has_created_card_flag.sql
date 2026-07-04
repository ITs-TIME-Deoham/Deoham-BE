-- =============================================================
-- V21: Add onboarding flag for first card creation
-- =============================================================

ALTER TABLE users
    ADD COLUMN has_created_card BOOLEAN NOT NULL DEFAULT FALSE;
