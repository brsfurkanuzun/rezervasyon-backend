-- Businesses now go live on creation; admins can still suspend them.
UPDATE businesses SET status = 'ACTIVE' WHERE status = 'PENDING_APPROVAL';
