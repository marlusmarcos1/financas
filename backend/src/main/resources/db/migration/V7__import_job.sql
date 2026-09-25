CREATE TABLE import_job (
    id            UUID PRIMARY KEY,
    user_id       UUID NOT NULL REFERENCES app_user (id),
    filename      VARCHAR(255) NOT NULL,
    mode          VARCHAR(10) NOT NULL CHECK (mode IN ('MERGE', 'REPLACE')),
    status        VARCHAR(20) NOT NULL CHECK (status IN ('DRY_RUN', 'APPLIED', 'FAILED')),
    summary_json  TEXT NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_import_job_user ON import_job (user_id);
