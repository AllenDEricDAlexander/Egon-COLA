-- Complete PostgreSQL initialization for the service archetype; only an empty managed target is eligible.
-- Tables retain existing business keys and foreign keys. IDs are provided by the application.
DO $egon$
BEGIN
    IF current_setting('egon_migration.role') = 'MASTER_DATA' THEN
        CREATE TABLE evaluation_course (
            id BIGINT PRIMARY KEY,
            code VARCHAR(96) NOT NULL,
            name VARCHAR(128) NOT NULL,
            credit INTEGER NOT NULL,
            status VARCHAR(32) NOT NULL,
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            tenant_id BIGINT NOT NULL DEFAULT 1,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
        );

        CREATE UNIQUE INDEX uk_evaluation_course_tenant_code_active ON evaluation_course (tenant_id, code) WHERE deleted_at IS NULL;

        CREATE INDEX idx_evaluation_course_tenant_active_create ON evaluation_course (tenant_id, create_time, id) WHERE deleted_at IS NULL;
    ELSIF current_setting('egon_migration.role') = 'SHARD' THEN
        CREATE TABLE evaluation_course_schedule_0 (
            id BIGINT PRIMARY KEY,
            course_id BIGINT NOT NULL,
            class_id BIGINT NOT NULL,
            starts_at TIMESTAMP NOT NULL,
            ends_at TIMESTAMP NOT NULL,
            status VARCHAR(32) NOT NULL,
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            tenant_id BIGINT NOT NULL DEFAULT 1,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            CONSTRAINT ck_course_schedule_0_window CHECK (starts_at < ends_at)
        );

        CREATE INDEX idx_course_schedule_0_overlap ON evaluation_course_schedule_0 (course_id, class_id, starts_at, ends_at);

        CREATE TABLE evaluation_course_schedule_1 (
            id BIGINT PRIMARY KEY,
            course_id BIGINT NOT NULL,
            class_id BIGINT NOT NULL,
            starts_at TIMESTAMP NOT NULL,
            ends_at TIMESTAMP NOT NULL,
            status VARCHAR(32) NOT NULL,
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            tenant_id BIGINT NOT NULL DEFAULT 1,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            CONSTRAINT ck_course_schedule_1_window CHECK (starts_at < ends_at)
        );

        CREATE INDEX idx_course_schedule_1_overlap ON evaluation_course_schedule_1 (course_id, class_id, starts_at, ends_at);

        CREATE TABLE evaluation_exam_0 (
            id BIGINT PRIMARY KEY,
            course_id BIGINT NOT NULL,
            title VARCHAR(128) NOT NULL,
            starts_at TIMESTAMP NOT NULL,
            ends_at TIMESTAMP NOT NULL,
            status VARCHAR(32) NOT NULL,
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            tenant_id BIGINT NOT NULL DEFAULT 1,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            CONSTRAINT ck_exam_0_window CHECK (starts_at < ends_at)
        );

        CREATE INDEX idx_exam_0_course_created ON evaluation_exam_0 (course_id, create_time, id);

        CREATE TABLE evaluation_exam_1 (
            id BIGINT PRIMARY KEY,
            course_id BIGINT NOT NULL,
            title VARCHAR(128) NOT NULL,
            starts_at TIMESTAMP NOT NULL,
            ends_at TIMESTAMP NOT NULL,
            status VARCHAR(32) NOT NULL,
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            tenant_id BIGINT NOT NULL DEFAULT 1,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            CONSTRAINT ck_exam_1_window CHECK (starts_at < ends_at)
        );

        CREATE INDEX idx_exam_1_course_created ON evaluation_exam_1 (course_id, create_time, id);

        CREATE TABLE evaluation_exam_paper_0 (
            id BIGINT PRIMARY KEY,
            exam_id BIGINT NOT NULL,
            title VARCHAR(128) NOT NULL,
            total_points INTEGER NOT NULL,
            status VARCHAR(32) NOT NULL,
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            tenant_id BIGINT NOT NULL DEFAULT 1,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            CONSTRAINT fk_exam_paper_0_exam
                FOREIGN KEY (exam_id) REFERENCES evaluation_exam_0 (id),
            CONSTRAINT ck_exam_paper_0_points CHECK (total_points > 0)
        );

        CREATE UNIQUE INDEX uk_evaluation_exam_paper_0_tenant_exam_active ON evaluation_exam_paper_0 (tenant_id, exam_id) WHERE deleted_at IS NULL;

        CREATE TABLE evaluation_exam_paper_1 (
            id BIGINT PRIMARY KEY,
            exam_id BIGINT NOT NULL,
            title VARCHAR(128) NOT NULL,
            total_points INTEGER NOT NULL,
            status VARCHAR(32) NOT NULL,
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            tenant_id BIGINT NOT NULL DEFAULT 1,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            CONSTRAINT fk_exam_paper_1_exam
                FOREIGN KEY (exam_id) REFERENCES evaluation_exam_1 (id),
            CONSTRAINT ck_exam_paper_1_points CHECK (total_points > 0)
        );

        CREATE UNIQUE INDEX uk_evaluation_exam_paper_1_tenant_exam_active ON evaluation_exam_paper_1 (tenant_id, exam_id) WHERE deleted_at IS NULL;

        CREATE TABLE evaluation_score_0 (
            id BIGINT PRIMARY KEY,
            exam_id BIGINT NOT NULL,
            course_id BIGINT NOT NULL,
            student_id BIGINT NOT NULL,
            points INTEGER NOT NULL,
            status VARCHAR(32) NOT NULL,
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            tenant_id BIGINT NOT NULL DEFAULT 1,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            CONSTRAINT fk_score_0_exam
                FOREIGN KEY (exam_id) REFERENCES evaluation_exam_0 (id),
            CONSTRAINT ck_score_0_points CHECK (points BETWEEN 0 AND 100)
        );

        CREATE INDEX idx_score_0_exam_created ON evaluation_score_0 (exam_id, create_time, id);

        CREATE INDEX idx_score_0_course_id ON evaluation_score_0 (course_id);

        CREATE INDEX idx_score_0_student_id ON evaluation_score_0 (student_id);

        CREATE UNIQUE INDEX uk_evaluation_score_0_tenant_exam_student_active ON evaluation_score_0 (tenant_id, exam_id, student_id) WHERE deleted_at IS NULL;

        CREATE INDEX idx_evaluation_score_0_tenant_exam_active_create ON evaluation_score_0 (tenant_id, exam_id, create_time, id) WHERE deleted_at IS NULL;

        CREATE TABLE evaluation_score_1 (
            id BIGINT PRIMARY KEY,
            exam_id BIGINT NOT NULL,
            course_id BIGINT NOT NULL,
            student_id BIGINT NOT NULL,
            points INTEGER NOT NULL,
            status VARCHAR(32) NOT NULL,
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            tenant_id BIGINT NOT NULL DEFAULT 1,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            CONSTRAINT fk_score_1_exam
                FOREIGN KEY (exam_id) REFERENCES evaluation_exam_1 (id),
            CONSTRAINT ck_score_1_points CHECK (points BETWEEN 0 AND 100)
        );

        CREATE INDEX idx_score_1_exam_created ON evaluation_score_1 (exam_id, create_time, id);

        CREATE INDEX idx_score_1_course_id ON evaluation_score_1 (course_id);

        CREATE INDEX idx_score_1_student_id ON evaluation_score_1 (student_id);

        CREATE UNIQUE INDEX uk_evaluation_score_1_tenant_exam_student_active ON evaluation_score_1 (tenant_id, exam_id, student_id) WHERE deleted_at IS NULL;

        CREATE INDEX idx_evaluation_score_1_tenant_exam_active_create ON evaluation_score_1 (tenant_id, exam_id, create_time, id) WHERE deleted_at IS NULL;
    ELSE
        RAISE EXCEPTION 'Unknown managed DDL role';
    END IF;
END
$egon$;

CREATE TABLE ddl_history (
    tenant_id bigint NOT NULL DEFAULT 0 CHECK (tenant_id = 0),
    script varchar(500) NOT NULL,
    type varchar(30) NOT NULL DEFAULT 'SQL' CHECK (type = 'SQL'),
    version varchar(30) NOT NULL,
    checksum char(64) NOT NULL CHECK (checksum ~ '^[0-9a-f]{64}$'),
    installed_on timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_ms bigint NOT NULL CHECK (execution_ms >= 0),
    route_fingerprint char(64) NOT NULL CHECK (route_fingerprint ~ '^[0-9a-f]{64}$'),
    CONSTRAINT pk_ddl_history PRIMARY KEY (script, type),
    CONSTRAINT uk_ddl_history_type_version UNIQUE (type, version)
);
