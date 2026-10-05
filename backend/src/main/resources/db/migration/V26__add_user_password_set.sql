ALTER TABLE users ADD COLUMN password_set BOOLEAN NOT NULL DEFAULT TRUE;

-- Social sign-ups were given a random password the user never saw.
UPDATE users SET password_set = FALSE
WHERE apple_user_id IS NOT NULL OR google_user_id IS NOT NULL;
