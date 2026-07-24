-- 变更内容：初始化 Organization 默认单数据源模式的完整业务表和基础权限数据。
-- 影响范围：用户、角色、权限、年级、班级及班级成员关系；仅用于全新脚手架工程初始化。
-- 兼容性说明：仅初始化未执行过迁移的新建脚手架工程；全部代理主键由应用侧生成 UUIDv7，不兼容自增主键。

CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE roles (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE permissions (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE user_roles (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    role_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE role_permissions (
    id VARCHAR(36) PRIMARY KEY,
    role_id VARCHAR(36) NOT NULL,
    permission_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id)
);

CREATE TABLE grades (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(160) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE school_classes (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    grade_name VARCHAR(120) NOT NULL,
    grade_id VARCHAR(36) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_school_class_grade_name UNIQUE (grade_id, name),
    CONSTRAINT uk_school_class_grade_id UNIQUE (grade_id, id),
    CONSTRAINT fk_school_classes_grade FOREIGN KEY (grade_id) REFERENCES grades(id)
);

CREATE TABLE school_class_users (
    id VARCHAR(36) PRIMARY KEY,
    grade_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    school_class_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_school_class_user UNIQUE (grade_id, school_class_id, user_id),
    CONSTRAINT fk_school_class_users_grade FOREIGN KEY (grade_id) REFERENCES grades(id),
    CONSTRAINT fk_school_class_users_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_school_class_users_class
        FOREIGN KEY (grade_id, school_class_id)
        REFERENCES school_classes(grade_id, id)
);

CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);
CREATE INDEX idx_school_classes_grade_id ON school_classes(grade_id);
CREATE INDEX idx_school_class_users_grade_class
    ON school_class_users(grade_id, school_class_id);

INSERT INTO roles(id, code, name, status, created_at)
VALUES ('019ba346-0000-7000-8000-000000000001', 'STUDENT', 'Student', 'ACTIVE', CURRENT_TIMESTAMP);

INSERT INTO permissions(id, code, name, type, status, created_at)
VALUES (
    '019ba346-0000-7000-8000-000000000002',
    'CLASS_READ',
    'Read school class',
    'API',
    'ACTIVE',
    CURRENT_TIMESTAMP
);
