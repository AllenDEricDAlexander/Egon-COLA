# Gateway GWS-02A DDC Runtime Extension Implementation Plan

> **For Codex:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 Gateway 补齐 DDC 运行时所需的 `HTTP_PROVIDER` 服务租约、可冻结的 Config Applier Registry、严格版本/checksum 语义和独立周期校准。

**Architecture:** 保持 DDC 是配置与服务注册 Component，不下沉 Gateway 路由能力。服务类型扩展复用现有 Redis Registry；配置 Apply 使用 exact/longest-prefix/fallback 责任链并在 Spring 单例初始化后冻结；发布消息与周期拉取共用 `DdcRefreshService.applySnapshot` 的原子版本逻辑；校准使用独立调度线程，避免阻塞租约心跳。

**Tech Stack:** Java 21、Spring Boot 3.5.x、DDC Starter、JUnit 5、AssertJ、Mockito。

---

## 全局约束

- 工作目录：`/Users/mario/SelfProject/Egon-COLA/.worktrees/gateway-wave-0-foundation`。
- 分支：`codex/gateway-wave-0-foundation`。
- 不修改 DDC Admin 数据库或现有 Flyway Migration。
- 不实现 Gateway Provider Runtime、路由、负载均衡或管理 API。
- 每个任务一个提交，行为改动严格 RED → GREEN → REFACTOR。
- 保持现有 `@DdcValue` 无自定义 Applier 时的字段绑定行为。

### 设计模式判断

- 使用 **Chain of Responsibility**：exact → longest prefix → field-binding fallback 是明确优先链。
- 使用 **Registry**：启动阶段收集 Key/Prefix Applier，冻结后只读，避免运行期实现漂移。
- 周期校准不使用 Observer 替代 Pub/Sub；它是补偿机制，与现有消息监听并存。
- 不使用 Strategy Factory 或 Service Locator；Applier 由调用方显式注册。

## Task 1: 扩展 HTTP Provider 服务类型和协议约束

**Files:**

- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/enums/DdcServiceKind.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/enums/DdcLeaseRole.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/registry/DdcServiceKey.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/registry/DdcServiceRegistration.java`
- Test: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/model/registry/DdcHttpProviderRegistrationTest.java`

**Steps:**

1. 先写测试覆盖 HTTP/HTTPS 注册、默认 group/version、secure 一致性、非法协议拒绝、canonical key round-trip。
2. 运行 `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-dynamic-config-center-starter -am -Dtest=DdcHttpProviderRegistrationTest -Dsurefire.failIfNoSpecifiedTests=false test`，确认因 `HTTP_PROVIDER` 不存在而失败。
3. 增加同名 `DdcServiceKind`/`DdcLeaseRole`，仅对 HTTP Provider 强制 protocol=`http|https` 且 secure 与协议一致。
4. 运行 DDC Starter 全量测试。
5. 提交：`feat(ddc): add HTTP provider service leases`。

## Task 2: 实现可冻结 Config Applier Registry

**Files:**

- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/service/DdcConfigApplierRegistry.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/service/DefaultDdcConfigApplierRegistry.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/config/DdcAutoConfig.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/service/DdcRefreshService.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/config/DdcAutoConfigTest.java`
- Test: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/service/DefaultDdcConfigApplierRegistryTest.java`

**Steps:**

1. 写失败测试：exact 优先、最长前缀、fallback、重复注册拒绝、非法前缀拒绝、freeze 后拒绝注册。
2. 运行聚焦测试确认 RED。
3. 实现接口和默认 Registry；前缀必须以 `.` 结尾，freeze 生成不可变快照。
4. DdcAutoConfig 暴露 Registry Bean，以字段绑定为 fallback，并用 `SmartInitializingSingleton` 在所有单例建立后冻结。
5. DdcRefreshService 通过 Registry resolve Applier；保留旧构造器以兼容现有调用方。
6. 更新 AutoConfig 测试，验证 Registry 存在且 Context 启动后 frozen。
7. 运行 DDC Starter 全量测试。
8. 提交：`feat(ddc): add composable config applier registry`。

## Task 3: 固化版本、checksum 和失败 ACK 语义

**Files:**

- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/service/DdcRefreshService.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/service/DdcRefreshServiceTest.java`

**Steps:**

1. 添加失败测试：
   - 相同版本、相同 checksum 幂等；
   - 相同版本、不同 checksum 返回 FAILED 且不 Apply；
   - 自定义 Apply 失败时 version/checksum 保持旧值；
   - ACK error 截断、去换行且不包含异常类型；
   - snapshot 同版本 checksum 不一致保持 LKG。
2. 运行聚焦测试确认至少一个新断言失败。
3. 统一 topic/snapshot 的版本比较方法；只有 Apply 成功后更新 version/checksum。
4. 对失败 ACK 输出固定前缀和最长 256 字符的单行安全信息。
5. 运行 DDC Starter 全量测试。
6. 提交：`fix(ddc): enforce config version checksum invariants`。

## Task 4: 增加独立周期配置校准

**Files:**

- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/config/DdcProperties.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/service/DdcRuntimeCoordinator.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/service/DdcRuntimeCoordinatorTest.java`

**Steps:**

1. 写失败测试：默认开启/30 秒、禁用时不拉取、`reconcileOnce` 应用更高版本、拉取失败保留 READY 和本地值、非法间隔启动失败。
2. 运行聚焦测试确认 RED。
3. 在 `DdcProperties.Consistency` 增加配置字段。
4. Coordinator 使用独立 `egon-cola-ddc-config-reconcile` scheduler；心跳 scheduler 保持原线程。
5. 校准调用 `pull()` 和现有 `applySnapshot`；异常只记录并等待下一周期，不清空本地状态、不伪造 ACK。
6. stop 时同时有界关闭两个 scheduler。
7. 运行 DDC Starter 与 DDC 完整 reactor 测试。
8. 提交：`feat(ddc): reconcile runtime config periodically`。

## Task 5: GWS-02A 验收

1. 运行：

```bash
./mvnw -B -ntp -f egon-cola-components/egon-cola-component-dynamic-config-center/pom.xml clean test
./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-rpc-test-suite -am test
git diff --check
git status --short
```

2. 预期 DDC 完整 reactor 与 RPC 回归全部成功，工作树干净。
3. 不创建空验收提交。

## 后续计划边界

- GWS-02B：DDC HMAC Management OpenAPI/Client、删除、容量保护。
- GWS-02C：RPC Contract Catalog/Snapshot、Provider Metadata Contributor。
- 三个计划全部完成后才宣称 GWS-02 完成。
