CREATE TABLE appointments (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id          UUID           NOT NULL REFERENCES users(id),
    business_id          UUID           NOT NULL REFERENCES businesses(id),
    employee_id          UUID           NOT NULL REFERENCES employees(id),
    service_id           UUID           NOT NULL REFERENCES services(id),
    start_date_time      TIMESTAMPTZ    NOT NULL,
    end_date_time        TIMESTAMPTZ    NOT NULL,
    status               VARCHAR(32)    NOT NULL,
    price                NUMERIC(12, 2) NOT NULL,
    customer_note        VARCHAR(1000),
    cancellation_reason  VARCHAR(500),
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    during               TSTZRANGE GENERATED ALWAYS AS (tstzrange(start_date_time, end_date_time, '[)')) STORED,
    CONSTRAINT chk_appointments_range CHECK (start_date_time < end_date_time),
    CONSTRAINT chk_appointments_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW'))
);

CREATE INDEX idx_appointments_employee_id ON appointments (employee_id);
CREATE INDEX idx_appointments_start ON appointments (start_date_time);
CREATE INDEX idx_appointments_end ON appointments (end_date_time);
CREATE INDEX idx_appointments_status ON appointments (status);
CREATE INDEX idx_appointments_customer_id ON appointments (customer_id);
CREATE INDEX idx_appointments_business_id ON appointments (business_id);

ALTER TABLE appointments
    ADD CONSTRAINT appointments_no_overlap
    EXCLUDE USING gist (
        employee_id WITH =,
        during WITH &&
    ) WHERE (status IN ('PENDING', 'CONFIRMED'));
