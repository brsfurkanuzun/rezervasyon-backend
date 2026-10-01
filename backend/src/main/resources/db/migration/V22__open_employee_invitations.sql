-- An invitation without an employee creates the expert profile from the invitee's account on acceptance.
ALTER TABLE employee_invitations ALTER COLUMN employee_id DROP NOT NULL;

CREATE INDEX idx_employee_invitations_business_pending
    ON employee_invitations (business_id) WHERE status = 'PENDING';
