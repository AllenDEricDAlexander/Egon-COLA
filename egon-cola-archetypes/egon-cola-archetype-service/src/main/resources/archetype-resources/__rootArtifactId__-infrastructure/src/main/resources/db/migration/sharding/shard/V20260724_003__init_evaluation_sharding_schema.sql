-- 变更内容：初始化 Evaluation 分片节点中的排课、考试、试卷和成绩物理分表。
-- 影响范围：每个 shard 库的 course_schedule_0/1、exam_0/1、exam_paper_0/1 和 score_0/1。
-- 约束说明：考试族按 exam_id 同库同表后缀；不建立指向 single 库 course 的跨库外键。

CREATE TABLE course_schedule_0 (
    id VARCHAR(36) PRIMARY KEY,
    course_id VARCHAR(36) NOT NULL,
    class_id VARCHAR(64) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_course_schedule_0_window CHECK (starts_at < ends_at)
);

CREATE TABLE course_schedule_1 (
    id VARCHAR(36) PRIMARY KEY,
    course_id VARCHAR(36) NOT NULL,
    class_id VARCHAR(64) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_course_schedule_1_window CHECK (starts_at < ends_at)
);

CREATE TABLE exam_0 (
    id VARCHAR(36) PRIMARY KEY,
    course_id VARCHAR(36) NOT NULL,
    title VARCHAR(128) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_exam_0_window CHECK (starts_at < ends_at)
);

CREATE TABLE exam_1 (
    id VARCHAR(36) PRIMARY KEY,
    course_id VARCHAR(36) NOT NULL,
    title VARCHAR(128) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_exam_1_window CHECK (starts_at < ends_at)
);

CREATE TABLE exam_paper_0 (
    id VARCHAR(36) PRIMARY KEY,
    exam_id VARCHAR(36) NOT NULL,
    title VARCHAR(128) NOT NULL,
    total_points INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_exam_paper_0_exam UNIQUE (exam_id),
    CONSTRAINT fk_exam_paper_0_exam FOREIGN KEY (exam_id) REFERENCES exam_0(id),
    CONSTRAINT ck_exam_paper_0_points CHECK (total_points > 0)
);

CREATE TABLE exam_paper_1 (
    id VARCHAR(36) PRIMARY KEY,
    exam_id VARCHAR(36) NOT NULL,
    title VARCHAR(128) NOT NULL,
    total_points INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_exam_paper_1_exam UNIQUE (exam_id),
    CONSTRAINT fk_exam_paper_1_exam FOREIGN KEY (exam_id) REFERENCES exam_1(id),
    CONSTRAINT ck_exam_paper_1_points CHECK (total_points > 0)
);

CREATE TABLE score_0 (
    id VARCHAR(36) PRIMARY KEY,
    exam_id VARCHAR(36) NOT NULL,
    course_id VARCHAR(36) NOT NULL,
    student_id VARCHAR(64) NOT NULL,
    points INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_score_0_exam_student UNIQUE (exam_id, student_id),
    CONSTRAINT fk_score_0_exam FOREIGN KEY (exam_id) REFERENCES exam_0(id),
    CONSTRAINT ck_score_0_points CHECK (points BETWEEN 0 AND 100)
);

CREATE TABLE score_1 (
    id VARCHAR(36) PRIMARY KEY,
    exam_id VARCHAR(36) NOT NULL,
    course_id VARCHAR(36) NOT NULL,
    student_id VARCHAR(64) NOT NULL,
    points INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_score_1_exam_student UNIQUE (exam_id, student_id),
    CONSTRAINT fk_score_1_exam FOREIGN KEY (exam_id) REFERENCES exam_1(id),
    CONSTRAINT ck_score_1_points CHECK (points BETWEEN 0 AND 100)
);

CREATE INDEX idx_course_schedule_0_overlap
    ON course_schedule_0(course_id, class_id, starts_at, ends_at);
CREATE INDEX idx_course_schedule_1_overlap
    ON course_schedule_1(course_id, class_id, starts_at, ends_at);
CREATE INDEX idx_exam_0_course_created ON exam_0(course_id, created_at, id);
CREATE INDEX idx_exam_1_course_created ON exam_1(course_id, created_at, id);
CREATE INDEX idx_score_0_exam_created ON score_0(exam_id, created_at, id);
CREATE INDEX idx_score_1_exam_created ON score_1(exam_id, created_at, id);
CREATE INDEX idx_score_0_course_id ON score_0(course_id);
CREATE INDEX idx_score_1_course_id ON score_1(course_id);
CREATE INDEX idx_score_0_student_id ON score_0(student_id);
CREATE INDEX idx_score_1_student_id ON score_1(student_id);
