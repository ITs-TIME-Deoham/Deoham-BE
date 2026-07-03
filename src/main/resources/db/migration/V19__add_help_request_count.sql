-- =============================================================
-- V19: Add help_request_count to users table
-- =============================================================

ALTER TABLE users
    ADD COLUMN help_request_count INT NOT NULL DEFAULT 0;
