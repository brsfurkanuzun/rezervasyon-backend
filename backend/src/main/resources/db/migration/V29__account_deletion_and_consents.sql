-- Set when the owner deletes the account; the row stays (anonymised) so appointments and reviews keep their history.
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMPTZ;

-- One row per consent decision at sign-up. Removed together with the account.
CREATE TABLE user_consents (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    consent_type VARCHAR(32) NOT NULL,
    granted      BOOLEAN     NOT NULL,
    channel      VARCHAR(32),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_user_consents_type CHECK (consent_type IN ('TERMS', 'PARTNER_TERMS', 'KVKK', 'PRIVACY', 'MARKETING'))
);

CREATE INDEX idx_user_consents_user ON user_consents (user_id, consent_type, created_at DESC);
