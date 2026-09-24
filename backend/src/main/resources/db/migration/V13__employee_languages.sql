CREATE TABLE employee_languages (
    employee_id UUID NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    language    VARCHAR(80) NOT NULL,
    sort_order  INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_employee_languages_employee ON employee_languages (employee_id);
