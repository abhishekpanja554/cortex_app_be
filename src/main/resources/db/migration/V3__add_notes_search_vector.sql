ALTER TABLE notes ADD COLUMN search_vector
    tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('english', title), 'A') ||
        setweight(to_tsvector('english', body), 'B')
    ) STORED;

CREATE INDEX idx_notes_search_vector ON notes USING GIN (search_vector);