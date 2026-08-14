ALTER TABLE identity_user
    DROP CONSTRAINT IF EXISTS identity_user_token_version_check;

ALTER TABLE identity_user
    DROP COLUMN IF EXISTS token_version;

ALTER TABLE identity_audit_log
    DROP COLUMN IF EXISTS session_id,
    DROP COLUMN IF EXISTS client_id;
