-- 变更内容：初始化 Light 分片模式中不分库分表的用户、权限和课程表结构。
-- 影响范围：single 逻辑库内的 users、roles、permissions、user_roles、role_permissions、courses 表及其约束。
-- 兼容性说明：本文件仅用于未执行过迁移的新建脚手架项目，无历史数据兼容与回滚负担。

CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    external_id VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_users_external_id UNIQUE (external_id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE roles (
    code VARCHAR(64) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE permissions (
    code VARCHAR(128) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE user_roles (
    user_id VARCHAR(36) NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    assigned_at TIMESTAMP NOT NULL,
    PRIMARY KEY (user_id, role_code),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_code) REFERENCES roles (code)
);

CREATE TABLE role_permissions (
    role_code VARCHAR(64) NOT NULL,
    permission_code VARCHAR(128) NOT NULL,
    granted_at TIMESTAMP NOT NULL,
    PRIMARY KEY (role_code, permission_code),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_code) REFERENCES roles (code),
    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_code) REFERENCES permissions (code)
);

CREATE TABLE courses (
    id VARCHAR(36) PRIMARY KEY,
    course_code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_courses_code UNIQUE (course_code)
);
