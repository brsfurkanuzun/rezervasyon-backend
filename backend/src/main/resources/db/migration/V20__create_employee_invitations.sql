CREATE TABLE employee_invitations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID         NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    employee_id UUID         NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    code        VARCHAR(16)  NOT NULL,
    email       VARCHAR(255),
    status      VARCHAR(16)  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    accepted_by UUID         REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_employee_invitations_code UNIQUE (code),
    CONSTRAINT chk_employee_invitations_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REVOKED'))
);
CREATE INDEX idx_employee_invitations_employee ON employee_invitations (employee_id);
CREATE INDEX idx_employee_invitations_pending_email ON employee_invitations (lower(email)) WHERE status = 'PENDING';

-- A user can hold at most one staff seat per business.
CREATE UNIQUE INDEX uk_employees_business_user ON employees (business_id, user_id) WHERE user_id IS NOT NULL;
