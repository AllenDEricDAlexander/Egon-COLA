# Light Open 手工建库手册

这些脚本属于数据库运维交付物，应用启动、Maven 构建和 MyBatis-Plus 均不会执行它们。
Light Open 是新 schema，采用显式 Snowflake `BIGINT` 主键；不会尝试转换旧模板的字符串主键数据。

## 执行顺序与目标

1. 在 `master_data` primary 上执行
   `master-data/001__create_light_master_data_schema.sql`。
2. 在每个 shard primary（`shard_0`、`shard_1`）上分别执行
   `shard/002__create_light_sharded_schema.sql`。
3. 先备份并记录脚本 SHA-256，再使用与应用相同的数据库角色执行；replica 只通过数据库复制获得结构。

示例（每个 primary 单独执行）：

```bash
psql "$MASTER_DATA_URL" --set ON_ERROR_STOP=1 \
  --file master-data/001__create_light_master_data_schema.sql
psql "$SHARD_0_URL" --set ON_ERROR_STOP=1 \
  --file shard/002__create_light_sharded_schema.sql
psql "$SHARD_1_URL" --set ON_ERROR_STOP=1 \
  --file shard/002__create_light_sharded_schema.sql
```

## 预检查、校验与失败处理

- 确认连接用户拥有目标 schema 的建表、约束和索引权限，并确认三个 primary 的备份可恢复。
- 记录 `sha256sum master-data/001__create_light_master_data_schema.sql shard/002__create_light_sharded_schema.sql`；该 checksum 随发布单保存脚本版本、目标库和执行时间。
- 校验 `users/courses` 的 ID 类型为 `bigint`，两个 shard 均存在 `school_classes_0/_1` 与
  `class_course_schedules_0/_1`，并比较两个 shard 的列、主键、唯一约束和本地外键定义。
- verification 结果、脚本 checksum 和数据库目标必须随发布单归档。
- 任一脚本失败立即停止后续库；恢复备份或提交新的前向修复脚本，禁止由应用重试 DDL。
- 应用在 schema 未准备好时应保持不可用/读写失败；这属于运维前置条件，不是自动修复触发点。

## 回退边界

脚本是一次性初始建库合同，不提供在线删除表回退。rollback 由 DBA 根据备份恢复，或在评审后执行新的前向 SQL；回退应用版本
不会自动修改已经执行的数据库结构。
