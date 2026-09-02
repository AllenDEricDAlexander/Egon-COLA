-- Manual operator migration: 将 Evaluation 排课、考试、试卷和成绩分表迁移到 BIGINT、tenant_id 和 EgonModel 七项技术字段。
-- Scope: 每个 shard 库的 course_schedule_0/1、exam_0/1、exam_paper_0/1、score_0/1。
-- Compatibility: 旧表中的历史行必须先完成明确的 Long/tenant 离线映射；不可转换或未知数据会使本迁移失败。

-- 脚手架只接受空的旧分片表；未知历史身份/租户归属不得由自动迁移工具猜测。
SELECT 1 / CASE WHEN EXISTS (SELECT 1 FROM course_schedule_0)
    OR EXISTS (SELECT 1 FROM course_schedule_1)
    OR EXISTS (SELECT 1 FROM exam_0)
    OR EXISTS (SELECT 1 FROM exam_1)
    OR EXISTS (SELECT 1 FROM exam_paper_0)
    OR EXISTS (SELECT 1 FROM exam_paper_1)
    OR EXISTS (SELECT 1 FROM score_0)
    OR EXISTS (SELECT 1 FROM score_1)
    THEN 0 ELSE 1 END;

ALTER TABLE course_schedule_0 ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE course_schedule_1 ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE course_schedule_0 ALTER COLUMN course_id SET DATA TYPE BIGINT;
ALTER TABLE course_schedule_1 ALTER COLUMN course_id SET DATA TYPE BIGINT;
ALTER TABLE course_schedule_0 ALTER COLUMN class_id SET DATA TYPE BIGINT;
ALTER TABLE course_schedule_1 ALTER COLUMN class_id SET DATA TYPE BIGINT;
ALTER TABLE exam_0 ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE exam_1 ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE exam_0 ALTER COLUMN course_id SET DATA TYPE BIGINT;
ALTER TABLE exam_1 ALTER COLUMN course_id SET DATA TYPE BIGINT;
ALTER TABLE exam_paper_0 ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE exam_paper_1 ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE exam_paper_0 ALTER COLUMN exam_id SET DATA TYPE BIGINT;
ALTER TABLE exam_paper_1 ALTER COLUMN exam_id SET DATA TYPE BIGINT;
ALTER TABLE score_0 ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE score_1 ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE score_0 ALTER COLUMN exam_id SET DATA TYPE BIGINT;
ALTER TABLE score_1 ALTER COLUMN exam_id SET DATA TYPE BIGINT;
ALTER TABLE score_0 ALTER COLUMN course_id SET DATA TYPE BIGINT;
ALTER TABLE score_1 ALTER COLUMN course_id SET DATA TYPE BIGINT;
ALTER TABLE score_0 ALTER COLUMN student_id SET DATA TYPE BIGINT;
ALTER TABLE score_1 ALTER COLUMN student_id SET DATA TYPE BIGINT;

ALTER TABLE course_schedule_0 RENAME COLUMN created_at TO create_time;
ALTER TABLE course_schedule_1 RENAME COLUMN created_at TO create_time;
ALTER TABLE course_schedule_0 RENAME COLUMN updated_at TO update_time;
ALTER TABLE course_schedule_1 RENAME COLUMN updated_at TO update_time;
ALTER TABLE exam_0 RENAME COLUMN created_at TO create_time;
ALTER TABLE exam_1 RENAME COLUMN created_at TO create_time;
ALTER TABLE exam_0 RENAME COLUMN updated_at TO update_time;
ALTER TABLE exam_1 RENAME COLUMN updated_at TO update_time;
ALTER TABLE exam_paper_0 RENAME COLUMN created_at TO create_time;
ALTER TABLE exam_paper_1 RENAME COLUMN created_at TO create_time;
ALTER TABLE exam_paper_0 RENAME COLUMN updated_at TO update_time;
ALTER TABLE exam_paper_1 RENAME COLUMN updated_at TO update_time;
ALTER TABLE score_0 RENAME COLUMN created_at TO create_time;
ALTER TABLE score_1 RENAME COLUMN created_at TO create_time;
ALTER TABLE score_0 RENAME COLUMN updated_at TO update_time;
ALTER TABLE score_1 RENAME COLUMN updated_at TO update_time;

ALTER TABLE course_schedule_0 RENAME TO evaluation_course_schedule_0;
ALTER TABLE course_schedule_1 RENAME TO evaluation_course_schedule_1;
ALTER TABLE exam_0 RENAME TO evaluation_exam_0;
ALTER TABLE exam_1 RENAME TO evaluation_exam_1;
ALTER TABLE exam_paper_0 RENAME TO evaluation_exam_paper_0;
ALTER TABLE exam_paper_1 RENAME TO evaluation_exam_paper_1;
ALTER TABLE score_0 RENAME TO evaluation_score_0;
ALTER TABLE score_1 RENAME TO evaluation_score_1;

ALTER TABLE evaluation_course_schedule_0 ADD COLUMN tenant_id BIGINT DEFAULT 1;
ALTER TABLE evaluation_course_schedule_1 ADD COLUMN tenant_id BIGINT DEFAULT 1;
ALTER TABLE evaluation_exam_0 ADD COLUMN tenant_id BIGINT DEFAULT 1;
ALTER TABLE evaluation_exam_1 ADD COLUMN tenant_id BIGINT DEFAULT 1;
ALTER TABLE evaluation_exam_paper_0 ADD COLUMN tenant_id BIGINT DEFAULT 1;
ALTER TABLE evaluation_exam_paper_1 ADD COLUMN tenant_id BIGINT DEFAULT 1;
ALTER TABLE evaluation_score_0 ADD COLUMN tenant_id BIGINT DEFAULT 1;
ALTER TABLE evaluation_score_1 ADD COLUMN tenant_id BIGINT DEFAULT 1;

ALTER TABLE evaluation_course_schedule_0 ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE evaluation_course_schedule_1 ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE evaluation_exam_0 ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE evaluation_exam_1 ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE evaluation_exam_paper_0 ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE evaluation_exam_paper_1 ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE evaluation_score_0 ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE evaluation_score_1 ADD COLUMN create_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE evaluation_course_schedule_0 ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE evaluation_course_schedule_1 ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE evaluation_exam_0 ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE evaluation_exam_1 ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE evaluation_exam_paper_0 ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE evaluation_exam_paper_1 ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE evaluation_score_0 ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE evaluation_score_1 ADD COLUMN update_user_id VARCHAR(128) DEFAULT 'migration';
ALTER TABLE evaluation_course_schedule_0 ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE evaluation_course_schedule_1 ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE evaluation_exam_0 ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE evaluation_exam_1 ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE evaluation_exam_paper_0 ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE evaluation_exam_paper_1 ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE evaluation_score_0 ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE evaluation_score_1 ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;

ALTER TABLE evaluation_course_schedule_0 ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE evaluation_course_schedule_1 ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE evaluation_exam_0 ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE evaluation_exam_1 ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE evaluation_exam_paper_0 ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE evaluation_exam_paper_1 ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE evaluation_score_0 ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE evaluation_score_1 ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE evaluation_exam_paper_0 DROP CONSTRAINT uk_exam_paper_0_exam;
ALTER TABLE evaluation_exam_paper_1 DROP CONSTRAINT uk_exam_paper_1_exam;
ALTER TABLE evaluation_score_0 DROP CONSTRAINT uk_score_0_exam_student;
ALTER TABLE evaluation_score_1 DROP CONSTRAINT uk_score_1_exam_student;
CREATE UNIQUE INDEX uk_evaluation_exam_paper_0_tenant_exam_active
    ON evaluation_exam_paper_0 (tenant_id, exam_id, is_deleted);
CREATE UNIQUE INDEX uk_evaluation_exam_paper_1_tenant_exam_active
    ON evaluation_exam_paper_1 (tenant_id, exam_id, is_deleted);
CREATE UNIQUE INDEX uk_evaluation_score_0_tenant_exam_student_active
    ON evaluation_score_0 (tenant_id, exam_id, student_id, is_deleted);
CREATE UNIQUE INDEX uk_evaluation_score_1_tenant_exam_student_active
    ON evaluation_score_1 (tenant_id, exam_id, student_id, is_deleted);
CREATE INDEX idx_evaluation_score_0_tenant_exam_active_create
    ON evaluation_score_0 (tenant_id, exam_id, is_deleted, create_time, id);
CREATE INDEX idx_evaluation_score_1_tenant_exam_active_create
    ON evaluation_score_1 (tenant_id, exam_id, is_deleted, create_time, id);
