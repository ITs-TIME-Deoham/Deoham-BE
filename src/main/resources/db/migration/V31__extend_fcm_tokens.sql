-- =============================================================
-- V30: ONDO — fcm_tokens 확장 (기기 식별 / 플랫폼 / 갱신시각 / 유니크 제약)
-- =============================================================
-- 토큰 갱신(rotation)과 계정 전환 시 중복 row / 오발송을 막기 위해
-- device_id, platform, updated_at 컬럼과 유니크 제약을 추가한다.

CREATE TYPE fcm_platform AS ENUM ('ANDROID', 'IOS', 'WEB');

ALTER TABLE fcm_tokens ADD COLUMN device_id  TEXT;
ALTER TABLE fcm_tokens ADD COLUMN platform   fcm_platform;
ALTER TABLE fcm_tokens ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- 하나의 토큰은 한 유저에게만 귀속 (계정 전환 시 소유자 재할당으로 유지)
ALTER TABLE fcm_tokens ADD CONSTRAINT uq_fcm_tokens_token UNIQUE (token);
-- 기기당 토큰 1개 (device_id NULL은 Postgres에서 서로 distinct 취급되어 다중 허용)
ALTER TABLE fcm_tokens ADD CONSTRAINT uq_fcm_tokens_user_device UNIQUE (user_id, device_id);
