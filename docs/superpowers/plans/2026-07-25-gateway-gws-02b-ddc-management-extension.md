# Gateway GWS-02B DDC Management Extension Implementation Plan

> **For Codex:** REQUIRED SUB-SKILL: Use superpowers:executing-plans and
> superpowers:test-driven-development to implement this plan task-by-task.

**Goal:** 为 Gateway Admin 提供不依赖 DDC Starter/JPA/Redisson 的稳定 Management
Client，并在 DDC Admin 中实现 HMAC 管理 OpenAPI、幂等删除和 UTF-8 配置容量保护。

**Architecture:** 新增独立 `dynamic-config-center-management-client` 公共模块，承载
稳定 DTO、HMAC canonical request、`RestClient` Adapter 和错误映射。DDC Admin 通过
Management Facade 把机器契约映射到现有 Config/Publish/Lease/Registry 服务，绝不返回
Entity。配置写入继续以数据库为事实源，发布继续复用现有同步 Target ACK 流程。

**Tech Stack:** Java 21、Spring Boot 3.5.x、Spring `RestClient`、Jackson、HMAC
SHA-256、JUnit 5、AssertJ、MockMvc、Mockito。

---

## 全局约束

- 工作目录：
  `/Users/mario/SelfProject/Egon-COLA/.worktrees/gateway-wave-0-foundation`。
- 分支：`codex/gateway-wave-0-foundation`。
- 不修改任何已有 Flyway Migration；本计划不需要数据库字段。
- 不让 Management Client 依赖 DDC Starter、Redisson、JPA 或 Admin。
- 不实现 Gateway Release 引用判断；GWS-09 在调用删除前证明未被 Active/Retryable
  Release 引用，DDC 只负责 expectedVersion、精确 Key、幂等与审计。
- Management OpenAPI 位于 `/api/v1/ddc/openapi/management`，继续受现有 HMAC
  Filter 保护。
- 每个任务独立 RED → GREEN → REFACTOR 并提交。

### 设计模式判断

- 使用 **Facade** 隔离稳定机器 DTO 与现有 Admin Entity/Service。
- 使用 **Adapter** 封装 `RestClient`、签名 Header 和 Result Envelope。
- 使用 **Specification/Guard** 集中执行 UTF-8 容量规则，避免 Create/Update/Publish
  三处漂移。
- 不引入 Repository Pattern 包装层；现有 Admin Service 已经承担事务边界。

## Task 1: 新增 Management Client 模块和稳定 DTO

**Files:**

- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/pom.xml`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/pom.xml`
- Modify: `egon-cola-components/egon-cola-components-bom/pom.xml`
- Create: `egon-cola-component-dynamic-config-center-management-client/pom.xml`
- Move: starter 中 `DdcCanonicalRequest`、`DdcRequestSigner` 到 management-client，
  保持 Java package 和二进制类名不变。
- Create: management DTO、query、status、exception 与
  `DdcManagementClient`。
- Test: `DdcManagementContractBoundaryTest`
- Test: `DdcManagementDtoSerializationTest`

**Behavior:**

1. Client 模块只允许 common core/result/crypto、Spring Web、Jackson。
2. 稳定 DTO 不引用 `admin.*`、Entity、Repository、Redisson。
3. Publish Task 包含任务时间和完整 Target ACK 投影。
4. Service Catalog/Instance 使用 management 自有快照 DTO，不泄漏 Redis Key。
5. BOM 同时导出 DDC Starter 和 Management Client。

**Verification:**

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl :egon-cola-component-dynamic-config-center-management-client -am test
```

**Commit:** `build(ddc): add management client contracts`

## Task 2: 实现带 HMAC 的 RestClient Adapter

**Files:**

- Create: `DdcManagementClientProperties`
- Create: `HttpDdcManagementClient`
- Create: `DdcManagementRequestFactory`
- Create: `DdcManagementClientException`
- Test: `HttpDdcManagementClientTest`
- Test: `DdcManagementRequestFactoryTest`

**Behavior:**

1. 每次请求生成 timestamp、唯一 nonce、body SHA-256 和签名。
2. Canonical query 排序、URL encoding 与 Admin Filter 完全一致。
3. 连接/读取超时必须为正；endpoint、accessKey、secretKey 必填。
4. 非 2xx、Result failure、空 data 和反序列化失败映射为稳定 Client Exception。
5. 日志和异常不包含 secret、完整 Config Value 或签名。
6. DELETE 使用请求体携带 expectedVersion、operator、reason，签名覆盖请求体。

**Commit:** `feat(ddc): add signed management client adapter`

## Task 3: 实现 Management Facade 与机器 OpenAPI

**Files:**

- Create: Admin `DdcManagementFacade`
- Create: Admin `DdcManagementOpenApiController`
- Modify: `DdcConfigService`
- Modify: `DdcPublishService`
- Modify: `DdcInstanceAdminService`
- Modify: repository query methods where required
- Test: `DdcManagementFacadeTest`
- Test: `DdcManagementOpenApiControllerTest`

**Behavior:**

1. Upsert 以 scope + configKey 定位，创建或按 expectedVersion 更新 Draft。
2. Delete 仅接受精确 Key，拒绝 `*`/前缀语法；不存在或已删除时幂等成功。
3. Delete 版本变化时拒绝，实际删除时写 Version 和 Operation Log，不发布空值。
4. Publish 复用 `SYNC_ALL_ACK`，结果与 Task API 均包含固化 Target。
5. Retry 复用原 Target 和 lease 校验。
6. Config Client、Service Catalog、Service Instance 全部映射为稳定 DTO。
7. Controller 返回 `ResultDto<Management DTO>`，不注入 Repository。
8. `/openapi/management/**` 必须经过已有 HMAC Filter。

**Commit:** `feat(ddc): expose management machine openapi`

## Task 4: 增加配置值容量保护

**Files:**

- Modify: `DdcAdminProperties`
- Modify: `application.yml`
- Create: `DdcConfigValueGuard`
- Modify: `DdcConfigService`
- Modify: `DdcPublishService`
- Test: `DdcConfigValueGuardTest`
- Modify: Config/Publish service tests

**Behavior:**

1. 默认 `max-value-bytes=1048576`。
2. 按 UTF-8 字节检查，`null` 视为 0 字节。
3. Create、Update、Upsert、Publish 在数据库/Redis mutation 前执行同一 Guard。
4. 上限必须大于 0，非法配置启动失败。
5. 超限错误不回显 Config Value。

**Commit:** `feat(ddc): enforce config value capacity`

## Task 5: GWS-02B 边界与回归验收

1. 验证所有 Management API 签名成功、缺失、错误、过期、Nonce 重放。
2. 验证 DTO 源码和字节码不引用 Entity/Repository/Redisson。
3. 验证删除幂等、expectedVersion、审计、无隐式发布。
4. 验证 Admin 与 Test 完整 reactor：

```bash
./mvnw -B -ntp \
  -f egon-cola-components/egon-cola-component-dynamic-config-center/pom.xml \
  clean test
./mvnw -B -ntp \
  -f egon-cola-components/egon-cola-components-bom/pom.xml \
  clean verify
git diff --check
git status --short
```

5. 不创建空验收提交。

## 后续边界

- GWS-02C 实现 RPC Contract Catalog/Snapshot/Metadata Contributor。
- GWS-09 才实现 Gateway Release 引用证明与 Active Key 删除编排。
