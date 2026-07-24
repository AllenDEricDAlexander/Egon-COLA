-- 变更内容：初始化 Evaluation 默认单数据源模式的完整课程、排课、考试、试卷和成绩表。
-- 影响范围：全新脚手架工程的全部 Evaluation 业务数据；不涉及历史数据迁移。
-- 约束说明：全部代理主键由应用侧生成 UUIDv7，默认模式保留同库外键和业务唯一约束。

CREATE TABLE course (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(96) NOT NULL,
    name VARCHAR(128) NOT NULL,
    credit INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_course_code UNIQUE (code)
);

CREATE TABLE course_schedule (
    id VARCHAR(36) PRIMARY KEY,
    course_id VARCHAR(36) NOT NULL,
    class_id VARCHAR(64) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_course_schedule_course FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT ck_course_schedule_window CHECK (starts_at < ends_at)
);

CREATE TABLE exam (
    id VARCHAR(36) PRIMARY KEY,
    course_id VARCHAR(36) NOT NULL,
    title VARCHAR(128) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_exam_course FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT ck_exam_window CHECK (starts_at < ends_at)
);

CREATE TABLE exam_paper (
    id VARCHAR(36) PRIMARY KEY,
    exam_id VARCHAR(36) NOT NULL,
    title VARCHAR(128) NOT NULL,
    total_points INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_exam_paper_exam UNIQUE (exam_id),
    CONSTRAINT fk_exam_paper_exam FOREIGN KEY (exam_id) REFERENCES exam(id),
    CONSTRAINT ck_exam_paper_points CHECK (total_points > 0)
);

CREATE TABLE score (
    id VARCHAR(36) PRIMARY KEY,
    exam_id VARCHAR(36) NOT NULL,
    course_id VARCHAR(36) NOT NULL,
    student_id VARCHAR(64) NOT NULL,
    points INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_score_exam_student UNIQUE (exam_id, student_id),
    CONSTRAINT fk_score_exam FOREIGN KEY (exam_id) REFERENCES exam(id),
    CONSTRAINT fk_score_course FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT ck_score_points CHECK (points BETWEEN 0 AND 100)
);

CREATE INDEX idx_course_schedule_overlap
    ON course_schedule(course_id, class_id, starts_at, ends_at);
CREATE INDEX idx_exam_course_created ON exam(course_id, created_at, id);
CREATE INDEX idx_score_exam_created ON score(exam_id, created_at, id);
CREATE INDEX idx_score_course_id ON score(course_id);
CREATE INDEX idx_score_student_id ON score(student_id);
