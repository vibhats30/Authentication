-- Add the enabled column to users table
-- This should be run if the column doesn't exist yet

-- Add enabled column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='users' AND column_name='enabled') THEN
        ALTER TABLE users ADD COLUMN enabled BOOLEAN;

        -- Set default values for existing users
        -- OAuth users should be enabled
        UPDATE users SET enabled = true WHERE provider != 'LOCAL';

        -- LOCAL users with verified email should be enabled
        UPDATE users SET enabled = true WHERE provider = 'LOCAL' AND email_verified = true;

        -- LOCAL users without verified email should be disabled
        UPDATE users SET enabled = false WHERE provider = 'LOCAL' AND email_verified = false;

        RAISE NOTICE 'Column enabled added and existing users updated successfully';
    ELSE
        RAISE NOTICE 'Column enabled already exists, skipping creation';

        -- Still update existing users that might have NULL values
        UPDATE users SET enabled = true
        WHERE provider != 'LOCAL' AND (enabled IS NULL OR enabled = false);

        UPDATE users SET enabled = true
        WHERE provider = 'LOCAL' AND email_verified = true AND (enabled IS NULL OR enabled = false);

        UPDATE users SET enabled = false
        WHERE provider = 'LOCAL' AND email_verified = false AND enabled IS NULL;

        RAISE NOTICE 'Existing users updated successfully';
    END IF;
END $$;

-- Verify the changes
SELECT
    provider,
    COUNT(*) as total_users,
    SUM(CASE WHEN enabled = true THEN 1 ELSE 0 END) as enabled_count,
    SUM(CASE WHEN enabled = false THEN 1 ELSE 0 END) as disabled_count,
    SUM(CASE WHEN enabled IS NULL THEN 1 ELSE 0 END) as null_count
FROM users
GROUP BY provider
ORDER BY provider;
