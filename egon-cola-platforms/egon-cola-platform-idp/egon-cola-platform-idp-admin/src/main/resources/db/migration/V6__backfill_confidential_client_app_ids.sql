UPDATE identity_client
SET app_id = client_id
WHERE client_type = 'CONFIDENTIAL'
  AND app_id IS NULL;

ALTER TABLE identity_client
    VALIDATE CONSTRAINT ck_identity_client_confidential_app_id;
