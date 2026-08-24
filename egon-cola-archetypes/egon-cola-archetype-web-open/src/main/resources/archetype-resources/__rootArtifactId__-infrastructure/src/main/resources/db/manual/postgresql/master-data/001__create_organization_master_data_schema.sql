-- Web Open master schema for PostgreSQL. Execute manually on the master_data primary.
-- The application never reads or executes this file.

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS roles (
    id BIGINT NOT NULL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_roles_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT NOT NULL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_permissions_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS user_roles (
    id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_user_roles_user_role UNIQUE (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGINT NOT NULL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_role_permissions_role_permission UNIQUE (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
);

CREATE TABLE IF NOT EXISTS grades (
    id BIGINT NOT NULL PRIMARY KEY,
    code VARCHAR(160) NOT NULL,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_grades_code UNIQUE (code)
);

-- Reserved example identifiers are intentionally stable and may be replaced by the operator.
INSERT INTO roles (id, code, name, status, created_at)
VALUES (1, 'STUDENT', 'Student', 'ACTIVE', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE
SET code = EXCLUDED.code,
    name = EXCLUDED.name,
    status = EXCLUDED.status,
    created_at = EXCLUDED.created_at;

INSERT INTO permissions (id, code, name, type, status, created_at)
VALUES (2, 'CLASS_READ', 'Read school class', 'API', 'ACTIVE', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE
SET code = EXCLUDED.code,
    name = EXCLUDED.name,
    type = EXCLUDED.type,
    status = EXCLUDED.status,
    created_at = EXCLUDED.created_at;
