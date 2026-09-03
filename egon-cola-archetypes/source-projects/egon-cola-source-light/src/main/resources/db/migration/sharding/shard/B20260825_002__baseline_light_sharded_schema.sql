-- 变更内容：直接建立 Light 分片表在 V20260825_002 后的累计最终结构。
-- 影响范围：每个 shard primary 内的 school_classes_0、school_classes_1、class_course_schedules_0、class_course_schedules_1 表及其局部约束和索引。
-- 兼容性说明：仅用于空的新环境；已有 V20260726/V20260825 迁移历史会忽略本 baseline 并继续由 Flyway validate。

CREATE TABLE school_classes_0 (
    id BIGINT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    semester VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE school_classes_1 (
    id BIGINT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    semester VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE class_course_schedules_0 (
    id BIGINT PRIMARY KEY,
    school_class_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_class_course_start_0
        UNIQUE (school_class_id, course_id, starts_at),
    CONSTRAINT fk_schedule_class_0
        FOREIGN KEY (school_class_id) REFERENCES school_classes_0 (id)
);

CREATE TABLE class_course_schedules_1 (
    id BIGINT PRIMARY KEY,
    school_class_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_class_course_start_1
        UNIQUE (school_class_id, course_id, starts_at),
    CONSTRAINT fk_schedule_class_1
        FOREIGN KEY (school_class_id) REFERENCES school_classes_1 (id)
);

CREATE INDEX idx_school_classes_tenant_active_0
    ON school_classes_0 (tenant_id, id);
CREATE INDEX idx_school_classes_tenant_active_1
    ON school_classes_1 (tenant_id, id);
CREATE UNIQUE INDEX uk_class_course_start_tenant_active_0
    ON class_course_schedules_0 (tenant_id, school_class_id, course_id, starts_at, is_deleted);
CREATE UNIQUE INDEX uk_class_course_start_tenant_active_1
    ON class_course_schedules_1 (tenant_id, school_class_id, course_id, starts_at, is_deleted);
