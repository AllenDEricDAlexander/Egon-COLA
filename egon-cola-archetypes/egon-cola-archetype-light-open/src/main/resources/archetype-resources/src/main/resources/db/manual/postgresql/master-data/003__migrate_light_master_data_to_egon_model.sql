-- 变更内容：将 Light 主数据表迁移到 Long 主键和 EgonModel 七项技术字段。
-- 影响范围：light_users、light_roles、light_permissions、light_user_roles、light_role_permissions、light_courses 及其约束。
-- 兼容性说明：旧表中的非空或不可转换标识会使类型变更失败；请先完成外部数据回填和校验。

-- 迁移前置条件：该脚手架只接受空的 V20260726 主数据表；任何历史行必须先完成离线身份/租户映射。
SELECT 1 / CASE WHEN EXISTS (SELECT 1 FROM light_users)
    OR EXISTS (SELECT 1 FROM light_roles)
    OR EXISTS (SELECT 1 FROM light_permissions)
    OR EXISTS (SELECT 1 FROM light_user_roles)
    OR EXISTS (SELECT 1 FROM light_role_permissions)
    OR EXISTS (SELECT 1 FROM light_courses)
    THEN 0 ELSE 1 END;

ALTER TABLE light_users ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE light_courses ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE light_user_roles ALTER COLUMN user_id SET DATA TYPE BIGINT;

ALTER TABLE light_roles ADD COLUMN id BIGINT;
ALTER TABLE light_permissions ADD COLUMN id BIGINT;
ALTER TABLE light_user_roles ADD COLUMN id BIGINT;
ALTER TABLE light_role_permissions ADD COLUMN id BIGINT;

ALTER TABLE light_users ADD COLUMN tenant_id BIGINT;
ALTER TABLE light_users ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE light_users ADD COLUMN create_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE light_users ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE light_users ADD COLUMN update_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE light_users ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE light_roles ADD COLUMN tenant_id BIGINT;
ALTER TABLE light_roles ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE light_roles ADD COLUMN create_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE light_roles ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE light_roles ADD COLUMN update_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE light_roles ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE light_permissions ADD COLUMN tenant_id BIGINT;
ALTER TABLE light_permissions ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE light_permissions ADD COLUMN create_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE light_permissions ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE light_permissions ADD COLUMN update_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE light_permissions ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE light_user_roles ADD COLUMN tenant_id BIGINT;
ALTER TABLE light_user_roles ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE light_user_roles ADD COLUMN create_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE light_user_roles ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE light_user_roles ADD COLUMN update_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE light_user_roles ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE light_role_permissions ADD COLUMN tenant_id BIGINT;
ALTER TABLE light_role_permissions ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE light_role_permissions ADD COLUMN create_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE light_role_permissions ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE light_role_permissions ADD COLUMN update_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE light_role_permissions ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE light_courses ADD COLUMN tenant_id BIGINT;
ALTER TABLE light_courses ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE light_courses ADD COLUMN create_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE light_courses ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE light_courses ADD COLUMN update_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE light_courses ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;

ALTER TABLE light_users DROP COLUMN created_at;
ALTER TABLE light_courses DROP COLUMN created_at;
ALTER TABLE light_roles DROP COLUMN created_at;
ALTER TABLE light_permissions DROP COLUMN created_at;
ALTER TABLE light_user_roles DROP COLUMN assigned_at;
ALTER TABLE light_role_permissions DROP COLUMN granted_at;

ALTER TABLE light_users ALTER COLUMN id SET NOT NULL;
ALTER TABLE light_courses ALTER COLUMN id SET NOT NULL;
ALTER TABLE light_roles ALTER COLUMN id SET NOT NULL;
ALTER TABLE light_permissions ALTER COLUMN id SET NOT NULL;
ALTER TABLE light_user_roles ALTER COLUMN id SET NOT NULL;
ALTER TABLE light_role_permissions ALTER COLUMN id SET NOT NULL;
ALTER TABLE light_users ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE light_roles ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE light_permissions ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE light_user_roles ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE light_role_permissions ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE light_courses ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE light_users ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE light_users ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE light_users ALTER COLUMN update_user_id SET NOT NULL;
ALTER TABLE light_users ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE light_users ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE light_roles ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE light_roles ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE light_roles ALTER COLUMN update_user_id SET NOT NULL;
ALTER TABLE light_roles ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE light_roles ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE light_permissions ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE light_permissions ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE light_permissions ALTER COLUMN update_user_id SET NOT NULL;
ALTER TABLE light_permissions ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE light_permissions ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE light_user_roles ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE light_user_roles ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE light_user_roles ALTER COLUMN update_user_id SET NOT NULL;
ALTER TABLE light_user_roles ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE light_user_roles ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE light_role_permissions ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE light_role_permissions ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE light_role_permissions ALTER COLUMN update_user_id SET NOT NULL;
ALTER TABLE light_role_permissions ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE light_role_permissions ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE light_courses ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE light_courses ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE light_courses ALTER COLUMN update_user_id SET NOT NULL;
ALTER TABLE light_courses ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE light_courses ALTER COLUMN is_deleted SET NOT NULL;

CREATE UNIQUE INDEX pk_light_users_id ON light_users (id);
CREATE UNIQUE INDEX pk_light_courses_id ON light_courses (id);
CREATE UNIQUE INDEX pk_light_roles_id ON light_roles (id);
CREATE UNIQUE INDEX pk_light_permissions_id ON light_permissions (id);
CREATE UNIQUE INDEX pk_light_user_roles_id ON light_user_roles (id);
CREATE UNIQUE INDEX pk_light_role_permissions_id ON light_role_permissions (id);
CREATE UNIQUE INDEX uk_light_users_tenant_external_active
    ON light_users (tenant_id, external_id, is_deleted);
CREATE UNIQUE INDEX uk_light_users_tenant_email_active
    ON light_users (tenant_id, email, is_deleted);
CREATE UNIQUE INDEX uk_light_roles_tenant_code_active
    ON light_roles (tenant_id, code, is_deleted);
CREATE UNIQUE INDEX uk_light_permissions_tenant_code_active
    ON light_permissions (tenant_id, code, is_deleted);
CREATE UNIQUE INDEX uk_light_courses_tenant_code_active
    ON light_courses (tenant_id, course_code, is_deleted);
CREATE UNIQUE INDEX uk_light_user_roles_tenant_member_active
    ON light_user_roles (tenant_id, user_id, role_code, is_deleted);
CREATE UNIQUE INDEX uk_light_role_permissions_tenant_grant_active
    ON light_role_permissions (tenant_id, role_code, permission_code, is_deleted);
