# OAuth2 Authentication Fix - Database Migration Instructions

## Problem
After adding the email verification feature, OAuth2 authentication (Google, Facebook, GitHub, Twitter) stopped working for existing users because they have `enabled=null` in the database.

## Solution
Run the provided SQL migration script to fix existing users.

## Instructions

### Option 1: Using psql Command Line

```bash
# Connect to your database and run the migration script
psql -U postgres -d authdb_dev -f fix-existing-oauth-users.sql
```

### Option 2: Using pgAdmin or Database GUI

1. Open your database management tool (pgAdmin, DBeaver, etc.)
2. Connect to your `authdb_dev` database
3. Open and execute the `fix-existing-oauth-users.sql` script

### Option 3: Manual SQL Execution

Connect to your database and run these commands:

```sql
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
```

## Verification

After running the migration, verify the changes:

```sql
-- Check that OAuth users are enabled
SELECT email, provider, enabled, email_verified
FROM users
WHERE provider != 'LOCAL';

-- Check that LOCAL users have correct enabled status
SELECT email, provider, enabled, email_verified
FROM users
WHERE provider = 'LOCAL';
```

Expected results:
- All OAuth users should have `enabled = true`
- LOCAL users with `email_verified = true` should have `enabled = true`
- LOCAL users with `email_verified = false` should have `enabled = false`

## After Migration

1. Restart your backend application
2. Try logging in with Google OAuth - it should work now
3. New OAuth users will automatically have `enabled = true` set during registration

## Notes

- This is a one-time migration needed for existing users
- New users created after this fix will have the correct `enabled` status automatically
- The fix is already deployed to the `feature/email-verification` branch
