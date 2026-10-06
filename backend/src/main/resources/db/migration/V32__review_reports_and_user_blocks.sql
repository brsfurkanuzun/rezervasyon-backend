-- Customers can report a review or block its author. Reported reviews and blocked authors are hidden from
-- that customer; reports are kept for moderation.
CREATE TABLE review_reports (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id   UUID        NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    reporter_id UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason      VARCHAR(500),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_review_reports_reporter UNIQUE (review_id, reporter_id)
);

CREATE INDEX idx_review_reports_reporter ON review_reports (reporter_id);

CREATE TABLE user_blocks (
    blocker_id UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_id UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (blocker_id, blocked_id)
);
