CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    external_id VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL,
    status VARCHAR(32) NOT NULL,
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT uk_users_external_id UNIQUE (external_id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE UNIQUE INDEX pk_users_id ON users (id);

CREATE TABLE roles (
    code VARCHAR(64) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
);

CREATE UNIQUE INDEX pk_roles_id ON roles (id);

CREATE TABLE permissions (
    code VARCHAR(128) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
);

CREATE UNIQUE INDEX pk_permissions_id ON permissions (id);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    PRIMARY KEY (user_id, role_code),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_code) REFERENCES roles (code)
);

CREATE UNIQUE INDEX pk_user_roles_id ON user_roles (id);

CREATE TABLE role_permissions (
    role_code VARCHAR(64) NOT NULL,
    permission_code VARCHAR(128) NOT NULL,
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    PRIMARY KEY (role_code, permission_code),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_code) REFERENCES roles (code),
    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_code) REFERENCES permissions (code)
);

CREATE UNIQUE INDEX pk_role_permissions_id ON role_permissions (id);

CREATE TABLE courses (
    id BIGINT PRIMARY KEY,
    course_code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT uk_courses_code UNIQUE (course_code)
);

CREATE UNIQUE INDEX pk_courses_id ON courses (id);

CREATE TABLE school_classes (
    id BIGINT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    semester VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
);

CREATE INDEX idx_school_classes_tenant_active_0 ON school_classes (tenant_id, id);

CREATE TABLE class_course_schedules (
    id BIGINT PRIMARY KEY,
    school_class_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT uk_class_course_start_0
        UNIQUE (school_class_id, course_id, starts_at),
    CONSTRAINT fk_schedule_class_0
        FOREIGN KEY (school_class_id) REFERENCES school_classes (id)
);

INSERT INTO roles(code,name,status,id,tenant_id) VALUES ('STUDENT','Student','ACTIVE',1001,1);

INSERT INTO permissions(code,name,status,id,tenant_id) VALUES ('CLASS_READ','Read class','ACTIVE',2001,1);
