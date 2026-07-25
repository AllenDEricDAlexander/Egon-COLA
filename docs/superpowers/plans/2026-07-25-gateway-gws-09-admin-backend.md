# Gateway GWS-09 Admin 后端实现计划

**Goal:** 建立可独立启动、可持久化、可恢复发布的 Gateway Admin 控制面，承载三级
接口目录、Draft、Release、审计与运行投影，且不进入业务请求热路径。

**Architecture:** 使用 Spring Boot MVC、Bean Validation、Spring Data JPA、Flyway
和 PostgreSQL。接口层只调用 Application Service；Domain 保存状态机和一致性约束；
Infrastructure 负责 JPA、DDC Management Client、时钟与投影。网络发布严格发生在
本地事务提交之后。

## 设计模式判断

- Aggregate：`GatewayGroup`、`GatewayDraft`、`GatewayRelease` 固化各自不变量。
- State：Release 和 Operation 生命周期拒绝非法跳转。
- Repository：隔离领域用例与 JPA。
- Application Service：编排事务、审计、幂等和 DDC 调用。
- Adapter：DDC Management Client 是唯一规则发布出口。
- 不引入通用工作流引擎；状态数量固定，显式状态机更容易审计和恢复。

## Task 1: 数据库与持久化骨架

- 为 Gateway Admin 新增唯一 `V1__create_gateway_admin_schema.sql`。
- 建立 Group、Application、三级目录、Operation/Definition、Draft、Release/
  Attempt/Target、Credential/Nonce、Audit 表及必要唯一索引。
- JSONB 只保存 Schema、Descriptor、Canonical Snapshot 和受约束 Metadata。
- 增加 Spring MVC/JPA/Flyway/PostgreSQL 依赖与 Admin Application。

**Commit:** `feat(gateway): persist admin control plane model`

## Task 2: Group、目录与 Operation 用例

- Group 的固定坐标、启停、Revision 和逻辑删除约束。
- Application 和 Business/Entity/Interface Group 三级目录。
- Operation Key 唯一、Definition 不可变版本、MANUAL/STARTER 来源隔离。
- `externalAccessible` 默认 false，false→true 写入重要审计。

**Commit:** `feat(gateway): manage interface catalog`

## Task 3: Draft 乐观锁与幂等

- 每个 Group 一个 Draft；Route/Policy 子资源结构化持久化。
- 所有写操作校验 `expectedRevision`，成功后只递增一次 Revision。
- `Idempotency-Key + payloadSha256` 相同重放原结果，不同 Payload 返回冲突。
- 提供 validate/diff 查询。

**Commit:** `feat(gateway): manage revisioned gateway drafts`

## Task 4: Release 状态机和 DDC 编排

- 基于指定 Draft Revision 编译不可变 Snapshot 并保存 Release Content。
- 本地事务提交后调用 `GatewayDdcRulePublisher`。
- Attempt/Target 固化；重试不重新编译 Draft；回滚复制历史内容创建新 Release。
- 非终态恢复使用数据库 CAS/租约，不使用 JVM 单例正确性。

**Commit:** `feat(gateway): orchestrate recoverable releases`

## Task 5: 管理 API、错误与投影

- 实现 `/api/v1/gateway/admin` 下的 Group、Application、Catalog、Draft、Release API。
- 统一 409/422/503 错误结构，不暴露 JPA/DDC 异常。
- Engine/Provider 投影响应带 `observedAt/source/stale`，不作为 Engine 寻址源。
- 重要操作落审计，快照只记录 SHA/大小/Release 引用。

**Commit:** `feat(gateway): expose admin management api`

## Task 6: 验收

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl :egon-cola-component-gateway-admin -am clean test
```

检查只有一个新 Gateway Admin Migration；Admin 不依赖 DDC Redis/Repository；发布网络
调用不位于数据库事务中；Revision、Idempotency、Release 非法跃迁和审计脱敏均有测试。
