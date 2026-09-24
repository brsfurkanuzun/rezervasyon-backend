CREATE TABLE employee_portfolio_urls (
    employee_id UUID NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    url         TEXT NOT NULL,
    sort_order  INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_employee_portfolio_employee ON employee_portfolio_urls (employee_id);
