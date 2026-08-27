-- Manual operator migration: 将 Organization 主数据迁移到 Long 主键、tenant_id 与 EgonModel 技术字段。
-- Scope: master_data 库中的 users、roles、permissions、user_roles、role_permissions、grades。
-- Compatibility: 旧表中的历史租户和审计身份必须先完成明确离线映射；未知历史数据不得由脚本猜测。

-- 空脚手架或已完成离线映射的表才允许继续；此处阻断未知历史身份。
SELECT 1 / CASE WHEN EXISTS (SELECT 1 FROM users)
    OR EXISTS (SELECT 1 FROM roles)
    OR EXISTS (SELECT 1 FROM permissions)
    OR EXISTS (SELECT 1 FROM user_roles)
    OR EXISTS (SELECT 1 FROM role_permissions)
    OR EXISTS (SELECT 1 FROM grades)
    THEN 0 ELSE 1 END;

ALTER TABLE users RENAME COLUMN created_at TO create_time;
ALTER TABLE roles RENAME COLUMN created_at TO create_time;
ALTER TABLE permissions RENAME COLUMN created_at TO create_time;
ALTER TABLE user_roles RENAME COLUMN created_at TO create_time;
ALTER TABLE role_permissions RENAME COLUMN created_at TO create_time;
ALTER TABLE grades RENAME COLUMN created_at TO create_time;

ALTER TABLE users ADD COLUMN tenant_id BIGINT DEFAULT 1;
ALTER TABLE roles ADD COLUMN tenant_id BIGINT DEFAULT 1;
ALTER TABLE permissions ADD COLUMN tenant_id BIGINT DEFAULT 1;
ALTER TABLE user_roles ADD COLUMN tenant_id BIGINT DEFAULT 1;
ALTER TABLE role_permissions ADD COLUMN tenant_id BIGINT DEFAULT 1;
ALTER TABLE grades ADD COLUMN tenant_id BIGINT DEFAULT 1;

ALTER TABLE users ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE roles ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE permissions ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE user_roles ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE role_permissions ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE grades ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE users ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE roles ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE permissions ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE user_roles ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE role_permissions ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE grades ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE users ADD COLUMN update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE roles ADD COLUMN update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE permissions ADD COLUMN update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE user_roles ADD COLUMN update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE role_permissions ADD COLUMN update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE grades ADD COLUMN update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE roles ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE permissions ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE user_roles ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE role_permissions ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE grades ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;

ALTER TABLE users ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE roles ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE permissions ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE user_roles ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE role_permissions ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE grades ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE users ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE roles ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE permissions ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE user_roles ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE role_permissions ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE grades ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE users ALTER COLUMN update_user_id SET NOT NULL;
ALTER TABLE roles ALTER COLUMN update_user_id SET NOT NULL;
ALTER TABLE permissions ALTER COLUMN update_user_id SET NOT NULL;
ALTER TABLE user_roles ALTER COLUMN update_user_id SET NOT NULL;
ALTER TABLE role_permissions ALTER COLUMN update_user_id SET NOT NULL;
ALTER TABLE grades ALTER COLUMN update_user_id SET NOT NULL;

DROP INDEX IF EXISTS uk_users_email;
DROP INDEX IF EXISTS uk_roles_code;
DROP INDEX IF EXISTS uk_permissions_code;
DROP INDEX IF EXISTS uk_user_roles_user_role;
DROP INDEX IF EXISTS uk_role_permissions_role_permission;
DROP INDEX IF EXISTS uk_grades_code;
CREATE UNIQUE INDEX uk_users_tenant_email_active ON users (tenant_id, email, is_deleted);
CREATE UNIQUE INDEX uk_roles_tenant_code_active ON roles (tenant_id, code, is_deleted);
CREATE UNIQUE INDEX uk_permissions_tenant_code_active ON permissions (tenant_id, code, is_deleted);
CREATE UNIQUE INDEX uk_user_roles_tenant_pair_active ON user_roles (tenant_id, user_id, role_id, is_deleted);
CREATE UNIQUE INDEX uk_role_permissions_tenant_pair_active
    ON role_permissions (tenant_id, role_id, permission_id, is_deleted);
CREATE UNIQUE INDEX uk_grades_tenant_code_active ON grades (tenant_id, code, is_deleted);
