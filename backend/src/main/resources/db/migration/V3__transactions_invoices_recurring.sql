CREATE TABLE recurring_rule (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES app_user (id),
    description         VARCHAR(160) NOT NULL,
    amount              NUMERIC(14, 2) NOT NULL CHECK (amount >= 0),
    amount_is_variable  BOOLEAN NOT NULL DEFAULT FALSE,
    frequency           VARCHAR(10) NOT NULL CHECK (frequency IN ('MONTHLY', 'YEARLY')),
    day_of_month        SMALLINT NOT NULL CHECK (day_of_month BETWEEN 1 AND 31),
    start_date          DATE NOT NULL,
    end_date            DATE,
    category_id         UUID NOT NULL REFERENCES category (id),
    card_id             UUID REFERENCES credit_card (id),
    account_id          UUID REFERENCES account (id),
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_recurring_rule_single_payment_method
        CHECK ((card_id IS NOT NULL) != (account_id IS NOT NULL))
);
CREATE INDEX idx_recurring_rule_user ON recurring_rule (user_id);

CREATE TABLE invoice (
    id                     UUID PRIMARY KEY,
    user_id                UUID NOT NULL REFERENCES app_user (id),
    card_id                UUID NOT NULL REFERENCES credit_card (id),
    reference_month        VARCHAR(7) NOT NULL,
    closing_date           DATE NOT NULL,
    due_date               DATE NOT NULL,
    status                 VARCHAR(10) NOT NULL CHECK (status IN ('OPEN', 'CLOSED', 'PAID')),
    paid_amount            NUMERIC(14, 2) NOT NULL DEFAULT 0,
    paid_on                DATE,
    paid_from_account_id   UUID REFERENCES account (id),
    created_at             TIMESTAMPTZ NOT NULL,
    updated_at             TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_invoice_card_month UNIQUE (card_id, reference_month)
);
CREATE INDEX idx_invoice_user ON invoice (user_id);

CREATE TABLE transaction (
    id                   UUID PRIMARY KEY,
    user_id              UUID NOT NULL REFERENCES app_user (id),
    kind                 VARCHAR(10) NOT NULL CHECK (kind IN ('INCOME', 'EXPENSE', 'TRANSFER')),
    description          VARCHAR(160) NOT NULL,
    amount               NUMERIC(14, 2) NOT NULL CHECK (amount >= 0),
    date                 DATE NOT NULL,
    category_id          UUID REFERENCES category (id),
    account_id           UUID REFERENCES account (id),
    card_id              UUID REFERENCES credit_card (id),
    invoice_id           UUID REFERENCES invoice (id),
    status               VARCHAR(10) NOT NULL CHECK (status IN ('PLANNED', 'PAID')),
    installment_plan_id  UUID,
    installment_number   SMALLINT,
    recurring_rule_id    UUID REFERENCES recurring_rule (id),
    income_entry_id      UUID,
    notes                VARCHAR(500),
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_transaction_single_payment_method
        CHECK (NOT (account_id IS NOT NULL AND card_id IS NOT NULL))
);
CREATE INDEX idx_transaction_user_date ON transaction (user_id, date);
CREATE INDEX idx_transaction_card ON transaction (card_id);
CREATE INDEX idx_transaction_invoice ON transaction (invoice_id);
CREATE INDEX idx_transaction_account ON transaction (account_id);
