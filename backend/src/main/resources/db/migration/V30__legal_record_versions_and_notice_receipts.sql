ALTER TABLE user_consents ADD COLUMN document_version VARCHAR(64);

CREATE TABLE user_notice_receipts (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notice_type      VARCHAR(32) NOT NULL,
    document_version VARCHAR(64) NOT NULL,
    channel          VARCHAR(32) NOT NULL,
    presented_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_user_notice_receipts_type CHECK (notice_type IN ('KVKK_NOTICE', 'PRIVACY_POLICY'))
);

CREATE INDEX idx_user_notice_receipts_user ON user_notice_receipts (user_id, notice_type, presented_at DESC);
