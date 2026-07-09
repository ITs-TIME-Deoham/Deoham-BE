-- =============================================================
-- V22: notify_type / report_target enum 값 리네임 (ask_* → card_*)
-- V9에서 ask_category/ask_status/apply_status는 리네임했지만
-- notify_type/report_target 값은 누락되어 Java enum과 불일치했음.
-- =============================================================

ALTER TYPE notify_type RENAME VALUE 'NEW_ASK' TO 'NEW_CARD';
ALTER TYPE notify_type RENAME VALUE 'ASK_APPLIED' TO 'CARD_APPLIED';

ALTER TYPE report_target RENAME VALUE 'ASK' TO 'CARD';