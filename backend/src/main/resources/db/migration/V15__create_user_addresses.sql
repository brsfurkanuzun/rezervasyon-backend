CREATE TABLE user_addresses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    label           VARCHAR(80) NOT NULL,
    recipient_name  VARCHAR(200) NOT NULL,
    phone           VARCHAR(32),
    address_line    VARCHAR(500) NOT NULL,
    city            VARCHAR(100) NOT NULL,
    district        VARCHAR(100),
    postal_code     VARCHAR(20),
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_addresses_user_id ON user_addresses(user_id);