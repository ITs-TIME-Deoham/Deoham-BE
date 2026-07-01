-- Align the database contract after the ask-to-card rename.

UPDATE cards
SET expires_at = created_at + INTERVAL '24 hours'
WHERE expires_at IS NULL;

ALTER TABLE cards
    DROP COLUMN IF EXISTS title,
    ALTER COLUMN expires_at SET NOT NULL;

ALTER TYPE report_target RENAME VALUE 'ASK' TO 'CARD';

ALTER TYPE notify_type RENAME VALUE 'NEW_ASK'     TO 'NEW_CARD';
ALTER TYPE notify_type RENAME VALUE 'ASK_APPLIED'  TO 'CARD_APPLIED';
