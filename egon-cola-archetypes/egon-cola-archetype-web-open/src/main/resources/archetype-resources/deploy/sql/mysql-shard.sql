-- Web Open sharded schema. Execute manually on every shard primary.
-- The application never reads or executes this file.

CREATE TABLE IF NOT EXISTS school_classes_0 (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    grade_name VARCHAR(120) NOT NULL,
    grade_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_school_classes_0_grade_name UNIQUE (grade_id, name),
    CONSTRAINT uk_school_classes_0_grade_id UNIQUE (grade_id, id)
);

CREATE TABLE IF NOT EXISTS school_classes_1 (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    grade_name VARCHAR(120) NOT NULL,
    grade_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_school_classes_1_grade_name UNIQUE (grade_id, name),
    CONSTRAINT uk_school_classes_1_grade_id UNIQUE (grade_id, id)
);

CREATE TABLE IF NOT EXISTS school_class_users_0 (
    id BIGINT NOT NULL PRIMARY KEY,
    grade_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    school_class_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_school_class_users_0 UNIQUE (grade_id, school_class_id, user_id)
);

CREATE TABLE IF NOT EXISTS school_class_users_1 (
    id BIGINT NOT NULL PRIMARY KEY,
    grade_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    school_class_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_school_class_users_1 UNIQUE (grade_id, school_class_id, user_id)
);

CREATE INDEX idx_school_classes_0_grade_id ON school_classes_0 (grade_id);
CREATE INDEX idx_school_classes_1_grade_id ON school_classes_1 (grade_id);
CREATE INDEX idx_school_class_users_0_grade_class
    ON school_class_users_0 (grade_id, school_class_id);
CREATE INDEX idx_school_class_users_1_grade_class
    ON school_class_users_1 (grade_id, school_class_id);
