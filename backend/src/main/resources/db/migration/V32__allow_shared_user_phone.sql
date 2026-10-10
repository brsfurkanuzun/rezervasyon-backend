-- Phone is contact info, not a sign-in identifier; several accounts may share one.
DROP INDEX IF EXISTS uk_users_phone;
