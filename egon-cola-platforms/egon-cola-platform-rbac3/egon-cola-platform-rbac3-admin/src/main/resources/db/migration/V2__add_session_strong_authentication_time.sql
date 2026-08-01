ALTER TABLE rbac3_session
    ADD COLUMN strong_authenticated_at TIMESTAMPTZ;

UPDATE rbac3_session
   SET strong_authenticated_at = authenticated_at
 WHERE auth_strength IN ('MFA', 'STRONG');

ALTER TABLE rbac3_session
    ADD CONSTRAINT ck_rbac3_session_strong_authentication_time
        CHECK (
            (auth_strength = 'PASSWORD' AND strong_authenticated_at IS NULL)
            OR (auth_strength IN ('MFA', 'STRONG') AND strong_authenticated_at IS NOT NULL)
        );
