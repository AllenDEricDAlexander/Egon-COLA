# Gateway GWS-01 Foundation Implementation Plan

> **For Codex:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 建立 Gateway Component 的完整 Maven 工程边界，并实现后续 Engine、Admin、Starter、Provider Runtime 共用的版本、身份、错误、请求上下文和生命周期基础契约。

**Architecture:** Gateway 以独立大型 Component 聚合。`gateway-contract` 只承载跨进程稳定类型，`gateway-core` 只依赖 Java 标准库与 Contract；Engine/Admin/Starter/Provider Runtime 在本任务中只建立依赖边界，不提前实现后续子 Spec 的网络、存储或 Spring 运行时。公共模型使用不可变值对象，Engine 生命周期使用显式 State Machine，协议差异通过稳定枚举和 typed interfaces 表达。

**Tech Stack:** Java 21、Maven Reactor、JUnit 5、AssertJ；本任务不引入新的第三方运行时依赖。

---

## 全局约束

- 工作目录固定为 `/Users/mario/SelfProject/Egon-COLA/.worktrees/gateway-wave-0-foundation`。
- 分支固定为 `codex/gateway-wave-0-foundation`。
- 不启动 Engine、Admin 或测试应用。
- 不修改任何现有 Flyway Migration。
- 每个任务只产生一个提交；下一任务开始前工作树必须干净。
- 行为代码遵守 RED → GREEN → REFACTOR；Maven 纯结构调整先用 reactor 选择失败证明模块尚不存在，再添加最小 POM。
- 不在 GWS-01 中加入 Reactor Netty、gRPC Handler、DDC Client、Kafka、Redis、JPA 或管理页面实现。

### 设计模式判断

- 使用 **Value Object**：Operation Key、错误、结果和上下文诊断信息需要不可变及构造约束。
- 使用 **State**：Engine 生命周期存在明确合法迁移，直接散落条件判断会使后续 Listener/DDC/RPC Slot 各自解释状态。
- 使用 **Ports and Adapters 的依赖方向**：本任务通过 Maven 依赖边界和无框架 Core 固化，具体 Port/Adapter 留给后续 GWS。
- 暂不引入 Strategy、Chain of Responsibility、Builder/Compiler：这些变化点属于 GWS-03/GWS-06/GWS-07；当前引入只会产生空抽象。

## Task 1: 建立 Gateway Reactor、产品模块和 BOM 边界

**Files:**

- Modify: `egon-cola-components/pom.xml`
- Modify: `egon-cola-components/egon-cola-components-bom/pom.xml`
- Create: `egon-cola-components/egon-cola-component-gateway/pom.xml`
- Create: `egon-cola-components/egon-cola-component-gateway/README.md`
- Create: `egon-cola-components/egon-cola-component-gateway/README.zh-CN.md`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-contract/pom.xml`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-core/pom.xml`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-engine/pom.xml`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-admin/pom.xml`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-starter/pom.xml`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-provider-runtime/pom.xml`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-admin-web/README.md`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/pom.xml`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-http-provider/pom.xml`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-rpc-contract/pom.xml`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-rpc-provider/pom.xml`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-rpc-consumer/pom.xml`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite/pom.xml`

**Step 1: 证明模块尚不存在**

Run:

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-gateway-core -am test
```

Expected: FAIL，Maven 报 selected project 不存在。

**Step 2: 添加最小 Reactor 与依赖图**

- Gateway 根 POM 聚合 Contract、Core、Engine、Admin、Starter、Provider Runtime、Test；
- Admin Web 不进入 Maven modules；
- Core 只依赖 Contract；
- Engine 只依赖 Core；
- Admin、Starter、Provider Runtime 只依赖 Contract；
- Test 子模块分别依赖未来所需产品 Artifact，但不创建行为代码；
- Gateway 根 POM 统一管理内部 Artifact 版本。

**Step 3: 导出公共下游 Artifact**

BOM 只增加：

- `egon-cola-component-gateway-starter`
- `egon-cola-component-gateway-provider-runtime`

不得导出 Contract、Core、Engine、Admin 或 Test。

**Step 4: 验证 Reactor 与依赖图**

Run:

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-gateway-test-suite -am test
```

Expected: BUILD SUCCESS；Gateway 及所需依赖均能完成 reactor 构建。

Run:

```bash
./mvnw -B -ntp -f egon-cola-components/egon-cola-components-bom/pom.xml help:effective-pom -Doutput=target/effective-pom.xml
```

Expected: BUILD SUCCESS；人工检查 effective POM 仅含两个 Gateway 公共 Artifact。

**Step 5: Commit**

```bash
git add egon-cola-components/pom.xml \
  egon-cola-components/egon-cola-components-bom/pom.xml \
  egon-cola-components/egon-cola-component-gateway
git commit -m "build(gateway): add component module boundaries"
```

## Task 2: 实现版本、协议身份和 Operation Key 契约

**Files:**

- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/version/GatewayContractVersions.java`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/protocol/GatewayProtocol.java`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/protocol/AccessZone.java`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/identity/GatewayOperationKey.java`
- Test: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-contract/src/test/java/top/egon/cola/component/gateway/contract/identity/GatewayOperationKeyTest.java`
- Test: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-contract/src/test/java/top/egon/cola/component/gateway/contract/GatewayContractBoundaryTest.java`

**Step 1: 写失败的 Operation Key 测试**

覆盖：

- HTTP method 使用大写，path 保持大小写语义并规范前导 `/`；
- 显示名称等非协议信息不参与 key；
- HTTP method/path 变化会改变 key；
- RPC 使用 `applicationCode:rpc:serviceName:group:version:fullMethodName`；
- 空 application、method/path 或 RPC 身份字段必须拒绝。

**Step 2: 运行并确认 RED**

Run:

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl :egon-cola-component-gateway-contract -am \
  -Dtest=GatewayOperationKeyTest test
```

Expected: FAIL，类型尚不存在或行为尚未实现。

**Step 3: 实现最小不可变契约**

- `GatewayContractVersions` 分别声明 API、Event、Rule 的 V1，禁止用一个常量代替三种版本；
- `GatewayOperationKey` 是不可变值对象；
- HTTP path 只做 trim、补前导 `/`、合并连续 `/`，不改变大小写、不静默移除尾 `/`；
- RPC 字段 trim 后必须非空，不改变大小写；
- 错误消息不回显敏感值。

**Step 4: 添加 Contract 依赖边界测试**

扫描 Contract main source，拒绝 Spring、JPA、Netty、gRPC、Reactor、Redis、Jackson、
Lombok 和其他 Gateway 产品模块 import。

**Step 5: 运行 GREEN**

Run:

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl :egon-cola-component-gateway-contract -am test
```

Expected: BUILD SUCCESS。

**Step 6: Commit**

```bash
git add egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-contract
git commit -m "feat(gateway): add version and operation identity contracts"
```

## Task 3: 实现统一错误与执行结果模型

**Files:**

- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/error/GatewayErrorCategory.java`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/error/GatewayError.java`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/error/GatewayResult.java`
- Test: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-contract/src/test/java/top/egon/cola/component/gateway/contract/error/GatewayResultTest.java`

**Step 1: 写失败测试**

覆盖：

- 成功结果不能携带错误；
- 失败结果必须携带 `GatewayError`；
- Error details 防御性复制且不允许 null key/value；
- `INTERNAL_ERROR` 的公共 message 不从原始异常生成；
- Result 明确区分 success/failure，调用者无需解析 message。

**Step 2: 运行 RED**

Run:

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl :egon-cola-component-gateway-contract -am \
  -Dtest=GatewayResultTest test
```

Expected: FAIL。

**Step 3: 实现最小模型**

- `GatewayErrorCategory` 固化 GWS-01 的 11 个类别；
- `GatewayError` 使用不可变 `Map<String, String>`；
- 提供受控 `internal(traceId)` 工厂，固定外部 message；
- `GatewayResult.success()` 与 `GatewayResult.failure(error)` 保证不变量。

**Step 4: 运行 GREEN**

Run:

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl :egon-cola-component-gateway-contract -am test
```

Expected: BUILD SUCCESS。

**Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-contract
git commit -m "feat(gateway): add shared error result model"
```

## Task 4: 实现无框架 Gateway Core 请求与上下文契约

**Files:**

- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-core/src/main/java/top/egon/cola/component/gateway/core/exchange/GatewayHeaders.java`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-core/src/main/java/top/egon/cola/component/gateway/core/exchange/GatewayBody.java`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-core/src/main/java/top/egon/cola/component/gateway/core/exchange/GatewayRequest.java`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-core/src/main/java/top/egon/cola/component/gateway/core/exchange/GatewayResponse.java`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-core/src/main/java/top/egon/cola/component/gateway/core/exchange/GatewayExchange.java`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-core/src/main/java/top/egon/cola/component/gateway/core/context/GatewayContext.java`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-core/src/main/java/top/egon/cola/component/gateway/core/context/GatewayStage.java`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-core/src/main/java/top/egon/cola/component/gateway/core/context/GatewayPrincipal.java`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-core/src/main/java/top/egon/cola/component/gateway/core/context/GatewayProviderSelection.java`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-core/src/main/java/top/egon/cola/component/gateway/core/context/GatewayGovernanceDecision.java`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-core/src/main/java/top/egon/cola/component/gateway/core/context/GatewayDiagnostic.java`
- Test: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-core/src/test/java/top/egon/cola/component/gateway/core/GatewayCoreBoundaryTest.java`
- Test: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-core/src/test/java/top/egon/cola/component/gateway/core/context/GatewayContextTest.java`

**Step 1: 写失败测试**

覆盖：

- Context 必须包含 request/trace/access zone/engine scope/start/deadline/stage；
- governance decisions 与 diagnostics 防御性复制；
- Principal attributes 和 Provider metadata 防御性复制；
- deadline 早于 startedAt 时拒绝；
- Core source 不导入 Spring、JPA、Netty、gRPC、Reactor、Redis、Kafka、Jackson 或其他 Gateway 产品模块。

**Step 2: 运行 RED**

Run:

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl :egon-cola-component-gateway-core -am \
  -Dtest=GatewayContextTest,GatewayCoreBoundaryTest test
```

Expected: FAIL。

**Step 3: 实现 typed interfaces 和不可变 Context**

- `GatewayRequest/Response/Exchange` 仅引用 Core 与 Contract 类型；
- `GatewayBody` 只定义协议无关的长度与是否可重放语义，不锁定 byte[] 或 Reactor 类型；
- `GatewayHeaders` 暴露只读查询，不规定网络层实现；
- `GatewayContext` 使用明确字段，不提供无类型扩展 Map；
- 未选择 operation/route/provider 时用 nullable-free `Optional` 查询。

**Step 4: 运行 GREEN**

Run:

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl :egon-cola-component-gateway-core -am test
```

Expected: BUILD SUCCESS。

**Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-core
git commit -m "feat(gateway): add framework-free exchange context"
```

## Task 5: 实现 Engine 生命周期 State Machine

**Files:**

- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-core/src/main/java/top/egon/cola/component/gateway/core/lifecycle/GatewayEngineState.java`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-core/src/main/java/top/egon/cola/component/gateway/core/lifecycle/GatewayEngineLifecycle.java`
- Test: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-core/src/test/java/top/egon/cola/component/gateway/core/lifecycle/GatewayEngineLifecycleTest.java`

**Step 1: 写失败测试**

覆盖：

- 正常路径 `NEW → STARTING → SYNCING_RULES → READY → DRAINING → STOPPED`；
- `READY` 才接受新请求；
- `DEGRADED` 是 READY 的服务子状态，可回到 READY 或进入 DRAINING；
- 启动/同步/运行阶段可进入 FAILED，FAILED 不接受请求且只能进入 STOPPED；
- 非法跳转（如 NEW → READY、DRAINING → READY）被拒绝；
- 同状态重复设置幂等。

**Step 2: 运行 RED**

Run:

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl :egon-cola-component-gateway-core -am \
  -Dtest=GatewayEngineLifecycleTest test
```

Expected: FAIL。

**Step 3: 实现 State Machine**

- 使用 `AtomicReference` 保证并发迁移原子性；
- 合法迁移集中在不可变映射；
- `acceptingRequests()` 仅在 READY/DEGRADED 返回 true；
- 不在 Core 中加入 Spring Lifecycle 或 Listener 操作。

**Step 4: 运行 GREEN**

Run:

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl :egon-cola-component-gateway-core -am test
```

Expected: BUILD SUCCESS。

**Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-core
git commit -m "feat(gateway): add engine lifecycle state machine"
```

## Task 6: 完成 GWS-01 Reactor 验收

**Files:**

- Modify if required: `egon-cola-components/egon-cola-component-gateway/README.md`
- Modify if required: `egon-cola-components/egon-cola-component-gateway/README.zh-CN.md`
- Modify if required: GWS-01 范围内的 POM 或测试

**Step 1: 聚焦验证**

Run:

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl :egon-cola-component-gateway-test-suite -am clean test
```

Expected: BUILD SUCCESS。

**Step 2: 公共 BOM 验证**

Run:

```bash
./mvnw -B -ntp -f egon-cola-components/egon-cola-components-bom/pom.xml clean verify
```

Expected: BUILD SUCCESS。

**Step 3: 检查 diff 与工作树**

Run:

```bash
git diff --check
git status --short
git log --oneline --decorate -6
```

Expected: 无 whitespace error；只包含 GWS-01 和本实施计划范围文件。

**Step 4: 更新 README 验收状态（如前面未完整）并 Commit**

仅在 README 或构建修复存在未提交变化时提交：

```bash
git add egon-cola-components/egon-cola-component-gateway
git commit -m "docs(gateway): document foundation module contract"
```

若无变化，不创建空提交。

## 完成定义

- GWS-01 的模块、BOM、身份、版本、错误、Core 与生命周期验收项有代码和测试证据；
- Starter 与 Provider Runtime 是独立 Artifact；
- Admin Web 不进入 Maven reactor；
- Contract/Core 均无运行时框架依赖；
- 未实现任何 GWS-02～GWS-13 行为；
- 工作树保留在 `codex/gateway-wave-0-foundation`，等待继续 GWS-02 或用户审核，不自动合并、不推送。
