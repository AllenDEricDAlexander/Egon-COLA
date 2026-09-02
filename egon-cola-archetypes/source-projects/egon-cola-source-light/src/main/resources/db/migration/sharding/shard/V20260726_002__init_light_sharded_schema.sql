-- 变更内容：初始化 Light 模板按 UUIDv7 分片键共置的班级与排课物理表。
-- 影响范围：每个 shard primary 内的 school_classes_0、school_classes_1、class_course_schedules_0、class_course_schedules_1 表及其局部约束。
-- 兼容性说明：本脚手架尚未执行迁移，不涉及历史数据、在线迁移或回滚兼容。

CREATE TABLE school_classes_0 (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    semester VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE school_classes_1 (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    semester VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE class_course_schedules_0 (
    id VARCHAR(36) PRIMARY KEY,
    school_class_id VARCHAR(36) NOT NULL,
    course_id VARCHAR(36) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_class_course_start_0
        UNIQUE (school_class_id, course_id, starts_at),
    CONSTRAINT fk_schedule_class_0
        FOREIGN KEY (school_class_id) REFERENCES school_classes_0 (id)
);

CREATE TABLE class_course_schedules_1 (
    id VARCHAR(36) PRIMARY KEY,
    school_class_id VARCHAR(36) NOT NULL,
    course_id VARCHAR(36) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_class_course_start_1
        UNIQUE (school_class_id, course_id, starts_at),
    CONSTRAINT fk_schedule_class_1
        FOREIGN KEY (school_class_id) REFERENCES school_classes_1 (id)
);
