CREATE TABLE appointment_services (
    appointment_id UUID NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    service_id UUID NOT NULL REFERENCES services(id),
    service_order INTEGER NOT NULL,
    PRIMARY KEY (appointment_id, service_id),
    CONSTRAINT uq_appointment_services_order UNIQUE (appointment_id, service_order)
);

INSERT INTO appointment_services (appointment_id, service_id, service_order)
SELECT id, service_id, 0
FROM appointments;

CREATE INDEX idx_appointment_services_service_id ON appointment_services (service_id);