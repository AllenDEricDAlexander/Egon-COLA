-- 变更内容：直接建立 Organization 分片表在 V20260825_004 后的累计最终结构。
-- 影响范围：每个 shard 库中的 school_classes_0/1、school_class_users_0/1 表及其租户唯一索引、查询索引和复合外键。
-- 兼容性说明：仅用于空的新环境；已有 V20260726/V20260825 迁移历史会忽略本 baseline 并继续由 Flyway validate。

CREATE TABLE school_classes_0 (
    id BIGINT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    grade_name VARCHAR(120) NOT NULL,
    grade_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_school_classes_0_tenant_grade_id_active
        UNIQUE (tenant_id, grade_id, id)
);

CREATE TABLE school_classes_1 (
    id BIGINT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    grade_name VARCHAR(120) NOT NULL,
    grade_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_school_classes_1_tenant_grade_id_active
        UNIQUE (tenant_id, grade_id, id)
);

CREATE TABLE school_class_users_0 (
    id BIGINT PRIMARY KEY,
    grade_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    school_class_id BIGINT NOT NULL,
    create_time TIMESTAMP NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_school_class_users_0_class
        FOREIGN KEY (tenant_id, grade_id, school_class_id)
        REFERENCES school_classes_0 (tenant_id, grade_id, id)
);

CREATE TABLE school_class_users_1 (
    id BIGINT PRIMARY KEY,
    grade_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    school_class_id BIGINT NOT NULL,
    create_time TIMESTAMP NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_school_class_users_1_class
        FOREIGN KEY (tenant_id, grade_id, school_class_id)
        REFERENCES school_classes_1 (tenant_id, grade_id, id)
);

CREATE UNIQUE INDEX uk_school_classes_0_tenant_grade_name_active
    ON school_classes_0 (tenant_id, grade_id, name, is_deleted);
CREATE UNIQUE INDEX uk_school_classes_1_tenant_grade_name_active
    ON school_classes_1 (tenant_id, grade_id, name, is_deleted);
CREATE UNIQUE INDEX uk_school_class_users_0_tenant_pair_active
    ON school_class_users_0 (tenant_id, grade_id, school_class_id, user_id, is_deleted);
CREATE UNIQUE INDEX uk_school_class_users_1_tenant_pair_active
    ON school_class_users_1 (tenant_id, grade_id, school_class_id, user_id, is_deleted);
CREATE INDEX idx_school_classes_0_grade_id ON school_classes_0 (grade_id);
CREATE INDEX idx_school_classes_1_grade_id ON school_classes_1 (grade_id);
CREATE INDEX idx_school_class_users_0_grade_class
    ON school_class_users_0 (grade_id, school_class_id);
CREATE INDEX idx_school_class_users_1_grade_class
    ON school_class_users_1 (grade_id, school_class_id);
CREATE INDEX idx_school_classes_0_tenant_grade
    ON school_classes_0 (tenant_id, grade_id, is_deleted);
CREATE INDEX idx_school_classes_1_tenant_grade
    ON school_classes_1 (tenant_id, grade_id, is_deleted);
CREATE INDEX idx_school_class_users_0_tenant_grade_class
    ON school_class_users_0 (tenant_id, grade_id, school_class_id);
CREATE INDEX idx_school_class_users_1_tenant_grade_class
    ON school_class_users_1 (tenant_id, grade_id, school_class_id);
