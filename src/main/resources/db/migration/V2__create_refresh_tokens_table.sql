CREATE TABLE refresh_tokens (
    id                  UUID            PRIMARY KEY,
    user_id             UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash          VARCHAR         NOT NULL UNIQUE,
    family_id           UUID            NOT NULL,
    revoked_at          TIMESTAMPTZ,
    expires_at          TIMESTAMPTZ     NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL,
    updated_at          TIMESTAMPTZ     NOT NULL
);

CREATE INDEX idx_session_manage_user ON refresh_tokens(user_id);
CREATE INDEX idx_session_manage_family ON refresh_tokens(family_id);