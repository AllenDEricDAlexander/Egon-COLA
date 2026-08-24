# Web Open 手工建库手册

这些 SQL 是 DBA 交付物。应用启动、Spring SQL 初始化、MyBatis-Plus 和 Maven 都不会读取或执行它们。

## 执行顺序

1. 备份 `master_data` 与每个 shard primary，并记录两个脚本的 SHA-256、目标库和执行时间。
2. 在每个 shard primary 上执行 `mysql-shard.sql`，确保 `school_classes_0/_1` 与
   `school_class_users_0/_1` 的列、约束和索引完全一致。
3. 在 `master_data` primary 上执行 `mysql-master.sql`，再按需确认保留的示例角色/权限 ID `1/2`。
4. 逐库检查所有 ID、外键和路由键为 `BIGINT`，并执行行数、唯一约束和索引校验。
5. 只有 schema 校验、备份记录和发布审批完成后，才允许启用应用实例。

## 失败与回退边界

- 执行任一语句失败即停止后续步骤；应用不会重试 DDL，也不会创建表或修改数据。
- `CREATE TABLE IF NOT EXISTS` 仅支持重复执行建表检查，不会覆盖既有列、约束或数据。
- 回退由 DBA 根据备份恢复，或经评审提交新的前向 SQL；回退应用版本不会自动逆向数据库结构。
- Never point Spring SQL initialization or a migration framework at this directory.
