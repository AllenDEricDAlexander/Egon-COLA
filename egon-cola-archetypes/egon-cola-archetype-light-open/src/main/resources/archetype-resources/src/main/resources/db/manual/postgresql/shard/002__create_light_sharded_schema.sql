-- 变更内容：初始化 Light Open 按 Snowflake Long 稳定槽共置的班级与排课物理表。
-- 影响范围：每个 shard primary 内的 school_classes_0/_1 与 class_course_schedules_0/_1。
-- 兼容性说明：两张逻辑表的 suffix 定义必须在每个 shard primary 完全一致，不执行历史数据转换。

CREATE TABLE school_classes_0 (
    id BIGINT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    semester VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE school_classes_1 (
    id BIGINT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    semester VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE class_course_schedules_0 (
    id BIGINT PRIMARY KEY,
    school_class_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
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
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_class_course_start_1
        UNIQUE (school_class_id, course_id, starts_at),
    CONSTRAINT fk_schedule_class_1
        FOREIGN KEY (school_class_id) REFERENCES school_classes_1 (id)
);
