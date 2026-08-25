-- 变更内容：将 Evaluation 主数据课程表迁移到正数 BIGINT、tenant_id 和 EgonModel 七项技术字段。
-- 影响范围：master_data 库中的 course（迁移后逻辑表 evaluation_course）。
-- 兼容性说明：旧表中的历史行必须先完成明确的 Long/tenant 离线映射；不可转换或未知数据会使本迁移失败。

-- 脚手架只接受空的旧主数据表；未知历史身份不得在 Flyway 内猜测。
SELECT 1 / CASE WHEN EXISTS (SELECT 1 FROM course) THEN 0 ELSE 1 END;

ALTER TABLE course ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE course RENAME COLUMN created_at TO create_time;
ALTER TABLE course RENAME COLUMN updated_at TO update_time;
ALTER TABLE course RENAME TO evaluation_course;

ALTER TABLE evaluation_course ADD COLUMN tenant_id BIGINT DEFAULT 1;
ALTER TABLE evaluation_course ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE evaluation_course ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE evaluation_course ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;

ALTER TABLE evaluation_course ALTER COLUMN id SET NOT NULL;
ALTER TABLE evaluation_course ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE evaluation_course ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE evaluation_course ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE evaluation_course ALTER COLUMN update_user_id SET NOT NULL;
ALTER TABLE evaluation_course ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE evaluation_course ALTER COLUMN is_deleted SET NOT NULL;

ALTER TABLE evaluation_course DROP CONSTRAINT uk_course_code;
CREATE UNIQUE INDEX uk_evaluation_course_tenant_code_active
    ON evaluation_course (tenant_id, code, is_deleted);
CREATE INDEX idx_evaluation_course_tenant_active_create
    ON evaluation_course (tenant_id, is_deleted, create_time, id);
