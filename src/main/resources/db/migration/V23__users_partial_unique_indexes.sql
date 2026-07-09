-- =============================================================
-- V23: users.nickname / users.firebase_uid를 전역 UNIQUE에서
-- partial unique index(WHERE deleted_at IS NULL)로 전환.
-- 소프트 삭제된 유저의 nickname/firebase_uid가 재사용을 막지 않도록 함.
-- =============================================================

ALTER TABLE users DROP CONSTRAINT users_nickname_key;
ALTER TABLE users DROP CONSTRAINT users_firebase_uid_key;

CREATE UNIQUE INDEX users_nickname_key ON users(nickname) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX users_firebase_uid_key ON users(firebase_uid) WHERE deleted_at IS NULL;