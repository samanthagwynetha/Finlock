CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    from_wallet_id UUID NOT NULL REFERENCES wallets(id),
    to_wallet_id UUID NOT NULL REFERENCES wallets(id),

    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',

    idempotency_key VARCHAR(225) NOT NULL UNIQUE,

    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_from_wallet ON transactions(from_wallet_id);
CREATE INDEX idx_transactions_to_wallet ON transactions(to_wallet_id);