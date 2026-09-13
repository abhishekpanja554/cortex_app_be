CREATE TABLE users (
    id                  UUID            PRIMARY KEY,
    email               VARCHAR(255)     NOT NULL UNIQUE,
    password_hash       VARCHAR         NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL,
    updated_at          TIMESTAMPTZ     NOT NULL
);

CREATE TABLE notes (
    id                  UUID            PRIMARY KEY,
    title               VARCHAR         NOT NULL,
    body                TEXT            NOT NULL,
    owner_id            UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at          TIMESTAMPTZ     NOT NULL,
    updated_at          TIMESTAMPTZ     NOT NULL
);

CREATE INDEX idx_notes_owner ON notes(owner_id);

CREATE TABLE tags (
    id                  UUID            PRIMARY KEY,
    name                VARCHAR         NOT NULL,
    owner_id            UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at          TIMESTAMPTZ     NOT NULL,
    updated_at          TIMESTAMPTZ     NOT NULL,
    UNIQUE (owner_id, name)
);

CREATE TABLE attachments (
    id                  UUID            PRIMARY KEY,
    note_id             UUID            NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
    filename            VARCHAR         NOT NULL,
    content_type        VARCHAR         NOT NULL,
    size_bytes          BIGINT          NOT NULL,
    storage_path        TEXT            NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL,
    updated_at          TIMESTAMPTZ     NOT NULL
);

CREATE INDEX idx_attachments_note ON attachments(note_id);

CREATE TABLE note_tags (
    note_id             UUID            NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
    tag_id              UUID            NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (note_id, tag_id)
);

CREATE INDEX idx_note_tag ON note_tags(tag_id);