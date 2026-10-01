CREATE TABLE device_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(200) NOT NULL,
    app         VARCHAR(20)  NOT NULL,
    environment VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_device_tokens_token UNIQUE (token)
);
CREATE INDEX idx_device_tokens_user_app ON device_tokens (user_id, app);
