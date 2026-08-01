DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM gateway_application
        WHERE deleted = FALSE
        GROUP BY biz_code, application_code, env
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION
            'duplicate active gateway application physical identity';
    END IF;
END $$;

DROP INDEX IF EXISTS uk_gateway_application_scope_active;

CREATE UNIQUE INDEX uk_gateway_application_physical_active
    ON gateway_application (biz_code, application_code, env)
    WHERE deleted = FALSE;
