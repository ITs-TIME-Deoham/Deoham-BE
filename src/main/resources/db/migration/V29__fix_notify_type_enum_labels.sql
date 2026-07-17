-- V9 renamed ask_* domain concepts to card_*, but missed the notify_type enum labels.
-- Align them so NotifyType.NEW_CARD / CARD_APPLIED can actually be persisted.
ALTER TYPE notify_type RENAME VALUE 'NEW_ASK' TO 'NEW_CARD';
ALTER TYPE notify_type RENAME VALUE 'ASK_APPLIED' TO 'CARD_APPLIED';