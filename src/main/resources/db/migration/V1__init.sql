-- Flyway baseline migration. Domain tables to be added in V2+.
-- Kept minimal so JPA validate-mode startup succeeds with no entities.

CREATE EXTENSION IF NOT EXISTS pgcrypto;
