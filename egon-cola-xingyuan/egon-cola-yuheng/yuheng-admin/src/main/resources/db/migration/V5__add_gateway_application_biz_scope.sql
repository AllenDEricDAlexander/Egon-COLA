ALTER TABLE gateway_application
    ADD COLUMN biz_code VARCHAR(128) NOT NULL DEFAULT 'default';

ALTER TABLE gateway_application
    ALTER COLUMN biz_code DROP DEFAULT;
