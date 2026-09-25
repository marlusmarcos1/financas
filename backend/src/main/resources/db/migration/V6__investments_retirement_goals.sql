CREATE TABLE investment_asset (
    id                UUID PRIMARY KEY,
    user_id           UUID NOT NULL REFERENCES app_user (id),
    ticker            VARCHAR(20) NOT NULL,
    name              VARCHAR(160) NOT NULL,
    asset_class       VARCHAR(20) NOT NULL CHECK (asset_class IN ('FII', 'STOCK', 'ETF', 'TREASURY', 'CDB', 'SAVINGS_BOX', 'CRYPTO', 'OTHER')),
    subclass          VARCHAR(20),
    indexer           VARCHAR(40),
    maturity_date     DATE,
    current_price     NUMERIC(14, 6),
    price_updated_at  TIMESTAMPTZ,
    purpose           VARCHAR(20) NOT NULL CHECK (purpose IN ('RETIREMENT', 'HOUSE', 'GENERAL')),
    archived          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_investment_asset_user ON investment_asset (user_id);

CREATE TABLE investment_transaction (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES app_user (id),
    asset_id    UUID NOT NULL REFERENCES investment_asset (id),
    type        VARCHAR(10) NOT NULL CHECK (type IN ('BUY', 'SELL', 'DIVIDEND', 'JCP', 'INTEREST', 'FEE')),
    date        DATE NOT NULL,
    quantity    NUMERIC(18, 8) NOT NULL DEFAULT 0,
    unit_price  NUMERIC(14, 6) NOT NULL DEFAULT 0,
    fees        NUMERIC(14, 2) NOT NULL DEFAULT 0,
    amount      NUMERIC(14, 2) NOT NULL,
    account_id  UUID REFERENCES account (id),
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_investment_transaction_user ON investment_transaction (user_id);
CREATE INDEX idx_investment_transaction_asset ON investment_transaction (asset_id, date);

CREATE TABLE allocation_target (
    id             UUID PRIMARY KEY,
    user_id        UUID NOT NULL REFERENCES app_user (id),
    purpose        VARCHAR(20) NOT NULL CHECK (purpose IN ('RETIREMENT', 'HOUSE', 'GENERAL')),
    asset_class    VARCHAR(20) NOT NULL CHECK (asset_class IN ('FII', 'STOCK', 'ETF', 'TREASURY', 'CDB', 'SAVINGS_BOX', 'CRYPTO', 'OTHER')),
    target_percent NUMERIC(5, 2) NOT NULL CHECK (target_percent BETWEEN 0 AND 100),
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_allocation_target_purpose_class UNIQUE (user_id, purpose, asset_class)
);

CREATE TABLE retirement_plan (
    id                                  UUID PRIMARY KEY,
    user_id                             UUID NOT NULL UNIQUE REFERENCES app_user (id),
    monthly_contribution                NUMERIC(14, 2) NOT NULL,
    contribution_annual_increase_percent NUMERIC(5, 2) NOT NULL DEFAULT 0,
    start_date                          DATE NOT NULL,
    horizon_years                       SMALLINT NOT NULL CHECK (horizon_years > 0),
    expected_return_nominal_annual      NUMERIC(6, 4) NOT NULL,
    expected_inflation_annual           NUMERIC(6, 4) NOT NULL,
    current_balance                     NUMERIC(14, 2) NOT NULL DEFAULT 0,
    created_at                          TIMESTAMPTZ NOT NULL,
    updated_at                          TIMESTAMPTZ NOT NULL
);

CREATE TABLE goal (
    id                          UUID PRIMARY KEY,
    user_id                     UUID NOT NULL REFERENCES app_user (id),
    name                        VARCHAR(160) NOT NULL,
    type                        VARCHAR(20) NOT NULL CHECK (type IN ('HOUSE', 'CAR', 'EMERGENCY', 'OTHER')),
    target_amount               NUMERIC(14, 2) NOT NULL CHECK (target_amount >= 0),
    target_date                 DATE,
    linked_account_id           UUID REFERENCES account (id),
    monthly_contribution_planned NUMERIC(14, 2) NOT NULL DEFAULT 0,
    priority                    SMALLINT NOT NULL DEFAULT 0,
    notes                       VARCHAR(500),
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_goal_user ON goal (user_id);

CREATE TABLE goal_contribution (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES app_user (id),
    goal_id     UUID NOT NULL REFERENCES goal (id),
    date        DATE NOT NULL,
    amount      NUMERIC(14, 2) NOT NULL CHECK (amount >= 0),
    source      VARCHAR(20) NOT NULL CHECK (source IN ('SALARY', 'SCHOLARSHIP', 'THIRTEENTH', 'EXTRA', 'MANUAL')),
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_goal_contribution_goal ON goal_contribution (goal_id);
