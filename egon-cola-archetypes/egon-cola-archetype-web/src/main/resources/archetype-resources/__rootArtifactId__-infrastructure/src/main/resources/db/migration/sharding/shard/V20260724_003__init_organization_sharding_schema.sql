-- 变更内容：初始化 Organization 分片节点中的班级和班级成员物理分表。
-- 影响范围：每个 shard 库的 school_classes_0/1 与 school_class_users_0/1。
-- 兼容性说明：仅初始化新建分片节点；grade_id 是共同分片键，且不建立指向 single 库 users 或 grades 的跨库外键。

CREATE TABLE school_classes_0 (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    grade_name VARCHAR(120) NOT NULL,
    grade_id VARCHAR(36) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_school_classes_0_grade_name UNIQUE (grade_id, name)
);

CREATE TABLE school_classes_1 (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    grade_name VARCHAR(120) NOT NULL,
    grade_id VARCHAR(36) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_school_classes_1_grade_name UNIQUE (grade_id, name)
);

CREATE TABLE school_class_users_0 (
    id VARCHAR(36) PRIMARY KEY,
    grade_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    school_class_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_school_class_users_0 UNIQUE (grade_id, school_class_id, user_id),
    CONSTRAINT fk_school_class_users_0_class
        FOREIGN KEY (school_class_id) REFERENCES school_classes_0(id)
);

CREATE TABLE school_class_users_1 (
    id VARCHAR(36) PRIMARY KEY,
    grade_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    school_class_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_school_class_users_1 UNIQUE (grade_id, school_class_id, user_id),
    CONSTRAINT fk_school_class_users_1_class
        FOREIGN KEY (school_class_id) REFERENCES school_classes_1(id)
);

CREATE INDEX idx_school_classes_0_grade_id ON school_classes_0(grade_id);
CREATE INDEX idx_school_classes_1_grade_id ON school_classes_1(grade_id);
CREATE INDEX idx_school_class_users_0_grade_class
    ON school_class_users_0(grade_id, school_class_id);
CREATE INDEX idx_school_class_users_1_grade_class
    ON school_class_users_1(grade_id, school_class_id);
