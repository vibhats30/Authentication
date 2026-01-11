-- Fix existing OAuth users to have enabled=true
-- This is needed because the enabled column was added after some users were created
-- OAuth users should always be enabled since their email is verified by the provider

-- Set enabled=true for all OAuth users (non-LOCAL provider)
UPDATE users
SET enabled = true
WHERE provider != 'LOCAL'
  AND (enabled IS NULL OR enabled = false);

-- Set enabled=true for LOCAL users who have emailVerified=true
UPDATE users
SET enabled = true
WHERE provider = 'LOCAL'
  AND email_verified = true
  AND (enabled IS NULL OR enabled = false);

-- Set enabled=false for LOCAL users who have not verified their email
UPDATE users
SET enabled = false
WHERE provider = 'LOCAL'
  AND email_verified = false
  AND enabled IS NULL;
