# Evaluation manual schema

> 历史脚本档案：本目录不是当前启动 DDL 入口。当前规范统一采用 MP-SDJ Starter 分布式受管 DDL、版本化 SQL 与 SHA-256 Manifest；下文手工步骤仅说明历史交付物，不作为新部署或升级流程。不要修改已应用脚本或历史校验和。

当前入口是 `../../egon-mp/V20260913_001__initialize_repository_schema.sql` 与
`repository-manifest.json`，由 `EgonColaPostgreDdlRunner` 在显式调用时执行；生成的工程不会自动调用它，
所以两条路径都要人工掌握。受管 schema 使用 `deleted_at` 与 `version`，而本目录的 003/004 只补 `is_deleted`，
两者结构不互通：不要把本目录脚本当作受管 schema 的前置或替代。

These scripts are operator-owned PostgreSQL DDL. The application never executes them,
does not create a migration history table, and starts only after the target databases
have been prepared.

The managed entry point is `../../egon-mp/V20260913_001__initialize_repository_schema.sql` with
`repository-manifest.json`, applied by `EgonColaPostgreDdlRunner` only when something calls it;
no generated project does. That managed schema uses `deleted_at` and `version`, while 003/004 here
only add `is_deleted`, so the two layouts are not interchangeable and these scripts are neither a
prerequisite nor a substitute for the managed schema.

Apply the archived scripts in this order:

1. `master-data/001__create_evaluation_master_data_schema.sql` on the `master_data`
   primary.
2. `shard/002__create_evaluation_sharded_schema.sql` on every `shard_N` primary.
3. `master-data/003__migrate_evaluation_master_data_to_egon_model.sql` on the
   `master_data` primary after reviewing the preflight and tenant mapping.
4. `shard/004__migrate_evaluation_sharded_to_tenant_model.sql` with the same bytes on
   every `shard_N` primary after reviewing the preflight and tenant mapping.
5. Verify that the five logical tables (`evaluation_course`,
   `evaluation_course_schedule`, `evaluation_exam`, `evaluation_exam_paper`,
   `evaluation_score`) and every physical suffix are present, all ID/reference columns
   are `BIGINT`, every routed table has positive `tenant_id`, and the constraint/index
   names match the scripts before enabling traffic.

Run the scripts with the database owner's reviewed change process. Replicas receive
the schema through the database replication or replica provisioning workflow; they
are not application migration targets. On a partial failure, stop rollout, restore
the affected database from the operator backup, and apply a reviewed forward fix.
Never point Spring SQL initialization, Flyway, Liquibase, or another migration tool at
this directory.
