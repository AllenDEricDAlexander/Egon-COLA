-- 变更内容：将 Light 主数据表迁移到 Long 主键和 EgonModel 七项技术字段。
-- 影响范围：users、roles、permissions、user_roles、role_permissions、courses 及其约束。
-- 兼容性说明：旧表中的非空或不可转换标识会使类型变更失败；请先完成外部数据回填和校验。

-- 迁移前置条件：该脚手架只接受空的 V20260726 主数据表；任何历史行必须先完成离线身份/租户映射。
SELECT 1 / CASE WHEN EXISTS (SELECT 1 FROM users)
    OR EXISTS (SELECT 1 FROM roles)
    OR EXISTS (SELECT 1 FROM permissions)
    OR EXISTS (SELECT 1 FROM user_roles)
    OR EXISTS (SELECT 1 FROM role_permissions)
    OR EXISTS (SELECT 1 FROM courses)
    THEN 0 ELSE 1 END;

ALTER TABLE users ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE courses ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE user_roles ALTER COLUMN user_id SET DATA TYPE BIGINT;

ALTER TABLE roles ADD COLUMN id BIGINT;
ALTER TABLE permissions ADD COLUMN id BIGINT;
ALTER TABLE user_roles ADD COLUMN id BIGINT;
ALTER TABLE role_permissions ADD COLUMN id BIGINT;

ALTER TABLE users ADD COLUMN tenant_id BIGINT;
ALTER TABLE users ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE users ADD COLUMN create_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE users ADD COLUMN update_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE roles ADD COLUMN tenant_id BIGINT;
ALTER TABLE roles ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE roles ADD COLUMN create_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE roles ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE roles ADD COLUMN update_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE roles ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE permissions ADD COLUMN tenant_id BIGINT;
ALTER TABLE permissions ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE permissions ADD COLUMN create_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE permissions ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE permissions ADD COLUMN update_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE permissions ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE user_roles ADD COLUMN tenant_id BIGINT;
ALTER TABLE user_roles ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE user_roles ADD COLUMN create_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE user_roles ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE user_roles ADD COLUMN update_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE user_roles ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE role_permissions ADD COLUMN tenant_id BIGINT;
ALTER TABLE role_permissions ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE role_permissions ADD COLUMN create_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE role_permissions ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE role_permissions ADD COLUMN update_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE role_permissions ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE courses ADD COLUMN tenant_id BIGINT;
ALTER TABLE courses ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE courses ADD COLUMN create_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE courses ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE courses ADD COLUMN update_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE courses ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;

ALTER TABLE users DROP COLUMN created_at;
ALTER TABLE courses DROP COLUMN created_at;
ALTER TABLE roles DROP COLUMN created_at;
ALTER TABLE permissions DROP COLUMN created_at;
ALTER TABLE user_roles DROP COLUMN assigned_at;
ALTER TABLE role_permissions DROP COLUMN granted_at;

ALTER TABLE users ALTER COLUMN id SET NOT NULL;
ALTER TABLE courses ALTER COLUMN id SET NOT NULL;
ALTER TABLE roles ALTER COLUMN id SET NOT NULL;
ALTER TABLE permissions ALTER COLUMN id SET NOT NULL;
ALTER TABLE user_roles ALTER COLUMN id SET NOT NULL;
ALTER TABLE role_permissions ALTER COLUMN id SET NOT NULL;
ALTER TABLE users ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE roles ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE permissions ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE user_roles ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE role_permissions ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE courses ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE users ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE users ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE users ALTER COLUMN update_user_id SET NOT NULL;
ALTER TABLE users ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE users ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE roles ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE roles ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE roles ALTER COLUMN update_user_id SET NOT NULL;
ALTER TABLE roles ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE roles ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE permissions ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE permissions ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE permissions ALTER COLUMN update_user_id SET NOT NULL;
ALTER TABLE permissions ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE permissions ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE user_roles ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE user_roles ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE user_roles ALTER COLUMN update_user_id SET NOT NULL;
ALTER TABLE user_roles ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE user_roles ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE role_permissions ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE role_permissions ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE role_permissions ALTER COLUMN update_user_id SET NOT NULL;
ALTER TABLE role_permissions ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE role_permissions ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE courses ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE courses ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE courses ALTER COLUMN update_user_id SET NOT NULL;
ALTER TABLE courses ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE courses ALTER COLUMN is_deleted SET NOT NULL;

CREATE UNIQUE INDEX pk_users_id ON users (id);
CREATE UNIQUE INDEX pk_courses_id ON courses (id);
CREATE UNIQUE INDEX pk_roles_id ON roles (id);
CREATE UNIQUE INDEX pk_permissions_id ON permissions (id);
CREATE UNIQUE INDEX pk_user_roles_id ON user_roles (id);
CREATE UNIQUE INDEX pk_role_permissions_id ON role_permissions (id);
CREATE UNIQUE INDEX uk_users_tenant_external_active
    ON users (tenant_id, external_id, is_deleted);
CREATE UNIQUE INDEX uk_users_tenant_email_active
    ON users (tenant_id, email, is_deleted);
CREATE UNIQUE INDEX uk_roles_tenant_code_active
    ON roles (tenant_id, code, is_deleted);
CREATE UNIQUE INDEX uk_permissions_tenant_code_active
    ON permissions (tenant_id, code, is_deleted);
CREATE UNIQUE INDEX uk_courses_tenant_code_active
    ON courses (tenant_id, course_code, is_deleted);
CREATE UNIQUE INDEX uk_user_roles_tenant_member_active
    ON user_roles (tenant_id, user_id, role_code, is_deleted);
CREATE UNIQUE INDEX uk_role_permissions_tenant_grant_active
    ON role_permissions (tenant_id, role_code, permission_code, is_deleted);
