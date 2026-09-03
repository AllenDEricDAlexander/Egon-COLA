-- 变更内容：直接建立 Organization 主数据表在 V20260825_003 后的累计最终结构并写入确定性权限种子。
-- 影响范围：master_data 库中的 users、roles、permissions、user_roles、role_permissions、grades 表及其租户索引和种子数据。
-- 兼容性说明：仅用于空的新环境；已有 V20260726/V20260825 迁移历史会忽略本 baseline 并继续由 Flyway validate。

CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE TABLE roles (
    id BIGINT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE TABLE permissions (
    id BIGINT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE TABLE user_roles (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time TIMESTAMP NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE TABLE role_permissions (
    id BIGINT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time TIMESTAMP NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE TABLE grades (
    id BIGINT PRIMARY KEY,
    code VARCHAR(160) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions (permission_id);
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

INSERT INTO roles(id, code, name, status, create_time, tenant_id,
                  create_user_id, update_user_id, update_time, is_deleted)
VALUES (1001, 'STUDENT', 'Student', 'ACTIVE', CURRENT_TIMESTAMP, 1,
        'migration', 'migration', CURRENT_TIMESTAMP, 0);

INSERT INTO permissions(id, code, name, type, status, create_time, tenant_id,
                        create_user_id, update_user_id, update_time, is_deleted)
VALUES (2001, 'CLASS_READ', 'Read school class', 'API', 'ACTIVE', CURRENT_TIMESTAMP, 1,
        'migration', 'migration', CURRENT_TIMESTAMP, 0);
