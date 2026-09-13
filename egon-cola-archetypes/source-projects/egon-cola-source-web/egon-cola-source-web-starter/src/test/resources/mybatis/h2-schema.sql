CREATE TABLE users (
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    id BIGINT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL
);

CREATE TABLE roles (
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    id BIGINT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL
);

CREATE TABLE permissions (
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    id BIGINT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL
);

CREATE TABLE user_roles (
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL
);

CREATE TABLE role_permissions (
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    id BIGINT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL
);

CREATE TABLE school_classes (
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    id BIGINT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    grade_name VARCHAR(120) NOT NULL,
    grade_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,






    CONSTRAINT uk_school_classes_0_tenant_grade_id_active
        UNIQUE (tenant_id, grade_id, id)
);

CREATE TABLE grades (
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    id BIGINT PRIMARY KEY,
    code VARCHAR(160) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL
);

CREATE TABLE school_class_users (
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    id BIGINT PRIMARY KEY,
    grade_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    school_class_id BIGINT NOT NULL,






    CONSTRAINT fk_school_class_users_0_class
        FOREIGN KEY (tenant_id, grade_id, school_class_id)
        REFERENCES school_classes (tenant_id, grade_id, id)
);

INSERT INTO roles(id,code,name,status,tenant_id) VALUES (1001,'STUDENT','Student','ACTIVE',1);
INSERT INTO permissions(id,code,name,type,status,tenant_id) VALUES (2001,'CLASS_READ','Read school class','API','ACTIVE',1);
