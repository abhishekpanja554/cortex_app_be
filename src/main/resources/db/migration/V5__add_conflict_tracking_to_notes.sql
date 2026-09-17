ALTER TABLE notes ADD COLUMN conflict_of UUID REFERENCES notes(id) ON DELETE SET NULL;
ALTER TABLE notes ADD COLUMN conflict_field VARCHAR(20);
CREATE INDEX idx_notes_conflict_of ON notes(conflict_of);