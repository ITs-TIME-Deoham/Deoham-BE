-- =============================================================
-- V18: Add idempotent oauth_states table creation
--   Ensures oauth_states table exists even if V17 failed locally.
--   Safe to run multiple times (used for local development).
-- =============================================================

CREATE TABLE IF NOT EXISTS oauth_states (
  state VARCHAR(128) NOT NULL PRIMARY KEY,
  provider VARCHAR(20) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_oauth_states_provider ON oauth_states(provider);
CREATE INDEX IF NOT EXISTS idx_oauth_states_expires_at ON oauth_states(expires_at);
