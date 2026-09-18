CREATE TABLE embedding_jobs (
    id                  UUID            PRIMARY KEY,
    note_id             UUID            NOT NULL,
    owner_id            UUID            NOT NULL,
    job_type            VARCHAR         NOT NULL,
    status              VARCHAR         NOT NULL DEFAULT 'PENDING',
    error_message       TEXT            NULL,
    created_at          TIMESTAMPTZ     NOT NULL,
    updated_at          TIMESTAMPTZ     NOT NULL
);

CREATE INDEX idx_embedding_jobs_status_created ON embedding_jobs(status, created_at);