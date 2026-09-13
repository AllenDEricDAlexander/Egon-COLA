-- Complete PostgreSQL initialization for the light archetype; only an empty managed target is eligible.
-- Tables retain existing business keys and foreign keys. IDs are provided by the application.
DO $egon$
BEGIN
    IF current_setting('egon_migration.role') = 'MASTER_DATA' THEN
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

        CREATE UNIQUE INDEX uk_users_tenant_external_active ON users (tenant_id, external_id) WHERE deleted_at IS NULL;

        CREATE UNIQUE INDEX uk_users_tenant_email_active ON users (tenant_id, email) WHERE deleted_at IS NULL;

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

        CREATE UNIQUE INDEX uk_roles_tenant_code_active ON roles (tenant_id, code) WHERE deleted_at IS NULL;

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

        CREATE UNIQUE INDEX uk_permissions_tenant_code_active ON permissions (tenant_id, code) WHERE deleted_at IS NULL;

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

        CREATE UNIQUE INDEX uk_user_roles_tenant_member_active ON user_roles (tenant_id, user_id, role_code) WHERE deleted_at IS NULL;

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

        CREATE UNIQUE INDEX uk_role_permissions_tenant_grant_active ON role_permissions (tenant_id, role_code, permission_code) WHERE deleted_at IS NULL;

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

        CREATE UNIQUE INDEX uk_courses_tenant_code_active ON courses (tenant_id, course_code) WHERE deleted_at IS NULL;

        INSERT INTO roles (code,name,status,id,tenant_id) VALUES ('STUDENT','Student','ACTIVE',1001,1);

        INSERT INTO permissions (code,name,status,id,tenant_id) VALUES ('CLASS_READ','Read class','ACTIVE',2001,1);
    ELSIF current_setting('egon_migration.role') = 'SHARD' THEN
        CREATE TABLE school_classes_0 (
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

        CREATE INDEX idx_school_classes_tenant_active_0 ON school_classes_0 (tenant_id, id);

        CREATE TABLE school_classes_1 (
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

        CREATE INDEX idx_school_classes_tenant_active_1 ON school_classes_1 (tenant_id, id);

        CREATE TABLE class_course_schedules_0 (
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
                FOREIGN KEY (school_class_id) REFERENCES school_classes_0 (id)
        );

        CREATE UNIQUE INDEX uk_class_course_start_tenant_active_0 ON class_course_schedules_0 (tenant_id, school_class_id, course_id, starts_at) WHERE deleted_at IS NULL;

        CREATE TABLE class_course_schedules_1 (
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
            CONSTRAINT uk_class_course_start_1
                UNIQUE (school_class_id, course_id, starts_at),
            CONSTRAINT fk_schedule_class_1
                FOREIGN KEY (school_class_id) REFERENCES school_classes_1 (id)
        );

        CREATE UNIQUE INDEX uk_class_course_start_tenant_active_1 ON class_course_schedules_1 (tenant_id, school_class_id, course_id, starts_at) WHERE deleted_at IS NULL;
    ELSE
        RAISE EXCEPTION 'Unknown managed DDL role';
    END IF;
END
$egon$;

CREATE TABLE ddl_history (
    tenant_id bigint NOT NULL DEFAULT 0 CHECK (tenant_id = 0),
    script varchar(500) NOT NULL,
    type varchar(30) NOT NULL DEFAULT 'SQL' CHECK (type = 'SQL'),
    version varchar(30) NOT NULL,
    checksum char(64) NOT NULL CHECK (checksum ~ '^[0-9a-f]{64}$'),
    installed_on timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_ms bigint NOT NULL CHECK (execution_ms >= 0),
    route_fingerprint char(64) NOT NULL CHECK (route_fingerprint ~ '^[0-9a-f]{64}$'),
    CONSTRAINT pk_ddl_history PRIMARY KEY (script, type),
    CONSTRAINT uk_ddl_history_type_version UNIQUE (type, version)
);
