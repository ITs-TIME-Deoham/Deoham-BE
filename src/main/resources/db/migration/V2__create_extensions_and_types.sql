-- =============================================================
-- V2: ONDO — PostGIS extension + PostgreSQL ENUM types
-- NOTE: This replaces the previous V2 domain schema.
--       Reset your local DB if V2 was previously applied.
-- =============================================================

CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TYPE ask_category   AS ENUM ('PHOTO', 'MEAL', 'RIDE', 'OTHER');
CREATE TYPE ask_status     AS ENUM ('OPEN', 'MATCHED', 'COMPLETED', 'CANCELLED');
CREATE TYPE apply_status   AS ENUM ('PENDING', 'ACCEPTED', 'REJECTED');
CREATE TYPE oauth_provider AS ENUM ('KAKAO', 'APPLE');
CREATE TYPE notify_type    AS ENUM ('NEW_ASK', 'ASK_APPLIED', 'MATCH_ACCEPTED', 'MATCH_REJECTED', 'CHAT_MESSAGE');
CREATE TYPE report_target  AS ENUM ('USER', 'ASK', 'CHAT');
CREATE TYPE report_reason  AS ENUM ('INAPPROPRIATE', 'FRAUD', 'OTHER');
