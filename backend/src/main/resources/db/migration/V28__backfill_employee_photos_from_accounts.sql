-- Expert profiles linked to an account but created before the account had a photo.
UPDATE employees e
SET photo_url = u.photo_url
FROM users u
WHERE e.user_id = u.id
  AND e.photo_url IS NULL
  AND u.photo_url IS NOT NULL;
