CREATE TABLE evaluation_course (
    id BIGINT PRIMARY KEY,
    code VARCHAR(96) NOT NULL,
    name VARCHAR(128) NOT NULL,
    credit INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    deleted_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE evaluation_course_schedule (
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
    deleted_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_course_schedule_0_window CHECK (starts_at < ends_at)
);

CREATE TABLE evaluation_exam (
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
    deleted_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_exam_0_window CHECK (starts_at < ends_at)
);

CREATE TABLE evaluation_exam_paper (
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
    deleted_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_exam_paper_0_exam
        FOREIGN KEY (exam_id) REFERENCES evaluation_exam (id),
    CONSTRAINT ck_exam_paper_0_points CHECK (total_points > 0)
);

CREATE TABLE evaluation_score (
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
    deleted_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_score_0_exam
        FOREIGN KEY (exam_id) REFERENCES evaluation_exam (id),
    CONSTRAINT ck_score_0_points CHECK (points BETWEEN 0 AND 100)
);
