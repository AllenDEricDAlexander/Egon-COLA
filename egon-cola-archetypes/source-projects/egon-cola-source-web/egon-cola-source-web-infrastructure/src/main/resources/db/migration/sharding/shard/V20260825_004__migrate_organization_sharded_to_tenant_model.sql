-- 变更内容：将 Organization 分片表迁移到 Long 主键、tenant_id 与 EgonModel 技术字段。
-- 影响范围：每个 shard 库的 school_classes_0/1 与 school_class_users_0/1。
-- 兼容性说明：旧分片表必须为空；未知 UUID/租户归属不得在 Flyway 内猜测。

SELECT 1 / CASE WHEN NOT EXISTS (SELECT 1 FROM school_classes_0)
    AND NOT EXISTS (SELECT 1 FROM school_classes_1)
    AND NOT EXISTS (SELECT 1 FROM school_class_users_0)
    AND NOT EXISTS (SELECT 1 FROM school_class_users_1)
    THEN 1 ELSE 0 END;

ALTER TABLE school_class_users_0 DROP CONSTRAINT fk_school_class_users_0_class;
ALTER TABLE school_class_users_1 DROP CONSTRAINT fk_school_class_users_1_class;
ALTER TABLE school_classes_0 DROP CONSTRAINT uk_school_classes_0_grade_name;
ALTER TABLE school_classes_0 DROP CONSTRAINT uk_school_classes_0_grade_id;
ALTER TABLE school_classes_1 DROP CONSTRAINT uk_school_classes_1_grade_name;
ALTER TABLE school_classes_1 DROP CONSTRAINT uk_school_classes_1_grade_id;
ALTER TABLE school_class_users_0 DROP CONSTRAINT uk_school_class_users_0;
ALTER TABLE school_class_users_1 DROP CONSTRAINT uk_school_class_users_1;

ALTER TABLE school_classes_0 ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE school_classes_1 ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE school_classes_0 ALTER COLUMN grade_id SET DATA TYPE BIGINT;
ALTER TABLE school_classes_1 ALTER COLUMN grade_id SET DATA TYPE BIGINT;
ALTER TABLE school_class_users_0 ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE school_class_users_1 ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE school_class_users_0 ALTER COLUMN grade_id SET DATA TYPE BIGINT;
ALTER TABLE school_class_users_1 ALTER COLUMN grade_id SET DATA TYPE BIGINT;
ALTER TABLE school_class_users_0 ALTER COLUMN user_id SET DATA TYPE BIGINT;
ALTER TABLE school_class_users_1 ALTER COLUMN user_id SET DATA TYPE BIGINT;
ALTER TABLE school_class_users_0 ALTER COLUMN school_class_id SET DATA TYPE BIGINT;
ALTER TABLE school_class_users_1 ALTER COLUMN school_class_id SET DATA TYPE BIGINT;

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
ALTER TABLE school_classes_0 ADD COLUMN is_deleted SMALLINT DEFAULT 0;
ALTER TABLE school_classes_1 ADD COLUMN is_deleted SMALLINT DEFAULT 0;
ALTER TABLE school_class_users_0 ADD COLUMN is_deleted SMALLINT DEFAULT 0;
ALTER TABLE school_class_users_1 ADD COLUMN is_deleted SMALLINT DEFAULT 0;

ALTER TABLE school_classes_0 ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE school_classes_1 ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE school_class_users_0 ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE school_class_users_1 ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE school_classes_0 ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE school_classes_1 ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE school_class_users_0 ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE school_class_users_1 ALTER COLUMN create_user_id SET NOT NULL;
ALTER TABLE school_classes_0 ALTER COLUMN update_user_id SET NOT NULL;
ALTER TABLE school_classes_1 ALTER COLUMN update_user_id SET NOT NULL;
ALTER TABLE school_class_users_0 ALTER COLUMN update_user_id SET NOT NULL;
ALTER TABLE school_class_users_1 ALTER COLUMN update_user_id SET NOT NULL;
ALTER TABLE school_classes_0 ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE school_classes_1 ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE school_class_users_0 ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE school_class_users_1 ALTER COLUMN create_time SET NOT NULL;
ALTER TABLE school_classes_0 ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE school_classes_1 ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE school_class_users_0 ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE school_class_users_1 ALTER COLUMN update_time SET NOT NULL;
ALTER TABLE school_classes_0 ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE school_classes_1 ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE school_class_users_0 ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE school_class_users_1 ALTER COLUMN is_deleted SET NOT NULL;

CREATE UNIQUE INDEX uk_school_classes_0_tenant_grade_name_active
    ON school_classes_0 (tenant_id, grade_id, name, is_deleted);
CREATE UNIQUE INDEX uk_school_classes_1_tenant_grade_name_active
    ON school_classes_1 (tenant_id, grade_id, name, is_deleted);
ALTER TABLE school_classes_0 ADD CONSTRAINT uk_school_classes_0_tenant_grade_id_active
    UNIQUE (tenant_id, grade_id, id);
ALTER TABLE school_classes_1 ADD CONSTRAINT uk_school_classes_1_tenant_grade_id_active
    UNIQUE (tenant_id, grade_id, id);
CREATE UNIQUE INDEX uk_school_class_users_0_tenant_pair_active
    ON school_class_users_0 (tenant_id, grade_id, school_class_id, user_id, is_deleted);
CREATE UNIQUE INDEX uk_school_class_users_1_tenant_pair_active
    ON school_class_users_1 (tenant_id, grade_id, school_class_id, user_id, is_deleted);
CREATE INDEX idx_school_classes_0_tenant_grade ON school_classes_0 (tenant_id, grade_id, is_deleted);
CREATE INDEX idx_school_classes_1_tenant_grade ON school_classes_1 (tenant_id, grade_id, is_deleted);
CREATE INDEX idx_school_class_users_0_tenant_grade_class
    ON school_class_users_0 (tenant_id, grade_id, school_class_id);
CREATE INDEX idx_school_class_users_1_tenant_grade_class
    ON school_class_users_1 (tenant_id, grade_id, school_class_id);

ALTER TABLE school_class_users_0
    ADD CONSTRAINT fk_school_class_users_0_class
    FOREIGN KEY (tenant_id, grade_id, school_class_id)
    REFERENCES school_classes_0 (tenant_id, grade_id, id);
ALTER TABLE school_class_users_1
    ADD CONSTRAINT fk_school_class_users_1_class
    FOREIGN KEY (tenant_id, grade_id, school_class_id)
    REFERENCES school_classes_1 (tenant_id, grade_id, id);
