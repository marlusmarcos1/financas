CREATE TABLE account (
    id               UUID PRIMARY KEY,
    user_id          UUID NOT NULL REFERENCES app_user (id),
    name             VARCHAR(120) NOT NULL,
    type             VARCHAR(20) NOT NULL CHECK (type IN ('CHECKING', 'SAVINGS_BOX', 'BROKERAGE', 'CASH')),
    institution      VARCHAR(120),
    initial_balance  NUMERIC(14, 2) NOT NULL DEFAULT 0,
    purpose          VARCHAR(20) NOT NULL CHECK (purpose IN ('DAILY', 'EMERGENCY_RESERVE', 'GOAL', 'INVESTMENT')),
    archived         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_account_user ON account (user_id);

CREATE TABLE credit_card (
    id                          UUID PRIMARY KEY,
    user_id                     UUID NOT NULL REFERENCES app_user (id),
    name                        VARCHAR(120) NOT NULL,
    issuer                      VARCHAR(120),
    credit_limit                NUMERIC(14, 2) NOT NULL CHECK (credit_limit >= 0),
    closing_day                 SMALLINT NOT NULL CHECK (closing_day BETWEEN 1 AND 31),
    due_day                     SMALLINT NOT NULL CHECK (due_day BETWEEN 1 AND 31),
    default_payment_account_id  UUID REFERENCES account (id),
    color                       VARCHAR(7),
    archived                    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_credit_card_user ON credit_card (user_id);

CREATE TABLE category (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES app_user (id),
    name        VARCHAR(120) NOT NULL,
    kind        VARCHAR(10) NOT NULL CHECK (kind IN ('INCOME', 'EXPENSE')),
    nature      VARCHAR(10) NOT NULL CHECK (nature IN ('FIXED', 'VARIABLE')),
    parent_id   UUID REFERENCES category (id),
    icon        VARCHAR(40),
    color       VARCHAR(7),
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_category_user ON category (user_id);

CREATE TABLE budget (
    id            UUID PRIMARY KEY,
    user_id       UUID NOT NULL REFERENCES app_user (id),
    category_id   UUID NOT NULL REFERENCES category (id),
    month         VARCHAR(7),
    limit_amount  NUMERIC(14, 2) NOT NULL CHECK (limit_amount >= 0),
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_budget_user ON budget (user_id);
CREATE UNIQUE INDEX uk_budget_category_default ON budget (category_id) WHERE month IS NULL;
CREATE UNIQUE INDEX uk_budget_category_month ON budget (category_id, month) WHERE month IS NOT NULL;

CREATE TABLE app_setting (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES app_user (id),
    key         VARCHAR(60) NOT NULL,
    value       VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_app_setting_user_key UNIQUE (user_id, key)
);
