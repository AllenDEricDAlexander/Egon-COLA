-- 变更内容：初始化 Light Open 主数据表与 BIGINT 技术主键。
-- 影响范围：master_data primary 内的 light_users、light_roles、light_permissions、light_user_roles、light_role_permissions、light_courses。
-- 兼容性说明：这是新 open schema 的首次建库脚本，不导入旧模板数据，不执行在线转换。

CREATE TABLE light_users (
    id BIGINT PRIMARY KEY,
    external_id VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_light_users_external_id UNIQUE (external_id),
    CONSTRAINT uk_light_users_email UNIQUE (email)
);

CREATE TABLE light_roles (
    code VARCHAR(64) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE light_permissions (
    code VARCHAR(128) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE light_user_roles (
    user_id BIGINT NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    assigned_at TIMESTAMP NOT NULL,
    PRIMARY KEY (user_id, role_code),
    CONSTRAINT fk_light_user_roles_user FOREIGN KEY (user_id) REFERENCES light_users (id),
    CONSTRAINT fk_light_user_roles_role FOREIGN KEY (role_code) REFERENCES light_roles (code)
);

CREATE TABLE light_role_permissions (
    role_code VARCHAR(64) NOT NULL,
    permission_code VARCHAR(128) NOT NULL,
    granted_at TIMESTAMP NOT NULL,
    PRIMARY KEY (role_code, permission_code),
    CONSTRAINT fk_light_role_permissions_role FOREIGN KEY (role_code) REFERENCES light_roles (code),
    CONSTRAINT fk_light_role_permissions_permission
        FOREIGN KEY (permission_code) REFERENCES light_permissions (code)
);

CREATE TABLE light_courses (
    id BIGINT PRIMARY KEY,
    course_code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_light_courses_code UNIQUE (course_code)
);
