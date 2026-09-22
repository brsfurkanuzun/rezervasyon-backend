-- Align integer widths with Hibernate Java int mapping
ALTER TABLE reviews
    ALTER COLUMN rating TYPE INTEGER
    USING rating::INTEGER;

ALTER TABLE working_hours
    ALTER COLUMN day_of_week TYPE INTEGER
    USING day_of_week::INTEGER;
