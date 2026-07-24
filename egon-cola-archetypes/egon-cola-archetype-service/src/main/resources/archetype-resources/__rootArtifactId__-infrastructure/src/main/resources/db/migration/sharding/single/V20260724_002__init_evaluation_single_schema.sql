-- 变更内容：初始化 Evaluation 分片模式中不分库分表的课程表。
-- 影响范围：single 库中的 course；排课、考试、试卷和成绩数据不在本库创建。
-- 约束说明：course 使用应用侧 UUIDv7 代理主键，供各分片表通过业务标识关联。

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
