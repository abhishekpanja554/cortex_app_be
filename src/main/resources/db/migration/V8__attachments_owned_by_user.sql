ALTER TABLE attachments ADD COLUMN owner_id UUID REFERENCES users(id) ON DELETE CASCADE;

UPDATE attachments a
SET owner_id = n.owner_id
FROM notes n
WHERE n.id = a.note_id;

ALTER TABLE attachments ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE attachments ALTER COLUMN note_id DROP NOT NULL;

CREATE INDEX idx_attachments_owner ON attachments(owner_id);
