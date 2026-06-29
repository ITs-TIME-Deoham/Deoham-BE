-- =============================================================
-- V9: Rename ask_* → card_*
-- ask_posts  → cards
-- ask_applies → card_applies
-- ask_category enum → card_category
-- ask_status enum   → card_status
-- apply_status enum → card_apply_status
-- chat_rooms.ask_id → chat_rooms.card_id
-- =============================================================

-- Rename enum types
ALTER TYPE ask_category RENAME TO card_category;
ALTER TYPE ask_status   RENAME TO card_status;
ALTER TYPE apply_status RENAME TO card_apply_status;

-- Rename tables
ALTER TABLE ask_posts   RENAME TO cards;
ALTER TABLE ask_applies RENAME TO card_applies;

-- Rename FK column in card_applies: ask_id → card_id
ALTER TABLE card_applies RENAME COLUMN ask_id TO card_id;

-- Rename FK column in chat_rooms: ask_id → card_id
ALTER TABLE chat_rooms RENAME COLUMN ask_id TO card_id;

-- Rename indexes on cards (was ask_posts)
ALTER INDEX ask_posts_location_idx   RENAME TO cards_location_idx;
ALTER INDEX idx_ask_posts_author_id  RENAME TO idx_cards_author_id;
ALTER INDEX idx_ask_posts_status     RENAME TO idx_cards_status;

-- Rename trigger on cards
ALTER TRIGGER ask_posts_set_updated_at ON cards RENAME TO cards_set_updated_at;

-- Rename indexes on card_applies (was ask_applies)
ALTER INDEX idx_ask_applies_ask_id       RENAME TO idx_card_applies_card_id;
ALTER INDEX idx_ask_applies_applicant_id RENAME TO idx_card_applies_applicant_id;

-- Rename unique constraint on card_applies
ALTER TABLE card_applies
    RENAME CONSTRAINT ask_applies_ask_id_applicant_id_key
    TO card_applies_card_id_applicant_id_key;

-- Rename FK index on chat_rooms
ALTER INDEX idx_chat_rooms_ask_id RENAME TO idx_chat_rooms_card_id;
