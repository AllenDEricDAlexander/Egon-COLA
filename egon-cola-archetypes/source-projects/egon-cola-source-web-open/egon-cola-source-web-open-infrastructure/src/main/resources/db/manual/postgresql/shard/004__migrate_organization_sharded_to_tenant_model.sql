-- Web Open sharded-table migration to the Common MyBatis-Plus EgonModel contract.
-- Run manually on every shard primary after 002__create_organization_sharded_schema.sql.
-- This script is intentionally not loaded by Spring/Flyway. Stop on a partially migrated schema.

SELECT 1 / CASE WHEN
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_name = 'school_classes_0' AND column_name = 'created_at')
    AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                    WHERE table_name = 'school_classes_0' AND column_name = 'tenant_id')
    THEN 1 ELSE 0 END;

ALTER TABLE school_classes_0 RENAME COLUMN created_at TO create_time;
ALTER TABLE school_classes_1 RENAME COLUMN created_at TO create_time;
ALTER TABLE school_class_users_0 RENAME COLUMN created_at TO create_time;
ALTER TABLE school_class_users_1 RENAME COLUMN created_at TO create_time;

ALTER TABLE school_classes_0 DROP CONSTRAINT IF EXISTS uk_school_classes_0_grade_name;
ALTER TABLE school_classes_0 DROP CONSTRAINT IF EXISTS uk_school_classes_0_grade_id;
ALTER TABLE school_classes_1 DROP CONSTRAINT IF EXISTS uk_school_classes_1_grade_name;
ALTER TABLE school_classes_1 DROP CONSTRAINT IF EXISTS uk_school_classes_1_grade_id;
ALTER TABLE school_class_users_0 DROP CONSTRAINT IF EXISTS uk_school_class_users_0;
ALTER TABLE school_class_users_1 DROP CONSTRAINT IF EXISTS uk_school_class_users_1;

ALTER TABLE school_classes_0 ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE school_classes_1 ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE school_class_users_0 ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE school_class_users_1 ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE school_classes_0 ADD COLUMN create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration';
ALTER TABLE school_classes_1 ADD COLUMN create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration';
ALTER TABLE school_class_users_0 ADD COLUMN create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration';
ALTER TABLE school_class_users_1 ADD COLUMN create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration';
ALTER TABLE school_classes_0 ADD COLUMN update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration';
ALTER TABLE school_classes_1 ADD COLUMN update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration';
ALTER TABLE school_class_users_0 ADD COLUMN update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration';
ALTER TABLE school_class_users_1 ADD COLUMN update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration';
ALTER TABLE school_classes_0 ADD COLUMN update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE school_classes_1 ADD COLUMN update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE school_class_users_0 ADD COLUMN update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE school_class_users_1 ADD COLUMN update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE school_classes_0 ADD COLUMN is_deleted SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE school_classes_1 ADD COLUMN is_deleted SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE school_class_users_0 ADD COLUMN is_deleted SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE school_class_users_1 ADD COLUMN is_deleted SMALLINT NOT NULL DEFAULT 0;

ALTER TABLE school_classes_0 ADD CONSTRAINT uk_school_classes_0_tenant_grade_name_active
    UNIQUE (tenant_id, grade_id, name, is_deleted);
ALTER TABLE school_classes_1 ADD CONSTRAINT uk_school_classes_1_tenant_grade_name_active
    UNIQUE (tenant_id, grade_id, name, is_deleted);
ALTER TABLE school_classes_0 ADD CONSTRAINT uk_school_classes_0_tenant_grade_id_active
    UNIQUE (tenant_id, grade_id, id);
ALTER TABLE school_classes_1 ADD CONSTRAINT uk_school_classes_1_tenant_grade_id_active
    UNIQUE (tenant_id, grade_id, id);
ALTER TABLE school_class_users_0 ADD CONSTRAINT uk_school_class_users_0_tenant_pair_active
    UNIQUE (tenant_id, grade_id, school_class_id, user_id, is_deleted);
ALTER TABLE school_class_users_1 ADD CONSTRAINT uk_school_class_users_1_tenant_pair_active
    UNIQUE (tenant_id, grade_id, school_class_id, user_id, is_deleted);
CREATE INDEX idx_school_classes_0_tenant_grade
    ON school_classes_0 (tenant_id, grade_id, is_deleted);
CREATE INDEX idx_school_classes_1_tenant_grade
    ON school_classes_1 (tenant_id, grade_id, is_deleted);
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
