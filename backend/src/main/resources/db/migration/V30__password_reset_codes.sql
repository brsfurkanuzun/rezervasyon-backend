-- Six-digit codes emailed for "forgot password". Only the newest row per user is accepted; older rows
-- stay for an hour so the request limit can count them.
CREATE TABLE password_reset_codes (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash  VARCHAR(100) NOT NULL,
    attempts   INTEGER      NOT NULL DEFAULT 0,
    expires_at TIMESTAMPTZ  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_password_reset_codes_user ON password_reset_codes (user_id, created_at DESC);
