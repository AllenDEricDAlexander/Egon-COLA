CREATE TABLE users (
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL,
    status VARCHAR(32) NOT NULL,

    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE roles (
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    id BIGINT NOT NULL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,

    CONSTRAINT uk_roles_code UNIQUE (code)
);

CREATE TABLE permissions (
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    id BIGINT NOT NULL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,

    CONSTRAINT uk_permissions_code UNIQUE (code)
);

CREATE TABLE user_roles (
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,

    CONSTRAINT uk_user_roles_user_role UNIQUE (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

CREATE TABLE role_permissions (
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    id BIGINT NOT NULL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,

    CONSTRAINT uk_role_permissions_role_permission UNIQUE (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
);

CREATE TABLE school_classes (
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    grade_name VARCHAR(120) NOT NULL,
    grade_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,

    CONSTRAINT uk_school_classes_0_grade_name UNIQUE (grade_id, name),
    CONSTRAINT uk_school_classes_0_grade_id UNIQUE (grade_id, id)
);

CREATE TABLE grades (
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    id BIGINT NOT NULL PRIMARY KEY,
    code VARCHAR(160) NOT NULL,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,

    CONSTRAINT uk_grades_code UNIQUE (code)
);

CREATE TABLE school_class_users (
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    id BIGINT NOT NULL PRIMARY KEY,
    grade_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    school_class_id BIGINT NOT NULL,

    CONSTRAINT uk_school_class_users_0 UNIQUE (grade_id, school_class_id, user_id)
);

INSERT INTO roles(id,code,name,status,tenant_id) VALUES (1001,'STUDENT','Student','ACTIVE',1);
INSERT INTO permissions(id,code,name,type,status,tenant_id) VALUES (2001,'CLASS_READ','Read school class','API','ACTIVE',1);
