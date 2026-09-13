-- Complete PostgreSQL initialization for web-open; requires an empty managed schema.
DO $egon$
BEGIN
    IF current_setting('egon_migration.role') = 'MASTER_DATA' THEN
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

        CREATE UNIQUE INDEX uk_users_tenant_email_active ON users (tenant_id, email) WHERE deleted_at IS NULL;

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

        CREATE UNIQUE INDEX uk_roles_tenant_code_active ON roles (tenant_id, code) WHERE deleted_at IS NULL;

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

        CREATE UNIQUE INDEX uk_permissions_tenant_code_active ON permissions (tenant_id, code) WHERE deleted_at IS NULL;

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

        CREATE UNIQUE INDEX uk_user_roles_tenant_pair_active ON user_roles (tenant_id, user_id, role_id) WHERE deleted_at IS NULL;

        CREATE INDEX idx_user_roles_tenant_role ON user_roles (tenant_id, role_id) WHERE deleted_at IS NULL;

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

        CREATE UNIQUE INDEX uk_role_permissions_tenant_pair_active ON role_permissions (tenant_id, role_id, permission_id) WHERE deleted_at IS NULL;

        CREATE INDEX idx_role_permissions_tenant_permission ON role_permissions (tenant_id, permission_id) WHERE deleted_at IS NULL;

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

        CREATE UNIQUE INDEX uk_grades_tenant_code_active ON grades (tenant_id, code) WHERE deleted_at IS NULL;

INSERT INTO roles(id,code,name,status,tenant_id) VALUES (1001,'STUDENT','Student','ACTIVE',1);
INSERT INTO permissions(id,code,name,type,status,tenant_id) VALUES (2001,'CLASS_READ','Read school class','API','ACTIVE',1);
    ELSIF current_setting('egon_migration.role') = 'SHARD' THEN
        CREATE TABLE school_classes_0 (
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

        CREATE INDEX idx_school_classes_0_tenant_grade ON school_classes_0 (tenant_id, grade_id) WHERE deleted_at IS NULL;

        CREATE TABLE school_classes_1 (
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

            CONSTRAINT uk_school_classes_1_grade_name UNIQUE (grade_id, name),
            CONSTRAINT uk_school_classes_1_grade_id UNIQUE (grade_id, id)
        );

        CREATE INDEX idx_school_classes_1_tenant_grade ON school_classes_1 (tenant_id, grade_id) WHERE deleted_at IS NULL;

        CREATE TABLE school_class_users_0 (
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

        CREATE INDEX idx_school_class_users_0_tenant_grade_class ON school_class_users_0 (tenant_id, grade_id, school_class_id);

        CREATE TABLE school_class_users_1 (
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

            CONSTRAINT uk_school_class_users_1 UNIQUE (grade_id, school_class_id, user_id)
        );

        CREATE INDEX idx_school_class_users_1_tenant_grade_class ON school_class_users_1 (tenant_id, grade_id, school_class_id);
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
