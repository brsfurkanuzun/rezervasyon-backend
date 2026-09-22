CREATE TABLE categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(64)  NOT NULL,
    name        VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_categories_code UNIQUE (code)
);

INSERT INTO categories (code, name) VALUES
    ('BEAUTY_SALON', 'Beauty Salon'),
    ('HAIRDRESSER', 'Hairdresser'),
    ('BARBER', 'Barber'),
    ('NAIL_SALON', 'Nail Salon'),
    ('SPA', 'Spa'),
    ('MASSAGE', 'Massage'),
    ('PSYCHOLOGIST', 'Psychologist'),
    ('DIETITIAN', 'Dietitian'),
    ('PILATES', 'Pilates'),
    ('YOGA', 'Yoga'),
    ('MAKEUP', 'Makeup'),
    ('SKIN_CARE', 'Skin Care'),
    ('OTHER', 'Other');
