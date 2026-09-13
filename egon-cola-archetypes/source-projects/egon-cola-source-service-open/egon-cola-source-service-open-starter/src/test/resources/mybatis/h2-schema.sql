CREATE TABLE evaluation_course (
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    id BIGINT PRIMARY KEY,
    code VARCHAR(96) NOT NULL,
    name VARCHAR(128) NOT NULL,
    credit INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,


    CONSTRAINT uk_course_code UNIQUE (code),
    CONSTRAINT ck_course_credit CHECK (credit > 0)
);

CREATE TABLE evaluation_course_schedule (
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    id BIGINT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL,


    CONSTRAINT ck_course_schedule_0_window CHECK (starts_at < ends_at)
);

CREATE TABLE evaluation_exam (
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    id BIGINT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL,


    CONSTRAINT ck_exam_0_window CHECK (starts_at < ends_at)
);

CREATE TABLE evaluation_exam_paper (
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    id BIGINT PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    total_points INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,


    CONSTRAINT uk_exam_paper_0_exam UNIQUE (exam_id),
    CONSTRAINT fk_exam_paper_0_exam FOREIGN KEY (exam_id) REFERENCES evaluation_exam(id),
    CONSTRAINT ck_exam_paper_0_points CHECK (total_points > 0)
);

CREATE TABLE evaluation_score (
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    id BIGINT PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    points INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,


    CONSTRAINT uk_score_0_exam_student UNIQUE (exam_id, student_id),
    CONSTRAINT fk_score_0_exam FOREIGN KEY (exam_id) REFERENCES evaluation_exam(id),
    CONSTRAINT ck_score_0_points CHECK (points BETWEEN 0 AND 100)
);
