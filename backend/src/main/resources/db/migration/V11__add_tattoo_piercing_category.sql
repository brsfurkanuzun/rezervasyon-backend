INSERT INTO categories (code, name)
VALUES ('TATTOO_PIERCING', 'Dövme Piercing')
ON CONFLICT (code) DO NOTHING;
