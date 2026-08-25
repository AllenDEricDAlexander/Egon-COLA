-- Web Open master-data migration to the Common MyBatis-Plus EgonModel contract.
-- Run manually on the master_data primary after 001__create_organization_master_data_schema.sql.
-- This script is intentionally not loaded by Spring/Flyway. Stop on a partially migrated schema.

SELECT 1 / CASE WHEN
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_name = 'users' AND column_name = 'created_at')
    AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                    WHERE table_name = 'users' AND column_name = 'tenant_id')
    THEN 1 ELSE 0 END;

ALTER TABLE users RENAME COLUMN created_at TO create_time;
ALTER TABLE roles RENAME COLUMN created_at TO create_time;
ALTER TABLE permissions RENAME COLUMN created_at TO create_time;
ALTER TABLE user_roles RENAME COLUMN created_at TO create_time;
ALTER TABLE role_permissions RENAME COLUMN created_at TO create_time;
ALTER TABLE grades RENAME COLUMN created_at TO create_time;

ALTER TABLE users DROP CONSTRAINT IF EXISTS uk_users_email;
ALTER TABLE roles DROP CONSTRAINT IF EXISTS uk_roles_code;
ALTER TABLE permissions DROP CONSTRAINT IF EXISTS uk_permissions_code;
ALTER TABLE user_roles DROP CONSTRAINT IF EXISTS uk_user_roles_user_role;
ALTER TABLE role_permissions DROP CONSTRAINT IF EXISTS uk_role_permissions_role_permission;
ALTER TABLE grades DROP CONSTRAINT IF EXISTS uk_grades_code;

ALTER TABLE users ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE roles ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE permissions ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE user_roles ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE role_permissions ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE grades ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;

ALTER TABLE users ADD COLUMN create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration';
ALTER TABLE roles ADD COLUMN create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration';
ALTER TABLE permissions ADD COLUMN create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration';
ALTER TABLE user_roles ADD COLUMN create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration';
ALTER TABLE role_permissions ADD COLUMN create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration';
ALTER TABLE grades ADD COLUMN create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration';
ALTER TABLE users ADD COLUMN update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration';
ALTER TABLE roles ADD COLUMN update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration';
ALTER TABLE permissions ADD COLUMN update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration';
ALTER TABLE user_roles ADD COLUMN update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration';
ALTER TABLE role_permissions ADD COLUMN update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration';
ALTER TABLE grades ADD COLUMN update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration';

ALTER TABLE users ADD COLUMN update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE roles ADD COLUMN update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE permissions ADD COLUMN update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE user_roles ADD COLUMN update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE role_permissions ADD COLUMN update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE grades ADD COLUMN update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ADD COLUMN is_deleted SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE roles ADD COLUMN is_deleted SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE permissions ADD COLUMN is_deleted SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE user_roles ADD COLUMN is_deleted SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE role_permissions ADD COLUMN is_deleted SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE grades ADD COLUMN is_deleted SMALLINT NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX uk_users_tenant_email_active
    ON users (tenant_id, email, is_deleted);
CREATE UNIQUE INDEX uk_roles_tenant_code_active
    ON roles (tenant_id, code, is_deleted);
CREATE UNIQUE INDEX uk_permissions_tenant_code_active
    ON permissions (tenant_id, code, is_deleted);
CREATE UNIQUE INDEX uk_user_roles_tenant_pair_active
    ON user_roles (tenant_id, user_id, role_id, is_deleted);
CREATE UNIQUE INDEX uk_role_permissions_tenant_pair_active
    ON role_permissions (tenant_id, role_id, permission_id, is_deleted);
CREATE UNIQUE INDEX uk_grades_tenant_code_active
    ON grades (tenant_id, code, is_deleted);
CREATE INDEX idx_user_roles_tenant_role
    ON user_roles (tenant_id, role_id, is_deleted);
CREATE INDEX idx_role_permissions_tenant_permission
    ON role_permissions (tenant_id, permission_id, is_deleted);
