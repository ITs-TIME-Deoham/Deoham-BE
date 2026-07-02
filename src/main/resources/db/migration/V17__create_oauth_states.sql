-- =============================================================
-- V17: Create oauth_states table
--   Stores OAuth state parameters for login flow validation.
--   Used to verify OAuth callbacks and prevent CSRF attacks.
-- =============================================================

CREATE TABLE oauth_states (
  state VARCHAR(128) NOT NULL PRIMARY KEY,
  provider VARCHAR(20) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_oauth_states_provider ON oauth_states(provider);
CREATE INDEX idx_oauth_states_expires_at ON oauth_states(expires_at);
