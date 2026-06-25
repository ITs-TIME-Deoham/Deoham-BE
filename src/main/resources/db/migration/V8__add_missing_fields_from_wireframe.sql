-- =============================================================
-- V8: Add missing fields derived from wireframe analysis
-- =============================================================

-- New ENUM types
CREATE TYPE user_gender       AS ENUM ('MALE', 'FEMALE');
CREATE TYPE preferred_gender  AS ENUM ('MALE', 'FEMALE', 'ANY');

-- users: gender, age, help_count
ALTER TABLE users
    ADD COLUMN gender     user_gender,
    ADD COLUMN age        INT,
    ADD COLUMN help_count INT NOT NULL DEFAULT 0;

-- ask_posts: preferred helper filter + retry counter; title becomes optional
ALTER TABLE ask_posts
    ALTER COLUMN title DROP NOT NULL,
    ADD COLUMN preferred_gender  preferred_gender,
    ADD COLUMN preferred_age_min INT,
    ADD COLUMN preferred_age_max INT,
    ADD COLUMN retry_count       INT NOT NULL DEFAULT 0;

