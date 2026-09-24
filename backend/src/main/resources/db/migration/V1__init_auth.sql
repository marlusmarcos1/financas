CREATE TABLE app_user (
    id             UUID PRIMARY KEY,
    username       VARCHAR(64) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    display_name   VARCHAR(120) NOT NULL,
    birth_date     DATE,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_app_user_username UNIQUE (username)
);
