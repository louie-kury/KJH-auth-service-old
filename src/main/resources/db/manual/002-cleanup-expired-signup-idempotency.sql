-- Run as a periodic maintenance job on the auth database, after reviewing target and credentials.
-- Repeat this bounded batch until zero rows are deleted; avoid printing row values.
-- InnoDB row locks serialize this DELETE with a concurrent signup using the same key.
-- Only committed records whose replay and proof-protection retention window expired are removed.
DELETE FROM signup_idempotency
WHERE expires_at <= UTC_TIMESTAMP(6)
ORDER BY expires_at
LIMIT 500;
