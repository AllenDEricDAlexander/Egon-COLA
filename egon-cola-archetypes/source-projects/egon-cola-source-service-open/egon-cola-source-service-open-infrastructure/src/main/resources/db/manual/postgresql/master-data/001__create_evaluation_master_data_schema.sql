-- Manual operator change: create Evaluation master-data schema.
-- Target: PostgreSQL master_data primary only; apply before shard/002.

CREATE TABLE course (
    id BIGINT PRIMARY KEY,
    code VARCHAR(96) NOT NULL,
    name VARCHAR(128) NOT NULL,
    credit INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_course_code UNIQUE (code),
    CONSTRAINT ck_course_credit CHECK (credit > 0)
);
