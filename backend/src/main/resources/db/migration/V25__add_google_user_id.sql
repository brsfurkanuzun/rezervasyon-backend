ALTER TABLE users ADD COLUMN google_user_id VARCHAR(255);
CREATE UNIQUE INDEX uk_users_google_user_id ON users (google_user_id) WHERE google_user_id IS NOT NULL;
