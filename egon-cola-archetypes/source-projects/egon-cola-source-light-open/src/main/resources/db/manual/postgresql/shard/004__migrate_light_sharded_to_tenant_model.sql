-- 变更内容：将 Light 分片表迁移到 Long 主键、tenant_id 路由和 EgonModel 七项技术字段。
-- 影响范围：light_school_classes_0/_1、light_class_course_schedules_0/_1 及其局部约束。
-- 兼容性说明：旧表中的非空或不可转换标识会使类型变更失败；请先完成外部数据回填和校验。

-- 迁移前置条件：该脚手架只接受空的 V20260726 分片表；历史行必须先完成离线身份/租户映射。
SELECT 1 / CASE WHEN EXISTS (SELECT 1 FROM light_school_classes_0)
    OR EXISTS (SELECT 1 FROM light_school_classes_1)
    OR EXISTS (SELECT 1 FROM light_class_course_schedules_0)
    OR EXISTS (SELECT 1 FROM light_class_course_schedules_1)
    THEN 0 ELSE 1 END;

ALTER TABLE light_school_classes_0 ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE light_school_classes_1 ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE light_class_course_schedules_0 ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE light_class_course_schedules_1 ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE light_class_course_schedules_0 ALTER COLUMN school_class_id SET DATA TYPE BIGINT;
ALTER TABLE light_class_course_schedules_1 ALTER COLUMN school_class_id SET DATA TYPE BIGINT;
ALTER TABLE light_class_course_schedules_0 ALTER COLUMN course_id SET DATA TYPE BIGINT;
ALTER TABLE light_class_course_schedules_1 ALTER COLUMN course_id SET DATA TYPE BIGINT;

ALTER TABLE light_school_classes_0 ADD COLUMN tenant_id BIGINT;
ALTER TABLE light_school_classes_0 ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE light_school_classes_0 ADD COLUMN create_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE light_school_classes_0 ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE light_school_classes_0 ADD COLUMN update_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE light_school_classes_0 ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE light_school_classes_1 ADD COLUMN tenant_id BIGINT;
ALTER TABLE light_school_classes_1 ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE light_school_classes_1 ADD COLUMN create_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE light_school_classes_1 ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE light_school_classes_1 ADD COLUMN update_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE light_school_classes_1 ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE light_class_course_schedules_0 ADD COLUMN tenant_id BIGINT;
ALTER TABLE light_class_course_schedules_0 ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE light_class_course_schedules_0 ADD COLUMN create_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE light_class_course_schedules_0 ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE light_class_course_schedules_0 ADD COLUMN update_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE light_class_course_schedules_0 ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE light_class_course_schedules_1 ADD COLUMN tenant_id BIGINT;
ALTER TABLE light_class_course_schedules_1 ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE light_class_course_schedules_1 ADD COLUMN create_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE light_class_course_schedules_1 ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE light_class_course_schedules_1 ADD COLUMN update_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE light_class_course_schedules_1 ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;

ALTER TABLE light_school_classes_0 DROP COLUMN created_at;
ALTER TABLE light_school_classes_1 DROP COLUMN created_at;
ALTER TABLE light_class_course_schedules_0 DROP COLUMN created_at;
ALTER TABLE light_class_course_schedules_1 DROP COLUMN created_at;

ALTER TABLE light_school_classes_0 ALTER COLUMN id SET NOT NULL;
ALTER TABLE light_school_classes_1 ALTER COLUMN id SET NOT NULL;
ALTER TABLE light_class_course_schedules_0 ALTER COLUMN id SET NOT NULL;
ALTER TABLE light_class_course_schedules_1 ALTER COLUMN id SET NOT NULL;
ALTER TABLE light_school_classes_0 ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE light_school_classes_1 ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE light_class_course_schedules_0 ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE light_class_course_schedules_1 ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE light_school_classes_0 ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE light_school_classes_0 ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE light_school_classes_0 ALTER COLUMN update_user_id SET NOT NULL;
ALTER TABLE light_school_classes_0 ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE light_school_classes_0 ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE light_school_classes_1 ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE light_school_classes_1 ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE light_school_classes_1 ALTER COLUMN update_user_id SET NOT NULL;
ALTER TABLE light_school_classes_1 ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE light_school_classes_1 ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE light_class_course_schedules_0 ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE light_class_course_schedules_0 ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE light_class_course_schedules_0 ALTER COLUMN update_user_id SET NOT NULL;
ALTER TABLE light_class_course_schedules_0 ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE light_class_course_schedules_0 ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE light_class_course_schedules_1 ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE light_class_course_schedules_1 ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE light_class_course_schedules_1 ALTER COLUMN update_user_id SET NOT NULL;
ALTER TABLE light_class_course_schedules_1 ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE light_class_course_schedules_1 ALTER COLUMN is_deleted SET NOT NULL;

CREATE INDEX idx_light_school_classes_tenant_active_0
    ON light_school_classes_0 (tenant_id, id);
CREATE INDEX idx_light_school_classes_tenant_active_1
    ON light_school_classes_1 (tenant_id, id);
CREATE UNIQUE INDEX uk_class_course_start_tenant_active_0
    ON light_class_course_schedules_0 (tenant_id, school_class_id, course_id, starts_at, is_deleted);
CREATE UNIQUE INDEX uk_class_course_start_tenant_active_1
    ON light_class_course_schedules_1 (tenant_id, school_class_id, course_id, starts_at, is_deleted);
