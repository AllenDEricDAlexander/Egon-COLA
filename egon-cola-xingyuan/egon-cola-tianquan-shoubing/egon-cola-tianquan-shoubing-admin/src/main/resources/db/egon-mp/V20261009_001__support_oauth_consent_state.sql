ALTER TABLE oauth_token ALTER COLUMN token_type TYPE VARCHAR(32);
ALTER TABLE oauth_token DROP CONSTRAINT ck_oauth_token_type;
ALTER TABLE oauth_token ADD CONSTRAINT ck_oauth_token_type
    CHECK (token_type IN ('ACCESS', 'REFRESH', 'ID_TOKEN', 'AUTHORIZATION_CODE', 'CONSENT_STATE'));
