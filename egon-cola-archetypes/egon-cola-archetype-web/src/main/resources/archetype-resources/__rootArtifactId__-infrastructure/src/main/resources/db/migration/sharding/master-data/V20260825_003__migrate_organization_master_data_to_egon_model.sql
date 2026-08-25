-- 变更内容：将 Organization 主数据表迁移到 Long 主键、tenant_id 与 EgonModel 技术字段。
-- 影响范围：master_data 库中的 users、roles、permissions、user_roles、role_permissions、grades。
-- 兼容性说明：只自动转换旧迁移写入的两个已知种子 UUID；其他历史行必须先完成离线 Long/tenant 映射。

-- 未知历史身份不得在 Flyway 内猜测；空脚手架和内置种子是唯一可自动迁移的状态。
SELECT 1 / CASE WHEN
    (NOT EXISTS (SELECT 1 FROM users)
     AND NOT EXISTS (SELECT 1 FROM user_roles)
     AND NOT EXISTS (SELECT 1 FROM role_permissions)
     AND NOT EXISTS (SELECT 1 FROM grades)
     AND NOT EXISTS (SELECT 1 FROM roles WHERE id <> '019ba346-0000-7000-8000-000000000001')
     AND NOT EXISTS (SELECT 1 FROM permissions WHERE id <> '019ba346-0000-7000-8000-000000000002'))
    THEN 1 ELSE 0 END;

UPDATE roles SET id = '1001' WHERE id = '019ba346-0000-7000-8000-000000000001';
UPDATE permissions SET id = '2001' WHERE id = '019ba346-0000-7000-8000-000000000002';

ALTER TABLE user_roles DROP CONSTRAINT IF EXISTS fk_user_roles_user;
ALTER TABLE user_roles DROP CONSTRAINT IF EXISTS fk_user_roles_role;
ALTER TABLE role_permissions DROP CONSTRAINT IF EXISTS fk_role_permissions_role;
ALTER TABLE role_permissions DROP CONSTRAINT IF EXISTS fk_role_permissions_permission;
ALTER TABLE roles DROP CONSTRAINT IF EXISTS uk_roles_code;
ALTER TABLE permissions DROP CONSTRAINT IF EXISTS uk_permissions_code;
ALTER TABLE user_roles DROP CONSTRAINT IF EXISTS uk_user_role;
ALTER TABLE role_permissions DROP CONSTRAINT IF EXISTS uk_role_permission;

ALTER TABLE users ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE roles ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE permissions ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE user_roles ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE user_roles ALTER COLUMN user_id SET DATA TYPE BIGINT;
ALTER TABLE user_roles ALTER COLUMN role_id SET DATA TYPE BIGINT;
ALTER TABLE role_permissions ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE role_permissions ALTER COLUMN role_id SET DATA TYPE BIGINT;
ALTER TABLE role_permissions ALTER COLUMN permission_id SET DATA TYPE BIGINT;
ALTER TABLE grades ALTER COLUMN id SET DATA TYPE BIGINT;

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
ALTER TABLE users ADD COLUMN is_deleted SMALLINT DEFAULT 0;
ALTER TABLE roles ADD COLUMN is_deleted SMALLINT DEFAULT 0;
ALTER TABLE permissions ADD COLUMN is_deleted SMALLINT DEFAULT 0;
ALTER TABLE user_roles ADD COLUMN is_deleted SMALLINT DEFAULT 0;
ALTER TABLE role_permissions ADD COLUMN is_deleted SMALLINT DEFAULT 0;
ALTER TABLE grades ADD COLUMN is_deleted SMALLINT DEFAULT 0;

ALTER TABLE users ALTER COLUMN id SET NOT NULL;
ALTER TABLE roles ALTER COLUMN id SET NOT NULL;
ALTER TABLE permissions ALTER COLUMN id SET NOT NULL;
ALTER TABLE user_roles ALTER COLUMN id SET NOT NULL;
ALTER TABLE role_permissions ALTER COLUMN id SET NOT NULL;
ALTER TABLE grades ALTER COLUMN id SET NOT NULL;
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
ALTER TABLE users ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE roles ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE permissions ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE user_roles ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE role_permissions ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE grades ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE users ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE roles ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE permissions ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE user_roles ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE role_permissions ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE grades ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE users ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE roles ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE permissions ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE user_roles ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE role_permissions ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE grades ALTER COLUMN is_deleted SET NOT NULL;

CREATE UNIQUE INDEX uk_users_tenant_email_active ON users (tenant_id, email, is_deleted);
CREATE UNIQUE INDEX uk_roles_tenant_code_active ON roles (tenant_id, code, is_deleted);
CREATE UNIQUE INDEX uk_permissions_tenant_code_active ON permissions (tenant_id, code, is_deleted);
CREATE UNIQUE INDEX uk_user_roles_tenant_pair_active ON user_roles (tenant_id, user_id, role_id, is_deleted);
CREATE UNIQUE INDEX uk_role_permissions_tenant_pair_active
    ON role_permissions (tenant_id, role_id, permission_id, is_deleted);
CREATE UNIQUE INDEX uk_grades_tenant_code_active ON grades (tenant_id, code, is_deleted);
CREATE INDEX idx_user_roles_tenant_role ON user_roles (tenant_id, role_id, is_deleted);
CREATE INDEX idx_role_permissions_tenant_permission
    ON role_permissions (tenant_id, permission_id, is_deleted);
