ALTER TABLE users ADD COLUMN onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE;

-- 기존 사용자는 이미 닉네임을 설정하고 앱을 사용 중이므로 온보딩 완료로 간주.
-- 단, 카카오 기본 닉네임(kakao_<id>)에 머물러 있는 사용자는 미완료로 남긴다.
UPDATE users SET onboarding_completed = TRUE WHERE nickname NOT LIKE 'kakao\_%';
