CREATE TABLE income_source (
    id                       UUID PRIMARY KEY,
    user_id                  UUID NOT NULL REFERENCES app_user (id),
    name                     VARCHAR(160) NOT NULL,
    type                     VARCHAR(20) NOT NULL CHECK (type IN ('SALARY', 'SCHOLARSHIP', 'THIRTEENTH', 'EXTRA', 'OTHER')),
    recurrence               VARCHAR(20) NOT NULL CHECK (recurrence IN ('MONTHLY', 'TEMPORARY', 'SPORADIC')),
    expected_amount          NUMERIC(14, 2) NOT NULL CHECK (expected_amount >= 0),
    pay_day                  SMALLINT CHECK (pay_day BETWEEN 1 AND 31),
    start_date               DATE,
    end_date                 DATE,
    expected_months          SMALLINT,
    tithe_applies            BOOLEAN NOT NULL DEFAULT TRUE,
    counts_in_base_budget    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at               TIMESTAMPTZ NOT NULL,
    updated_at               TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_income_source_user ON income_source (user_id);

CREATE TABLE income_entry (
    id                UUID PRIMARY KEY,
    user_id           UUID NOT NULL REFERENCES app_user (id),
    source_id         UUID NOT NULL REFERENCES income_source (id),
    account_id        UUID REFERENCES account (id),
    reference_month   VARCHAR(7) NOT NULL,
    received_on       DATE,
    amount            NUMERIC(14, 2) NOT NULL CHECK (amount >= 0),
    status            VARCHAR(10) NOT NULL CHECK (status IN ('EXPECTED', 'RECEIVED')),
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_income_entry_user_month ON income_entry (user_id, reference_month);
CREATE INDEX idx_income_entry_source ON income_entry (source_id);

CREATE TABLE tithe_ledger (
    id                UUID PRIMARY KEY,
    user_id           UUID NOT NULL REFERENCES app_user (id),
    reference_month   VARCHAR(7) NOT NULL,
    base_amount       NUMERIC(14, 2) NOT NULL DEFAULT 0,
    percent           NUMERIC(5, 2) NOT NULL,
    due_amount        NUMERIC(14, 2) NOT NULL DEFAULT 0,
    paid_amount       NUMERIC(14, 2) NOT NULL DEFAULT 0,
    paid_on           DATE,
    status            VARCHAR(10) NOT NULL CHECK (status IN ('PENDING', 'PAID')),
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_tithe_ledger_user_month UNIQUE (user_id, reference_month)
);
