ALTER TABLE reports
ADD COLUMN description VARCHAR(500) NOT NULL DEFAULT '';

ALTER TABLE reports
ADD CONSTRAINT uk_reporter_reported_user UNIQUE (reporter_id, reported_user_id);
