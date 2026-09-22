CREATE TABLE working_hours (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id  UUID    NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    day_of_week  SMALLINT NOT NULL,
    start_time   TIME     NOT NULL,
    end_time     TIME     NOT NULL,
    is_available BOOLEAN  NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_working_hours_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT chk_working_hours_range CHECK (start_time < end_time)
);

CREATE INDEX idx_working_hours_employee_id ON working_hours (employee_id);

CREATE TABLE time_offs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id  UUID         NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    title        VARCHAR(150),
    start_at     TIMESTAMPTZ  NOT NULL,
    end_at       TIMESTAMPTZ  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_time_offs_range CHECK (start_at < end_at)
);

CREATE INDEX idx_time_offs_employee_id ON time_offs (employee_id);
CREATE INDEX idx_time_offs_range ON time_offs (start_at, end_at);
