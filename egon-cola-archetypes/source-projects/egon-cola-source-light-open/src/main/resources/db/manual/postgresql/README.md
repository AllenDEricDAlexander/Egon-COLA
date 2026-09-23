# Light Open 手工建库与迁移手册

> 历史脚本档案：本目录不是当前启动 DDL 入口。当前规范统一采用 MP-SDJ Starter 分布式受管 DDL、版本化 SQL 与 SHA-256 Manifest；下文手工步骤仅说明历史交付物，不作为新部署或升级流程。不要修改已应用脚本或历史校验和。

当前入口是 `../../egon-mp/V20260913_001__initialize_repository_schema.sql` 与
`repository-manifest.json`，由 `EgonColaPostgreDdlRunner` 在显式调用时执行；生成的工程不会自动调用它，
所以两条路径都要人工掌握。受管 schema 使用 `deleted_at` 与 `version`，而本目录的 003/004 只补 `is_deleted`，
两者结构不互通：不要把本目录脚本当作受管 schema 的前置或替代。

这些脚本是数据库运维交付物；应用启动、Maven 构建和 MyBatis-Plus 均不会执行它们，Open profile 不引入 Flyway。
逻辑表名使用 `light_*`，实际 master/shard 物理表使用 `light_users`、`light_school_classes_0` 等名称，由 ShardingSphere 负责映射。


1. 在 `master_data` primary 上执行 `master-data/001__create_light_master_data_schema.sql`，再执行 `master-data/003__migrate_light_master_data_to_egon_model.sql`。
2. 在每个 shard primary（`shard_0`、`shard_1`）上分别执行 `shard/002__create_light_sharded_schema.sql`，再执行 `shard/004__migrate_light_sharded_to_tenant_model.sql`。
3. 每个物理库执行前先备份、核对脚本 checksum 和 predecessor 状态；replica 只通过数据库复制获得结构。

$MASTER_DATA_URL、$SHARD_0_URL、$SHARD_1_URL 是运维自备的 shell 变量，
本工程的 POM、YAML 或 `deploy/env/*` 都不定义它们；执行前先按目标 primary 导出。

```bash
psql "$MASTER_DATA_URL" --set ON_ERROR_STOP=1 \
  --file master-data/001__create_light_master_data_schema.sql
psql "$MASTER_DATA_URL" --set ON_ERROR_STOP=1 \
  --file master-data/003__migrate_light_master_data_to_egon_model.sql
psql "$SHARD_0_URL" --set ON_ERROR_STOP=1 \
  --file shard/002__create_light_sharded_schema.sql
psql "$SHARD_0_URL" --set ON_ERROR_STOP=1 \
  --file shard/004__migrate_light_sharded_to_tenant_model.sql
psql "$SHARD_1_URL" --set ON_ERROR_STOP=1 \
  --file shard/002__create_light_sharded_schema.sql
psql "$SHARD_1_URL" --set ON_ERROR_STOP=1 \
  --file shard/004__migrate_light_sharded_to_tenant_model.sql
```


- 003/004 会在发现 predecessor 中已有历史行时 fail-fast；不要猜测 tenant 映射或在线转换未知身份。
- 确认连接用户有目标 schema 的 DDL、约束和索引权限，并确认三个 primary 的备份可恢复。
- 记录 `sha256sum` 覆盖 001、002、003、004；校验所有表存在 `id/tenant_id/create_user_id/create_time/update_user_id/update_time/is_deleted`。
- 校验两个 shard 的 suffix、列、约束、唯一索引和 tenant 路由定义完全一致；应用使用正数 `tenant_id`。
- 将 verification 结果、目标库和脚本 checksum 随发布单归档。
- 任一脚本失败立即停止后续库，恢复备份或提交新的前向 SQL；禁止应用自动重试 DDL。


脚本不提供在线删除表回退（rollback）。迁移执行后只能由 DBA 按备份恢复，或经评审执行新的前向修复 SQL；回退应用版本不会自动修改已执行结构。
