CREATE TABLE services (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id      UUID           NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    name             VARCHAR(200)   NOT NULL,
    description      TEXT,
    duration_minutes INTEGER        NOT NULL,
    price            NUMERIC(12, 2) NOT NULL,
    currency         VARCHAR(3)     NOT NULL DEFAULT 'TRY',
    is_active        BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_services_duration CHECK (duration_minutes > 0),
    CONSTRAINT chk_services_price CHECK (price >= 0)
);

CREATE INDEX idx_services_business_id ON services (business_id);

CREATE TABLE employee_services (
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    service_id  UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    PRIMARY KEY (employee_id, service_id)
);
