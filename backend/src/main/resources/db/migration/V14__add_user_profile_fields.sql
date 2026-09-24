ALTER TABLE users
    ADD COLUMN photo_url VARCHAR(1000),
    ADD COLUMN birth_date DATE,
    ADD COLUMN gender VARCHAR(32);