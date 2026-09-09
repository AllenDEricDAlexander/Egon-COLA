ALTER TABLE rbac3_business_participation
    ADD COLUMN application_code VARCHAR(128);

ALTER TABLE rbac3_operation_sod_rule
    ADD COLUMN application_code VARCHAR(128);

ALTER TABLE rbac3_service_permission
    ADD COLUMN application_code VARCHAR(128);

UPDATE rbac3_business_participation participation
SET application_code = application.application_code
FROM rbac3_application application
WHERE application.id = participation.application_id;

UPDATE rbac3_operation_sod_rule rule
SET application_code = application.application_code
FROM rbac3_application application
WHERE application.id = rule.application_id;

UPDATE rbac3_service_permission permission
SET application_code = application.application_code
FROM rbac3_application application
WHERE application.id = permission.application_id;

ALTER TABLE rbac3_business_participation
    ALTER COLUMN application_code SET NOT NULL;

ALTER TABLE rbac3_operation_sod_rule
    ALTER COLUMN application_code SET NOT NULL;

ALTER TABLE rbac3_service_permission
    ALTER COLUMN application_code SET NOT NULL;

CREATE UNIQUE INDEX uk_rbac3_application_id_code
    ON rbac3_application (id, application_code);

ALTER TABLE rbac3_business_participation
    ADD CONSTRAINT fk_rbac3_business_participation_application_code
        FOREIGN KEY (application_id, application_code)
        REFERENCES rbac3_application (id, application_code);

ALTER TABLE rbac3_operation_sod_rule
    ADD CONSTRAINT fk_rbac3_operation_sod_application_code
        FOREIGN KEY (application_id, application_code)
        REFERENCES rbac3_application (id, application_code);

ALTER TABLE rbac3_service_permission
    ADD CONSTRAINT fk_rbac3_service_permission_application_code
        FOREIGN KEY (application_id, application_code)
        REFERENCES rbac3_application (id, application_code);

CREATE FUNCTION rbac3_sync_application_identity()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.application_id IS NULL AND NEW.application_code IS NOT NULL THEN
        SELECT application.id
        INTO NEW.application_id
        FROM rbac3_application application
        WHERE application.application_code = NEW.application_code;
    ELSIF NEW.application_code IS NULL AND NEW.application_id IS NOT NULL THEN
        SELECT application.application_code
        INTO NEW.application_code
        FROM rbac3_application application
        WHERE application.id = NEW.application_id;
    ELSIF NEW.application_id IS NOT NULL
            AND NEW.application_code IS NOT NULL THEN
        PERFORM 1
        FROM rbac3_application application
        WHERE application.id = NEW.application_id
          AND application.application_code = NEW.application_code;
        IF NOT FOUND THEN
            RAISE EXCEPTION
                'RBAC3 application id and code do not identify the same application'
                USING ERRCODE = '23503';
        END IF;
    END IF;

    IF NEW.application_id IS NULL OR NEW.application_code IS NULL THEN
        RAISE EXCEPTION 'RBAC3 application identity is required'
            USING ERRCODE = '23502';
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER trg_rbac3_business_participation_application_identity
    BEFORE INSERT OR UPDATE OF application_id, application_code
    ON rbac3_business_participation
    FOR EACH ROW
    EXECUTE FUNCTION rbac3_sync_application_identity();

CREATE TRIGGER trg_rbac3_operation_sod_application_identity
    BEFORE INSERT OR UPDATE OF application_id, application_code
    ON rbac3_operation_sod_rule
    FOR EACH ROW
    EXECUTE FUNCTION rbac3_sync_application_identity();

CREATE TRIGGER trg_rbac3_service_permission_application_identity
    BEFORE INSERT OR UPDATE OF application_id, application_code
    ON rbac3_service_permission
    FOR EACH ROW
    EXECUTE FUNCTION rbac3_sync_application_identity();
