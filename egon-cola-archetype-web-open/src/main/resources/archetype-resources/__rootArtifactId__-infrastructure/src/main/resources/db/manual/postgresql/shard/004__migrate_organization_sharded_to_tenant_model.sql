-- Manual operator migration: 将 Organization 分片表迁移到 tenant_id 与 EgonModel 技术字段。
-- Scope: 每个 shard 库的 school_classes_0/1、school_class_users_0/1。
-- Compatibility: 历史行必须先完成明确的租户离线映射；未知身份不得由脚本猜测。

-- 当前脚手架没有租户历史映射；执行前请完成离线映射并在事务外备份。
SELECT 1 / CASE WHEN EXISTS (SELECT 1 FROM school_classes_0)
    OR EXISTS (SELECT 1 FROM school_classes_1)
    OR EXISTS (SELECT 1 FROM school_class_users_0)
    OR EXISTS (SELECT 1 FROM school_class_users_1)
    THEN 0 ELSE 1 END;

ALTER TABLE school_classes_0 RENAME COLUMN created_at TO create_time;
ALTER TABLE school_classes_1 RENAME COLUMN created_at TO create_time;
ALTER TABLE school_class_users_0 RENAME COLUMN created_at TO create_time;
ALTER TABLE school_class_users_1 RENAME COLUMN created_at TO create_time;
ALTER TABLE school_classes_0 ADD COLUMN tenant_id BIGINT DEFAULT 1;
ALTER TABLE school_classes_1 ADD COLUMN tenant_id BIGINT DEFAULT 1;
ALTER TABLE school_class_users_0 ADD COLUMN tenant_id BIGINT DEFAULT 1;
ALTER TABLE school_class_users_1 ADD COLUMN tenant_id BIGINT DEFAULT 1;
ALTER TABLE school_classes_0 ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE school_classes_1 ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE school_class_users_0 ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE school_class_users_1 ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE school_classes_0 ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE school_classes_1 ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE school_class_users_0 ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE school_class_users_1 ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE school_classes_0 ADD COLUMN update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE school_classes_1 ADD COLUMN update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE school_class_users_0 ADD COLUMN update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE school_class_users_1 ADD COLUMN update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE school_classes_0 ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE school_classes_1 ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE school_class_users_0 ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE school_class_users_1 ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;

ALTER TABLE school_classes_0 ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE school_classes_1 ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE school_class_users_0 ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE school_class_users_1 ALTER COLUMN tenant_id SET NOT NULL;

DROP INDEX IF EXISTS uk_school_classes_0_grade_name;
DROP INDEX IF EXISTS uk_school_classes_1_grade_name;
DROP INDEX IF EXISTS uk_school_class_users_0;
DROP INDEX IF EXISTS uk_school_class_users_1;
CREATE UNIQUE INDEX uk_school_classes_0_tenant_grade_name_active
    ON school_classes_0 (tenant_id, grade_id, name, is_deleted);
CREATE UNIQUE INDEX uk_school_classes_1_tenant_grade_name_active
    ON school_classes_1 (tenant_id, grade_id, name, is_deleted);
CREATE UNIQUE INDEX uk_school_class_users_0_tenant_pair_active
    ON school_class_users_0 (tenant_id, grade_id, school_class_id, user_id, is_deleted);
CREATE UNIQUE INDEX uk_school_class_users_1_tenant_pair_active
    ON school_class_users_1 (tenant_id, grade_id, school_class_id, user_id, is_deleted);
CREATE INDEX idx_school_classes_0_tenant_grade ON school_classes_0 (tenant_id, grade_id, is_deleted);
CREATE INDEX idx_school_classes_1_tenant_grade ON school_classes_1 (tenant_id, grade_id, is_deleted);
CREATE INDEX idx_school_class_users_0_tenant_class
    ON school_class_users_0 (tenant_id, grade_id, school_class_id);
CREATE INDEX idx_school_class_users_1_tenant_class
    ON school_class_users_1 (tenant_id, grade_id, school_class_id);
