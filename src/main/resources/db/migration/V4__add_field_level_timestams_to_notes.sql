ALTER TABLE notes ADD COLUMN title_updated_at TIMESTAMPTZ;
ALTER TABLE notes ADD COLUMN body_updated_at TIMESTAMPTZ;
UPDATE notes SET title_updated_at = notes.updated_at, body_updated_at = notes.updated_at;
ALTER TABLE notes ALTER COLUMN title_updated_at SET NOT NULL;
ALTER TABLE notes ALTER COLUMN body_updated_at SET NOT NULL;