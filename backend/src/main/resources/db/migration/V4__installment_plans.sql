CREATE TABLE installment_plan (
    id                          UUID PRIMARY KEY,
    user_id                     UUID NOT NULL REFERENCES app_user (id),
    card_id                     UUID NOT NULL REFERENCES credit_card (id),
    description                 VARCHAR(160) NOT NULL,
    purchase_date               DATE NOT NULL,
    total_amount                NUMERIC(14, 2) NOT NULL CHECK (total_amount >= 0),
    installment_count           SMALLINT NOT NULL CHECK (installment_count > 0),
    installment_amount          NUMERIC(14, 2) NOT NULL,
    first_installment_number    SMALLINT NOT NULL CHECK (first_installment_number > 0),
    first_invoice_month         VARCHAR(7) NOT NULL,
    interest_rate_monthly       NUMERIC(7, 4),
    category_id                 UUID REFERENCES category (id),
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_installment_plan_first_number CHECK (first_installment_number <= installment_count)
);
CREATE INDEX idx_installment_plan_user ON installment_plan (user_id);

ALTER TABLE transaction
    ADD CONSTRAINT fk_transaction_installment_plan
    FOREIGN KEY (installment_plan_id) REFERENCES installment_plan (id);
