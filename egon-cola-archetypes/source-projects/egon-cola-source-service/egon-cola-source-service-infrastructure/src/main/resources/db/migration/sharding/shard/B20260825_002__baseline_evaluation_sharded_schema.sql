-- 变更内容：直接建立 Evaluation 分片表在 V20260825_002 后的累计最终结构。
-- 影响范围：每个 shard 库中的 evaluation_course_schedule、evaluation_exam、evaluation_exam_paper、evaluation_score 0/1 物理表及其约束和索引。
-- 兼容性说明：仅用于空的新环境；已有 V20260726/V20260825 迁移历史会忽略本 baseline 并继续由 Flyway validate。

CREATE TABLE evaluation_course_schedule_0 (
    id BIGINT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    create_user_id VARCHAR(128) DEFAULT 'migration',
    update_user_id VARCHAR(128) DEFAULT 'migration',
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT ck_course_schedule_0_window CHECK (starts_at < ends_at)
);

CREATE TABLE evaluation_course_schedule_1 (
    id BIGINT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    create_user_id VARCHAR(128) DEFAULT 'migration',
    update_user_id VARCHAR(128) DEFAULT 'migration',
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT ck_course_schedule_1_window CHECK (starts_at < ends_at)
);

CREATE TABLE evaluation_exam_0 (
    id BIGINT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    create_user_id VARCHAR(128) DEFAULT 'migration',
    update_user_id VARCHAR(128) DEFAULT 'migration',
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT ck_exam_0_window CHECK (starts_at < ends_at)
);

CREATE TABLE evaluation_exam_1 (
    id BIGINT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    create_user_id VARCHAR(128) DEFAULT 'migration',
    update_user_id VARCHAR(128) DEFAULT 'migration',
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT ck_exam_1_window CHECK (starts_at < ends_at)
);

CREATE TABLE evaluation_exam_paper_0 (
    id BIGINT PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    total_points INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    create_user_id VARCHAR(128) DEFAULT 'migration',
    update_user_id VARCHAR(128) DEFAULT 'migration',
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_exam_paper_0_exam
        FOREIGN KEY (exam_id) REFERENCES evaluation_exam_0 (id),
    CONSTRAINT ck_exam_paper_0_points CHECK (total_points > 0)
);

CREATE TABLE evaluation_exam_paper_1 (
    id BIGINT PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    total_points INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    create_user_id VARCHAR(128) DEFAULT 'migration',
    update_user_id VARCHAR(128) DEFAULT 'migration',
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_exam_paper_1_exam
        FOREIGN KEY (exam_id) REFERENCES evaluation_exam_1 (id),
    CONSTRAINT ck_exam_paper_1_points CHECK (total_points > 0)
);

CREATE TABLE evaluation_score_0 (
    id BIGINT PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    points INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    create_user_id VARCHAR(128) DEFAULT 'migration',
    update_user_id VARCHAR(128) DEFAULT 'migration',
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_score_0_exam
        FOREIGN KEY (exam_id) REFERENCES evaluation_exam_0 (id),
    CONSTRAINT ck_score_0_points CHECK (points BETWEEN 0 AND 100)
);

CREATE TABLE evaluation_score_1 (
    id BIGINT PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    points INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    create_user_id VARCHAR(128) DEFAULT 'migration',
    update_user_id VARCHAR(128) DEFAULT 'migration',
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_score_1_exam
        FOREIGN KEY (exam_id) REFERENCES evaluation_exam_1 (id),
    CONSTRAINT ck_score_1_points CHECK (points BETWEEN 0 AND 100)
);

CREATE INDEX idx_course_schedule_0_overlap
    ON evaluation_course_schedule_0 (course_id, class_id, starts_at, ends_at);
CREATE INDEX idx_course_schedule_1_overlap
    ON evaluation_course_schedule_1 (course_id, class_id, starts_at, ends_at);
CREATE INDEX idx_exam_0_course_created
    ON evaluation_exam_0 (course_id, create_time, id);
CREATE INDEX idx_exam_1_course_created
    ON evaluation_exam_1 (course_id, create_time, id);
CREATE UNIQUE INDEX uk_evaluation_exam_paper_0_tenant_exam_active
    ON evaluation_exam_paper_0 (tenant_id, exam_id, is_deleted);
CREATE UNIQUE INDEX uk_evaluation_exam_paper_1_tenant_exam_active
    ON evaluation_exam_paper_1 (tenant_id, exam_id, is_deleted);
CREATE INDEX idx_score_0_exam_created
    ON evaluation_score_0 (exam_id, create_time, id);
CREATE INDEX idx_score_1_exam_created
    ON evaluation_score_1 (exam_id, create_time, id);
CREATE INDEX idx_score_0_course_id ON evaluation_score_0 (course_id);
CREATE INDEX idx_score_1_course_id ON evaluation_score_1 (course_id);
CREATE INDEX idx_score_0_student_id ON evaluation_score_0 (student_id);
CREATE INDEX idx_score_1_student_id ON evaluation_score_1 (student_id);
CREATE UNIQUE INDEX uk_evaluation_score_0_tenant_exam_student_active
    ON evaluation_score_0 (tenant_id, exam_id, student_id, is_deleted);
CREATE UNIQUE INDEX uk_evaluation_score_1_tenant_exam_student_active
    ON evaluation_score_1 (tenant_id, exam_id, student_id, is_deleted);
CREATE INDEX idx_evaluation_score_0_tenant_exam_active_create
    ON evaluation_score_0 (tenant_id, exam_id, is_deleted, create_time, id);
CREATE INDEX idx_evaluation_score_1_tenant_exam_active_create
    ON evaluation_score_1 (tenant_id, exam_id, is_deleted, create_time, id);
