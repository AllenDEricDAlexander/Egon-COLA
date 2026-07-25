# Gateway GWS-02C RPC Contract Extension Implementation Plan

> **For Codex:** REQUIRED SUB-SKILL: Use superpowers:executing-plans and
> superpowers:test-driven-development to implement this plan task-by-task.

**Goal:** 让 Gateway Starter 能只读获取 RPC Provider 已校验 Contract 及标准
FileDescriptorSet Snapshot，并允许 Provider 安全贡献 `gateway.*` Metadata。

**Architecture:** `RpcProviderBeanScanner` 只扫描一次并生成不可变
`RpcProviderMethodRegistry`；Server Definition、Provider Lifecycle 与
`DefaultRpcContractCatalog` 共用该结果。Snapshot Builder 从已校验 Proto Method
Descriptor 收集传递文件依赖，按文件名排序并生成标准 `FileDescriptorSet` 和 SHA-256。
Metadata Contributor 通过 Spring ordered collection 合并，在建立任何租约前完成冲突和
保留键校验。

**Tech Stack:** Java 21、Spring Boot 3.5.x、gRPC、Protobuf DescriptorProtos、
JUnit 5、AssertJ、Mockito。

---

## 全局约束

- 工作目录：
  `/Users/mario/SelfProject/Egon-COLA/.worktrees/gateway-wave-0-foundation`。
- 不提供动态 Handler、Channel Cache、Provider Directory 或生产 Gateway Forwarder。
- 保持 Unary-only、单 `INTERNAL_GATEWAY` 和现有 RPC Wire Protocol。
- Catalog 不暴露可变集合；Snapshot 不包含 Java 反射对象或 Provider 地址。
- Contributor 失败必须发生在 Server 注册租约之前。
- 每个任务独立 RED → GREEN → REFACTOR 并提交。

### 设计模式判断

- 使用 **Catalog** 提供已验证 Contract 的只读索引。
- 使用 **Builder** 从 Proto Descriptor Graph 构造确定性 Snapshot。
- 使用有序 **Contributor/Strategy** 扩展 Provider Metadata；合并器负责冲突规则。
- 不重新扫描 Provider，不建立第二份 Method Registry。

## Task 1: 建立单次扫描的 Provider Definition 边界

**Files:**

- Modify: `RpcProviderBeanScanner`
- Modify: `RpcProviderMethodRegistry`
- Modify: `RpcProviderLifecycle`
- Modify: `EgonRpcAutoConfig`
- Modify: lifecycle/scanner/auto-config tests

**Behavior:**

1. Provider-enabled Context 创建唯一不可变 Method Registry Bean。
2. 重复 Service Identity 或同一 Wire Service 不同 group/version 启动失败。
3. Lifecycle 接收该 Registry，不再在 `start()` 重扫。
4. 无 Provider 时仍由 Lifecycle 按现有错误语义失败。

**Commit:** `refactor(rpc): share validated provider registry`

## Task 2: 实现只读 Contract Catalog

**Files:**

- Create: `RpcContractCatalog`
- Create: `DefaultRpcContractCatalog`
- Modify: `EgonRpcAutoConfig`
- Test: `DefaultRpcContractCatalogTest`
- Modify: `RpcProviderBeanScannerTest`

**Behavior:**

1. `contracts()`/`find()` 只包含实际 Provider。
2. 列表按 serviceName/group/version 稳定排序。
3. 返回不可变 Descriptor 列表，Catalog 无 register/remove API。
4. Catalog 与 Lifecycle 引用同一 Registry 实例。

**Commit:** `feat(rpc): expose immutable contract catalog`

## Task 3: 构造确定性 Protobuf Contract Snapshot

**Files:**

- Create: `RpcContractSnapshot`
- Create: `RpcMethodSnapshot`
- Create: `RpcType`
- Create: `RpcContractSnapshotBuilder`
- Modify: `RpcContractCatalog`
- Test: `RpcContractSnapshotBuilderTest`
- Test: `RpcContractSnapshotReconstructionTest`

**Behavior:**

1. Snapshot 包含 service identity、proto package/service、methods。
2. FileDescriptorSet 包含重建方法所需全部传递依赖。
3. 文件按 name 排序后序列化，SHA-256 针对最终 bytes。
4. Method 按 fullMethodName 排序且全部为 UNARY。
5. Snapshot record 对 `byte[]` defensive copy。
6. 测试从 bytes 重建 FileDescriptor 和所有 Method Descriptor。

**Commit:** `feat(rpc): export deterministic contract snapshots`

## Task 4: 实现有序 Provider Metadata Contributor

**Files:**

- Create: `RpcProviderMetadataContributor`
- Create: `RpcProviderMetadataMerger`
- Modify: `RpcProviderLeaseManager`
- Modify: `EgonRpcAutoConfig`
- Test: `RpcProviderMetadataMergerTest`
- Modify: `RpcProviderLifecycleTest`

**Behavior:**

1. Contributor 使用 Spring Order，空 Map 允许。
2. 相同 Key/相同 Value 幂等；相同 Key/不同 Value 启动失败。
3. 禁止覆盖 `egon.rpc.transport`、`serialization`、`runtime-version`。
4. 用户 properties Metadata 与 Contributor 也执行冲突校验。
5. `gateway.weight` 为 1～10000；tags/zone/region 等执行长度与格式校验。
6. 最终结果继续经过 `DdcServiceRegistration` 的 32/64/512/Secret Guard。
7. Contributor 异常时不建立任何 Provider 租约。

**Commit:** `feat(rpc): compose provider metadata contributors`

## Task 5: GWS-02C 回归验收

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl :egon-cola-component-rpc-test-suite -am clean test
./mvnw -B -ntp \
  -f egon-cola-components/egon-cola-component-dynamic-config-center/pom.xml \
  test
git diff --check
git status --short
```

检查：

1. Snapshot 源码/序列化结果不含 `Class`、`Method`、Bean、Host/Port。
2. RPC Test Mock Gateway 仍只存在测试模块。
3. DDC/RPC Production 包不出现 Gateway 路由、负载均衡或治理实现。
4. 不创建空验收提交。
