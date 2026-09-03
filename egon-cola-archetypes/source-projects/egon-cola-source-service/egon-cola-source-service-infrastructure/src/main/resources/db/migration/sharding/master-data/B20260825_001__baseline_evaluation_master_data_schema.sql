-- 变更内容：直接建立 Evaluation 主数据课程表在 V20260825_001 后的累计最终结构。
-- 影响范围：master_data 库中的 evaluation_course 表及其租户唯一和查询索引。
-- 兼容性说明：仅用于空的新环境；已有 V20260726/V20260825 迁移历史会忽略本 baseline 并继续由 Flyway validate。

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
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX uk_evaluation_course_tenant_code_active
    ON evaluation_course (tenant_id, code, is_deleted);
CREATE INDEX idx_evaluation_course_tenant_active_create
    ON evaluation_course (tenant_id, is_deleted, create_time, id);
